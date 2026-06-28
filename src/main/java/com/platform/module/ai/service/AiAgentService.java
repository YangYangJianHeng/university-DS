package com.platform.module.ai.service;

import com.platform.module.ai.config.ChatConfig;
import com.platform.module.ai.dto.ChatContext;
import com.platform.module.ai.entity.AIChatMessage;
import com.platform.module.ai.entity.AIChatSession;
import com.platform.module.ai.mapper.AIChatMessageMapper;
import com.platform.module.ai.mapper.AIChatSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.module.competition.entity.Competition;
import com.platform.module.competition.mapper.CompetitionMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.mapper.UserMapper;
import com.platform.module.ai.config.OllamaConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Agent 智能体
 * 实现简单的意图识别 + 数据查询 + RAG 检索 + 回复生成流程
 * 模拟 LangGraph 的节点式工作流
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentService {

    private final ChatLanguageModel chatLanguageModel;
    private final OpenAIClient openAIClient;
    private final OllamaClient ollamaClient;
    private final OllamaConfig ollamaConfig;
    private final ChatConfig chatConfig;
    private final RagKnowledgeService ragService;
    private final ChatCompressionService compressionService;
    private final AIChatSessionMapper sessionMapper;
    private final AIChatMessageMapper messageMapper;
    private final ProjectMapper projectMapper;
    private final CompetitionMapper competitionMapper;
    private final TaskMapper taskMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserMapper userMapper;

    // ==================== Agent 主流程 ====================

    /**
     * 处理用户消息的完整 Agent 流程
     * 模拟 LangGraph: classify → collectContext → buildPrompt → generateResponse
     */
    public String processMessage(Long userId, Long sessionId, String userMessage) {
        // Step 1: 意图分类
        String intent = classifyIntent(userMessage);

        // Step 2: 收集用户上下文数据
        ChatContext ctx = collectUserContext(userId, intent, userMessage);

        // Step 3: RAG 检索相关知识
        String ragContext = ragService.searchContext(userMessage, 3);

        // Step 4: 检查并压缩历史消息
        List<Map<String, String>> history = compressionService.checkAndCompress(sessionId, userId);

        // Step 5: 构建完整 prompt
        List<Map<String, String>> messages = buildPrompt(history, userMessage, ctx, ragContext, intent);

        // Step 6: 调用 LLM 生成回复
        String response = doChat(messages);

        // Step 7: 保存消息到数据库
        saveMessage(sessionId, userId, "user", userMessage);
        saveMessage(sessionId, userId, "assistant", response != null ? response : "抱歉，暂时无法回复。");

        // Step 8: 自动生成会话标题（首次对话时）
        autoTitleSession(sessionId, userMessage);

        return response;
    }

    /**
     * 流式处理（SSE）
     */
    public void processMessageStream(Long userId, Long sessionId, String userMessage,
                                      java.util.function.Consumer<String> onChunk,
                                      Runnable onDone) {
        String intent = classifyIntent(userMessage);
        ChatContext ctx = collectUserContext(userId, intent, userMessage);
        String ragContext = ragService.searchContext(userMessage, 3);
        List<Map<String, String>> history = compressionService.checkAndCompress(sessionId, userId);
        List<Map<String, String>> messages = buildPrompt(history, userMessage, ctx, ragContext, intent);

        // 保存用户消息
        saveMessage(sessionId, userId, "user", userMessage);

        // 流式输出
        StringBuilder fullResponse = new StringBuilder();
        doChatStream(messages,
                chunk -> {
                    fullResponse.append(chunk);
                    onChunk.accept(chunk);
                },
                () -> {
                    // 保存完整回复
                    saveMessage(sessionId, userId, "assistant", fullResponse.toString());
                    autoTitleSession(sessionId, userMessage);
                    onDone.run();
                });
    }

    // ==================== 意图分类 ====================

    /**
     * 基于关键词 + 规则的意图分类
     */
    private String classifyIntent(String message) {
        String lower = message.toLowerCase();

        // 项目相关关键词
        String[] projectKw = {"项目", "任务", "进度", "里程碑", "逾期", "deadline",
                "project", "task", "我的项目", "项目管理", "分工", "甘特图", "团队协作"};
        for (String kw : projectKw) {
            if (lower.contains(kw)) return "PROJECT";
        }

        // 竞赛相关关键词
        String[] competitionKw = {"比赛", "竞赛", "大赛", "参赛", "报名", "备赛",
                "competition", "赛事", "获奖", "评委", "答辩"};
        for (String kw : competitionKw) {
            if (lower.contains(kw)) return "COMPETITION";
        }

        // 团队相关
        String[] teamKw = {"团队", "队员", "成员", "导师", "招人", "招募",
                "组队", "team", "纳新"};
        for (String kw : teamKw) {
            if (lower.contains(kw)) return "TEAM";
        }

        return "GENERAL";
    }

    // ==================== 用户上下文收集 ====================

    /**
     * 收集与当前用户和意图相关的数据
     */
    private ChatContext collectUserContext(Long userId, String intent, String message) {
        ChatContext ctx = new ChatContext();

        // 获取用户基本信息
        User user = userMapper.selectById(userId);
        if (user != null) {
            ctx.setUserName(user.getUsername());
            ctx.setUserMajor(user.getMajor());
            ctx.setUserGrade(user.getGrade());
        }

        // 获取用户参与的项目
        List<TeamMember> memberships = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        List<Long> teamIds = memberships.stream().map(TeamMember::getTeamId).collect(Collectors.toList());

        if (!teamIds.isEmpty()) {
            List<Project> projects = projectMapper.selectList(
                    new LambdaQueryWrapper<Project>()
                            .in(Project::getTeamId, teamIds)
                            .ne(Project::getStatus, "CLOSED"));
            ctx.setUserProjects(projects);

            // 获取这些项目的任务
            if (!projects.isEmpty()) {
                List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
                List<Task> tasks = taskMapper.selectList(
                        new LambdaQueryWrapper<Task>().in(Task::getProjectId, projectIds));
                ctx.setUserTasks(tasks);
            }
        }

        // 根据意图补充数据
        if ("COMPETITION".equals(intent)) {
            List<Competition> competitions = competitionMapper.selectList(
                    new LambdaQueryWrapper<Competition>()
                            .orderByDesc(Competition::getRegisterEnd)
                            .last("LIMIT 20"));
            ctx.setRecentCompetitions(competitions);
        }

        return ctx;
    }

    // ==================== Prompt 构建 ====================

    /**
     * 构建完整的对话 prompt
     * 结构：System Prompt (Skill Doc) + RAG Context + User Context + History + Current Message
     */
    private List<Map<String, String>> buildPrompt(
            List<Map<String, String>> history,
            String userMessage,
            ChatContext ctx,
            String ragContext,
            String intent) {

        List<Map<String, String>> messages = new ArrayList<>();

        // System Prompt: 角色和语气规范
        messages.add(Map.of("role", "system", "content", buildSystemPrompt()));

        // 用户数据上下文
        String dataContext = buildDataContext(ctx);
        if (!dataContext.isEmpty()) {
            messages.add(Map.of("role", "system", "content",
                    "以下是用户的平台数据，请基于实际数据回答问题：\n" + dataContext));
        }

        // RAG 知识上下文
        if (ragContext != null && !ragContext.isEmpty()) {
            messages.add(Map.of("role", "system", "content",
                    "以下是相关知识库内容，可作为参考：\n" + ragContext));
        }

        // 历史消息
        messages.addAll(history);

        // 当前用户消息
        messages.add(Map.of("role", "user", "content", userMessage));

        return messages;
    }

    // ==================== System Prompt ====================

    private String buildSystemPrompt() {
        return """
        你是「组队星」平台的 AI 智能助手（小星），为大学生提供项目管理和竞赛比赛咨询服务。
        
        ## 身份和行为规范
        - 你的名字是"小星"，像一个经验丰富、热心亲切的学长/学姐
        - 始终使用中文回复，语气亲切温暖但不油腻
        - 回复控制在200-500字，适当使用emoji点缀（每段1-2个）
        - 优先使用用户的实际平台数据来回答，而不是泛泛而谈
        - 绝不编造不存在的数据或功能
        - 涉及密码、隐私等信息时友好提醒用户保护安全
        
        ## 平台功能引导
        提及平台功能时请使用准确的功能名称：
        - 任务中心：查看和管理所有任务
        - 项目管理：查看项目详情和进度
        - 竞赛中心：浏览和搜索竞赛信息
        - 招募广场：发布和查看招募信息
        - 我的团队：管理团队成员
        - 成果展厅：查看和提交成果
        
        ## 数据分析原则
        - 看到用户数据中的逾期任务时，特别提醒并给出处理建议
        - 根据任务完成率判断项目健康度
        - 根据用户技能推荐适合的竞赛
        - 当用户无数据时，友好告知并引导前往对应功能
        
        ## 说话语气示例
        时间充裕："距截止还有{p0}天，好好打磨作品~ ✨"
        时间紧迫："只剩{p0}天啦！抓紧行动起来 💪"
        任务完成："项目进展不错！继续保持 🎉"
        有逾期："看到有{p0}个任务逾期了，建议尽快处理 ⚠️"
        
        请严格遵守以上规范，为用户提供温暖专业的服务体验。
        """.replace("{p0}", "%s");
    }

    // ==================== 数据上下文构建 ====================

    private String buildDataContext(ChatContext ctx) {
        StringBuilder sb = new StringBuilder();

        // 用户信息
        sb.append("当前用户：").append(ctx.getUserName() != null ? ctx.getUserName() : "未知");
        if (ctx.getUserMajor() != null) sb.append("，专业：").append(ctx.getUserMajor());
        if (ctx.getUserGrade() != null) sb.append("，年级：").append(ctx.getUserGrade());
        sb.append("\n\n");

        // 项目列表
        List<Project> projects = ctx.getUserProjects();
        if (projects != null && !projects.isEmpty()) {
            sb.append("用户的项目列表：\n");
            for (Project p : projects) {
                sb.append(String.format("- [%s] %s（状态：%s，描述：%s）\n",
                        p.getId(), p.getName(), p.getStatus(),
                        p.getDescription() != null ? p.getDescription() : "无"));
            }
            sb.append("\n");
        }

        // 任务统计
        List<Task> tasks = ctx.getUserTasks();
        if (tasks != null && !tasks.isEmpty()) {
            long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
            long inProgress = tasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count();
            long pending = tasks.stream().filter(t -> "PENDING".equals(t.getStatus())).count();
            long overdue = tasks.stream().filter(t -> "OVERDUE".equals(t.getStatus())).count();

            sb.append("任务统计：\n");
            sb.append(String.format("  已完成：%d，进行中：%d，待处理：%d，已逾期：%d\n",
                    completed, inProgress, pending, overdue));

            List<Task> overdueTasks = tasks.stream()
                    .filter(t -> "OVERDUE".equals(t.getStatus()))
                    .collect(Collectors.toList());
            if (!overdueTasks.isEmpty()) {
                sb.append("逾期任务列表：\n");
                for (Task t : overdueTasks) {
                    sb.append(String.format("  - %s（优先级：%s，截止：%s）\n",
                            t.getTitle(), t.getPriority(), t.getDeadline()));
                }
            }
            sb.append(String.format("总完成率：%.0f%%\n\n",
                    tasks.isEmpty() ? 0 : (double) completed / tasks.size() * 100));
        }

        // 竞赛推荐
        List<Competition> competitions = ctx.getRecentCompetitions();
        if (competitions != null && !competitions.isEmpty()) {
            sb.append("近期可报名的竞赛：\n");
            for (Competition c : competitions) {
                sb.append(String.format("- [%s] %s（级别：%s，类别：%s，报名截止：%s）\n",
                        c.getId(), c.getName(), c.getLevel() != null ? c.getLevel() : "未知",
                        c.getCategory() != null ? c.getCategory() : "未知",
                        c.getRegisterEnd() != null ? c.getRegisterEnd().toString() : "未知"));
            }
        }

        return sb.toString();
    }

    // ==================== 消息持久化 ====================

    private void saveMessage(Long sessionId, Long userId, String role, String content) {
        AIChatMessage msg = new AIChatMessage();
        msg.setSessionId(sessionId);
        msg.setUserId(userId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCompressed(0);
        msg.setTokenCount(compressionService.estimateTokens(content));
        messageMapper.insert(msg);

        // 更新会话消息计数
        AIChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setMessageCount(
                    Math.toIntExact(messageMapper.selectCount(
                            new LambdaQueryWrapper<AIChatMessage>()
                                    .eq(AIChatMessage::getSessionId, sessionId))));
            sessionMapper.updateById(session);
        }
    }

    /**
     * 自动为会话生成标题（首次回复后）
     */
    private void autoTitleSession(Long sessionId, String firstMessage) {
        AIChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getMessageCount() != null && session.getMessageCount() > 2) return;

        // 使用首条消息前20字作为标题
        String title = firstMessage.length() > 20
                ? firstMessage.substring(0, 20) + "..."
                : firstMessage;
        session.setTitle(title);
        sessionMapper.updateById(session);
    }

    // ==================== Chat (LangChain4j) ====================

    /**
     * 使用 LangChain4j ChatLanguageModel 调用 LLM
     */
    private String doChat(List<Map<String, String>> messages) {
        try {
            List<ChatMessage> lcMessages = toLangChainMessages(messages);
            ChatResponse response = chatLanguageModel.chat(ChatRequest.builder().messages(lcMessages).build());
            return response.aiMessage().text();
        } catch (Exception e) {
            log.error("LLM chat error: {}", e.getMessage());
            return "抱歉，AI 服务暂时不可用：" + e.getMessage();
        }
    }

    /**
     * 流式调用 - 保持原有方式（OpenAIClient/OllamaClient）
     * 同步已切换到 LangChain4j
     */
    private void doChatStream(List<Map<String, String>> messages,
                               java.util.function.Consumer<String> onChunk,
                               Runnable onDone) {
        // 使用原有 OpenAIClient 进行流式调用（它直接从 .env 读取 Key）
        openAIClient.chatStream(messages, onChunk, onDone);
    }

    /**
     * 将 Map 格式消息转换为 LangChain4j ChatMessage
     */
    private List<ChatMessage> toLangChainMessages(List<Map<String, String>> messages) {
        List<ChatMessage> result = new ArrayList<>();
        if (messages == null) return result;

        for (Map<String, String> msg : messages) {
            String role = msg.get("role");
            String content = msg.get("content");
            if (content == null || content.isEmpty()) continue;

            switch (role != null ? role : "") {
                case "system" -> result.add(SystemMessage.from(content));
                case "user" -> result.add(UserMessage.from(content));
                default -> result.add(AiMessage.from(content)); // assistant / other
            }
        }
        return result;
    }
}
