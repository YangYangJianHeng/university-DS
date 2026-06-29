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

        // Step 3: RAG 检索相关知识（通用对话跳过，避免不相关数据污染）
        String ragContext = "GENERAL".equals(intent) ? "" : ragService.searchContext(userMessage, 3);

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
        // 通用对话跳过 RAG，避免不相关数据污染
        String ragContext = "GENERAL".equals(intent) ? "" : ragService.searchContext(userMessage, 3);
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

        // 竞赛相关关键词（增加"题目""选题"等推荐类词）
        String[] competitionKw = {"比赛", "竞赛", "大赛", "参赛", "报名", "备赛",
                "competition", "赛事", "获奖", "评委", "答辩", "赛题", "选题"};
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
     * GENERAL意图只获取用户基本信息，不查询项目/任务，避免不必要的数据污染
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

        // 通用对话：只收集用户基本信息，不查询项目/任务/竞赛数据
        if ("GENERAL".equals(intent)) {
            return ctx;
        }

        // 平台专业场景：收集项目数据
        List<TeamMember> memberships = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        List<Long> teamIds = memberships.stream().map(TeamMember::getTeamId).collect(Collectors.toList());

        if (!teamIds.isEmpty()) {
            List<Project> projects = projectMapper.selectList(
                    new LambdaQueryWrapper<Project>()
                            .in(Project::getTeamId, teamIds)
                            .ne(Project::getStatus, "CLOSED")
                            .orderByDesc(Project::getCreatedAt)
                            .last("LIMIT 5"));
            ctx.setUserProjects(projects);

            // 获取这些项目的任务（按优先级，限制最多20个）
            if (!projects.isEmpty()) {
                List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
                List<Task> tasks = taskMapper.selectList(
                        new LambdaQueryWrapper<Task>()
                                .in(Task::getProjectId, projectIds)
                                .orderByAsc(Task::getDeadline)
                                .last("LIMIT 20"));
                ctx.setUserTasks(tasks);
            }
        }

        // COMPETITION意图：额外补充竞赛数据
        if ("COMPETITION".equals(intent)) {
            // 优先筛选：报名中 + 按截止日期升序排列（即将截止的排前面）
            LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<Competition>()
                    .orderByAsc(Competition::getRegisterEnd)
                    .last("LIMIT 15");

            // 如果有用户专业信息，尝试筛选相关类别的竞赛
            if (user != null && user.getMajor() != null && !user.getMajor().isEmpty()) {
                wrapper.and(w -> w
                        .like(Competition::getDescription, user.getMajor())
                        .or()
                        .like(Competition::getCategory, getShortMajor(user.getMajor())));
            }

            List<Competition> competitions = competitionMapper.selectList(wrapper);

            // 如果专业筛选结果太少，降级为全量查询
            if (competitions.size() < 3 && user != null && user.getMajor() != null && !user.getMajor().isEmpty()) {
                competitions = competitionMapper.selectList(
                        new LambdaQueryWrapper<Competition>()
                                .orderByAsc(Competition::getRegisterEnd)
                                .last("LIMIT 15"));
            }

            ctx.setRecentCompetitions(competitions);
        }

        return ctx;
    }

    /**
     * 提取专业简称（如"计算机科学与技术" → "计算机"）
     */
    private String getShortMajor(String major) {
        if (major == null || major.isEmpty()) return "";
        // 取前2-4个中文字作为短专业名
        String shortName = major.replaceAll("[^\\u4e00-\\u9fa5]", "");
        return shortName.length() > 3 ? shortName.substring(0, 3) : shortName;
    }

    // ==================== Prompt 构建 ====================

    /**
     * 构建完整的对话 prompt
     * 结构：System Prompt + 意图引导 + RAG Context + User Context + History + Current Message
     * GENERAL意图不注入数据上下文，保持对话自由
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

        // 意图引导：告诉LLM当前用户意图是什么，应该如何应答
        String intentGuidance = buildIntentGuidance(intent, ctx);
        if (!intentGuidance.isEmpty()) {
            messages.add(Map.of("role", "system", "content", intentGuidance));
        }

        // RAG 知识上下文（通用对话跳过，避免不相关数据污染LLM回答）
        if (!"GENERAL".equals(intent) && ragContext != null && !ragContext.isEmpty()) {
            messages.add(Map.of("role", "system", "content",
                    "以下是相关知识库内容，可作为参考：\n" + ragContext));
        }

        // 用户数据上下文：仅平台专业场景注入，通用对话跳过
        if (!"GENERAL".equals(intent)) {
            String dataContext = buildDataContext(ctx);
            if (!dataContext.isEmpty() && hasMeaningfulData(ctx)) {
                messages.add(Map.of("role", "system", "content",
                        "以下是用户的平台数据，请基于实际数据回答问题：\n" + dataContext));
            }
        }

        // 历史消息
        messages.addAll(history);

        // 当前用户消息
        messages.add(Map.of("role", "user", "content", userMessage));

        return messages;
    }

    /**
     * 判断上下文是否包含有意义的数据（除了用户基本信息之外）
     */
    private boolean hasMeaningfulData(ChatContext ctx) {
        return (ctx.getUserProjects() != null && !ctx.getUserProjects().isEmpty())
                || (ctx.getUserTasks() != null && !ctx.getUserTasks().isEmpty())
                || (ctx.getRecentCompetitions() != null && !ctx.getRecentCompetitions().isEmpty());
    }

    /**
     * 根据用户意图构建引导语，帮助LLM更精准地回答
     */
    private String buildIntentGuidance(String intent, ChatContext ctx) {
        return switch (intent) {
            case "COMPETITION" -> {
                StringBuilder guidance = new StringBuilder();
                guidance.append("当前用户意图：竞赛推荐与咨询\n");
                guidance.append("请根据用户的专业、年级和项目经历，从提供的竞赛列表中筛选最匹配的3-5个竞赛进行推荐。\n");
                guidance.append("推荐时需：1)说明推荐理由（结合用户背景）；2)提醒报名截止时间；3)给出参赛建议。\n");
                if (ctx.getUserName() != null) {
                    guidance.append("用户叫").append(ctx.getUserName()).append("，");
                }
                if (ctx.getUserMajor() != null) {
                    guidance.append("专业是").append(ctx.getUserMajor()).append("，");
                }
                if (ctx.getUserGrade() != null) {
                    guidance.append(ctx.getUserGrade()).append("。");
                }
                guidance.append("请称呼用户名字，温暖地给出推荐。\n");
                yield guidance.toString();
            }
            case "PROJECT" -> """
                    当前用户意图：项目管理与进度查询
                    请基于用户的项目数据和任务统计，分析项目健康度，对逾期任务重点提醒并给出可操作的建议。
                    """;
            case "TEAM" -> """
                    当前用户意图：团队管理与协作
                    请基于用户的团队数据，给出团队管理和协作优化建议。
                    """;
            default -> """
                    当前用户意图：通用对话
                    这是一次自由对话，用户可能在闲聊、问知识或寻求建议，无需关联平台数据。
                    请自然友好地回答用户问题，像一个真正的朋友一样聊天。
                    """;
        };
    }

    // ==================== System Prompt ====================

    private String buildSystemPrompt() {
        return """
        你是「组队星」平台的 AI 智能助手，名字叫"小星"，像一个热心亲切的学长/学姐。
        - 始终保持中文回复，语气亲切温暖
        
        ## 通用对话场景（用户闲聊、问知识、写代码等非平台问题）
        - 作为普通AI助手自由回答即可，无需关联平台数据
        - 像朋友一样自然对话，可以幽默风趣
        
        ## 平台专业场景（仅当用户明确询问项目/竞赛/团队等平台功能时）
        1. 结合用户专业、年级和项目经历推荐竞赛，说明推荐理由
        2. 分析项目任务完成率和进度，逾期任务重点提醒
        3. 绝不编造不存在的数据或功能
        4. 用户数据为空时：友好引导前往平台对应功能模块
        
        ## 平台功能名称
        竞赛中心、项目管理、任务中心、招募广场、我的团队、成果展厅
        """;
    }

    // ==================== 数据上下文构建 ====================

    private String buildDataContext(ChatContext ctx) {
        StringBuilder sb = new StringBuilder();

        // 用户信息
        sb.append("当前用户：").append(ctx.getUserName() != null ? ctx.getUserName() : "未知");
        if (ctx.getUserMajor() != null) sb.append("，专业：").append(ctx.getUserMajor());
        if (ctx.getUserGrade() != null) sb.append("，年级：").append(ctx.getUserGrade());
        sb.append("\n\n");

        // 项目列表（限制最多5个）
        List<Project> projects = ctx.getUserProjects();
        if (projects != null && !projects.isEmpty()) {
            sb.append("用户参与的项目（最多展示5个）：\n");
            int count = 0;
            for (Project p : projects) {
                if (count >= 5) break;
                sb.append(String.format("- [%s] %s（状态：%s）\n",
                        p.getId(), p.getName(), p.getStatus()));
                count++;
            }
            sb.append("\n");
        }

        // 任务统计（聚合统计 + 只显示逾期任务）
        List<Task> tasks = ctx.getUserTasks();
        if (tasks != null && !tasks.isEmpty()) {
            long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
            long inProgress = tasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count();
            long pending = tasks.stream().filter(t -> "PENDING".equals(t.getStatus())).count();
            long overdue = tasks.stream().filter(t -> "OVERDUE".equals(t.getStatus())).count();

            sb.append("任务统计：已完成").append(completed).append("，进行中").append(inProgress)
                    .append("，待处理").append(pending).append("，已逾期").append(overdue);
            sb.append(String.format("（总完成率%.0f%%）\n", 
                    (double) completed / tasks.size() * 100));

            // 只列出逾期任务（最多5个）
            List<Task> overdueTasks = tasks.stream()
                    .filter(t -> "OVERDUE".equals(t.getStatus()))
                    .limit(5)
                    .collect(Collectors.toList());
            if (!overdueTasks.isEmpty()) {
                sb.append("逾期任务（需关注）：\n");
                for (Task t : overdueTasks) {
                    sb.append(String.format("  - %s（优先级：%s，截止：%s）\n",
                            t.getTitle(), t.getPriority(), t.getDeadline()));
                }
            }
            sb.append("\n");
        }

        // 竞赛推荐（限制最多10个）
        List<Competition> competitions = ctx.getRecentCompetitions();
        if (competitions != null && !competitions.isEmpty()) {
            sb.append("可参考的竞赛列表（最多展示10个）：\n");
            int count = 0;
            for (Competition c : competitions) {
                if (count >= 10) break;
                sb.append(String.format("- [%s] %s（级别：%s，类别：%s，报名截止：%s）\n",
                        c.getId(), c.getName(),
                        c.getLevel() != null ? c.getLevel() : "未知",
                        c.getCategory() != null ? c.getCategory() : "未知",
                        c.getRegisterEnd() != null ? c.getRegisterEnd().toString() : "未知"));
                count++;
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
