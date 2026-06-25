package com.platform.module.collaboration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.common.util.UserContext;
import com.platform.module.collaboration.dto.*;
import com.platform.module.collaboration.entity.*;
import com.platform.module.collaboration.mapper.*;
import com.platform.module.collaboration.service.TaskService;
import com.platform.module.notification.entity.Notification;
import com.platform.module.notification.mapper.NotificationMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.entity.UserSkill;
import com.platform.module.user.mapper.UserMapper;
import com.platform.module.user.mapper.UserSkillMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private final ProjectMapper projectMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserMapper userMapper;
    private final UserSkillMapper userSkillMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final TaskCommentMapper taskCommentMapper;
    private final NotificationMapper notificationMapper;

    private static final Set<String> VALID_STATUSES = Set.of("TODO", "IN_PROGRESS", "SUBMITTED", "REVIEWING", "DONE", "OVERDUE");
    private static final Set<String> VALID_PRIORITIES = Set.of("HIGH", "MEDIUM", "LOW");

    public TaskServiceImpl(ProjectMapper projectMapper,
                           TeamMemberMapper teamMemberMapper,
                           UserMapper userMapper,
                           UserSkillMapper userSkillMapper,
                           TaskAssigneeMapper taskAssigneeMapper,
                           TaskSubmissionMapper taskSubmissionMapper,
                           TaskCommentMapper taskCommentMapper,
                           NotificationMapper notificationMapper) {
        this.projectMapper = projectMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.userMapper = userMapper;
        this.userSkillMapper = userSkillMapper;
        this.taskAssigneeMapper = taskAssigneeMapper;
        this.taskSubmissionMapper = taskSubmissionMapper;
        this.taskCommentMapper = taskCommentMapper;
        this.notificationMapper = notificationMapper;
    }

    // ==================== 任务管理 ====================

    @Override
    @Transactional
    public Task createTask(TaskCreateRequest request) {
        // 输入校验
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw BusinessException.badRequest("任务标题不能为空");
        }
        if (request.getProjectId() == null) {
            throw BusinessException.badRequest("请选择所属项目");
        }

        // 验证项目存在
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            throw BusinessException.notFound("所属项目不存在");
        }

        // 权限校验：只有项目负责人可以创建任务
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId != null && !currentUserId.equals(project.getLeaderId())) {
            throw BusinessException.forbidden("只有项目负责人可以创建任务");
        }

        // 优先级校验
        if (request.getPriority() != null && !VALID_PRIORITIES.contains(request.getPriority())) {
            throw BusinessException.badRequest("无效的优先级: " + request.getPriority());
        }

        // 创建任务
        Task task = new Task();
        task.setProjectId(request.getProjectId());
        task.setCreatorId(currentUserId);
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setRequiredSkills(request.getRequiredSkills());
        task.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        task.setDeadline(request.getDeadline());
        task.setStatus("TODO");
        save(task);

        // 指派执行人
        if (request.getAssignees() != null && !request.getAssignees().isEmpty()) {
            for (TaskCreateRequest.AssigneeInfo ai : request.getAssignees()) {
                addAssignee(task.getId(), ai.getUserId(), ai.getAssignmentNote());
            }
        }

        return task;
    }

    @Override
    public TaskDetailVO getTaskDetail(Long taskId) {
        Task task = getById(taskId);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }

        TaskDetailVO vo = new TaskDetailVO();
        vo.setId(task.getId());
        vo.setProjectId(task.getProjectId());
        vo.setCreatorId(task.getCreatorId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setRequiredSkills(task.getRequiredSkills());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setDeadline(task.getDeadline());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());

        // 创建者信息
        User creator = userMapper.selectById(task.getCreatorId());
        vo.setCreatorName(creator != null ? creator.getUsername() : null);

        // 执行人列表
        List<TaskAssignee> assignees = getAssignees(taskId);
        List<TaskDetailVO.AssigneeVO> assigneeVOs = new ArrayList<>();
        for (TaskAssignee a : assignees) {
            TaskDetailVO.AssigneeVO avo = new TaskDetailVO.AssigneeVO();
            avo.setId(a.getId());
            avo.setUserId(a.getUserId());
            avo.setAssignmentNote(a.getAssignmentNote());
            avo.setSkillMatchScore(a.getSkillMatchScore() != null ? a.getSkillMatchScore().toString() : "0");
            avo.setStatus(a.getStatus());
            avo.setAssignedAt(a.getAssignedAt());
            User u = userMapper.selectById(a.getUserId());
            if (u != null) {
                avo.setUserName(u.getUsername());
                avo.setUserAvatar(u.getAvatar());
            }
            assigneeVOs.add(avo);
        }
        vo.setAssignees(assigneeVOs);

        // 成果提交列表
        List<TaskSubmission> submissions = getSubmissions(taskId);
        List<TaskDetailVO.SubmissionVO> submissionVOs = new ArrayList<>();
        for (TaskSubmission s : submissions) {
            TaskDetailVO.SubmissionVO svo = new TaskDetailVO.SubmissionVO();
            svo.setId(s.getId());
            svo.setSubmitterId(s.getSubmitterId());
            svo.setTitle(s.getTitle());
            svo.setDescription(s.getDescription());
            svo.setStatus(s.getStatus());
            svo.setReviewComment(s.getReviewComment());
            svo.setReviewerId(s.getReviewerId());
            svo.setReviewedAt(s.getReviewedAt());
            svo.setCreatedAt(s.getCreatedAt());

            // 解析文件URL列表
            if (s.getFileUrls() != null) {
                try {
                    svo.setFileUrls(parseJsonArray(s.getFileUrls()));
                } catch (Exception ignored) {}
            }

            User su = userMapper.selectById(s.getSubmitterId());
            if (su != null) svo.setSubmitterName(su.getUsername());

            if (s.getReviewerId() != null) {
                User rv = userMapper.selectById(s.getReviewerId());
                if (rv != null) svo.setReviewerName(rv.getUsername());
            }
            submissionVOs.add(svo);
        }
        vo.setSubmissions(submissionVOs);

        // 评论列表
        List<TaskComment> comments = getComments(taskId);
        List<TaskDetailVO.CommentVO> commentVOs = new ArrayList<>();
        for (TaskComment c : comments) {
            TaskDetailVO.CommentVO cvo = new TaskDetailVO.CommentVO();
            cvo.setId(c.getId());
            cvo.setUserId(c.getUserId());
            cvo.setContent(c.getContent());
            cvo.setCreatedAt(c.getCreatedAt());

            // 解析@提及
            if (c.getMentionedUserIds() != null) {
                try {
                    List<Long> ids = parseJsonLongArray(c.getMentionedUserIds());
                    cvo.setMentionedUserIds(ids);
                } catch (Exception ignored) {}
            }

            User cu = userMapper.selectById(c.getUserId());
            if (cu != null) {
                cvo.setUserName(cu.getUsername());
                cvo.setUserAvatar(cu.getAvatar());
            }
            commentVOs.add(cvo);
        }
        vo.setComments(commentVOs);

        return vo;
    }

    @Override
    public Task updateTask(Task task) {
        Task existing = getById(task.getId());
        if (existing == null) {
            throw BusinessException.notFound("任务不存在");
        }
        // 权限校验：只有任务创建者可以更新
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId != null && !currentUserId.equals(existing.getCreatorId())) {
            throw BusinessException.forbidden("只有任务创建者可以修改任务");
        }
        if (task.getTitle() != null) {
            if (task.getTitle().trim().isEmpty()) {
                throw BusinessException.badRequest("任务标题不能为空");
            }
            existing.setTitle(task.getTitle());
        }
        if (task.getDescription() != null) existing.setDescription(task.getDescription());
        if (task.getRequiredSkills() != null) existing.setRequiredSkills(task.getRequiredSkills());
        if (task.getPriority() != null) {
            if (!VALID_PRIORITIES.contains(task.getPriority())) {
                throw BusinessException.badRequest("无效的优先级: " + task.getPriority());
            }
            existing.setPriority(task.getPriority());
        }
        if (task.getDeadline() != null) existing.setDeadline(task.getDeadline());
        if (task.getStatus() != null) {
            if (!VALID_STATUSES.contains(task.getStatus())) {
                throw BusinessException.badRequest("无效的任务状态: " + task.getStatus());
            }
            existing.setStatus(task.getStatus());
        }
        updateById(existing);
        return existing;
    }

    @Override
    public void updateStatus(Long id, String status) {
        Task task = getById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        if (!VALID_STATUSES.contains(status)) {
            throw BusinessException.badRequest("无效的任务状态: " + status);
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId != null && !currentUserId.equals(task.getCreatorId())) {
            throw BusinessException.forbidden("只有任务创建者可以更新任务状态");
        }
        task.setStatus(status);
        updateById(task);
    }

    @Override
    public List<Task> getByProject(Long projectId) {
        return list(new LambdaQueryWrapper<Task>()
                .eq(Task::getProjectId, projectId)
                .orderByDesc(Task::getCreatedAt));
    }

    @Override
    public List<Task> getMyTasks(Long userId) {
        // 获取用户被指派的所有任务
        List<TaskAssignee> assignments = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>().eq(TaskAssignee::getUserId, userId));
        if (assignments.isEmpty()) return Collections.emptyList();

        List<Long> taskIds = assignments.stream().map(TaskAssignee::getTaskId).collect(Collectors.toList());
        return list(new LambdaQueryWrapper<Task>()
                .in(Task::getId, taskIds)
                .orderByDesc(Task::getCreatedAt));
    }

    @Override
    public List<Task> getManagedTasks(Long leaderId) {
        // 获取负责人创建的所有任务
        return list(new LambdaQueryWrapper<Task>()
                .eq(Task::getCreatorId, leaderId)
                .orderByDesc(Task::getCreatedAt));
    }

    // ==================== 人员指派 ====================

    @Override
    public List<TaskDetailVO.AssigneeVO> recommendAssignees(Long taskId) {
        Task task = getById(taskId);
        if (task == null) throw BusinessException.notFound("任务不存在");

        Project project = projectMapper.selectById(task.getProjectId());
        if (project == null) throw BusinessException.notFound("项目不存在");

        // 获取团队成员
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, project.getTeamId()));
        if (members.isEmpty()) return Collections.emptyList();

        // 获取任务所需技能
        Set<String> requiredSkills = new HashSet<>();
        if (task.getRequiredSkills() != null && !task.getRequiredSkills().isEmpty()) {
            requiredSkills = Arrays.stream(task.getRequiredSkills().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }

        // 计算每个成员的技能匹配度
        List<TaskDetailVO.AssigneeVO> recommendations = new ArrayList<>();
        for (TeamMember member : members) {
            if (member.getUserId().equals(task.getCreatorId())) continue; // 跳过创建者自己

            List<UserSkill> skills = userSkillMapper.selectList(
                    new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, member.getUserId()));
            Set<String> userSkillNames = skills.stream()
                    .map(UserSkill::getSkillName)
                    .collect(Collectors.toSet());

            double matchScore = 0;
            if (!requiredSkills.isEmpty()) {
                long matched = requiredSkills.stream().filter(userSkillNames::contains).count();
                matchScore = (double) matched / requiredSkills.size();
            }

            User user = userMapper.selectById(member.getUserId());
            TaskDetailVO.AssigneeVO vo = new TaskDetailVO.AssigneeVO();
            vo.setUserId(member.getUserId());
            vo.setUserName(user != null ? user.getUsername() : null);
            vo.setUserAvatar(user != null ? user.getAvatar() : null);
            vo.setSkillMatchScore(String.format("%.2f", matchScore));
            recommendations.add(vo);
        }

        // 按匹配度降序，返回Top-5
        recommendations.sort((a, b) -> {
            double sa = Double.parseDouble(a.getSkillMatchScore());
            double sb = Double.parseDouble(b.getSkillMatchScore());
            return Double.compare(sb, sa);
        });
        return recommendations.size() > 5 ? recommendations.subList(0, 5) : recommendations;
    }

    @Override
    @Transactional
    public void addAssignee(Long taskId, Long userId, String assignmentNote) {
        Task task = getById(taskId);
        if (task == null) throw BusinessException.notFound("任务不存在");

        // 权限校验
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId != null && !currentUserId.equals(task.getCreatorId())) {
            throw BusinessException.forbidden("只有任务创建者可以指派执行人");
        }

        // 检查是否已存在
        Long count = taskAssigneeMapper.selectCount(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, taskId)
                        .eq(TaskAssignee::getUserId, userId));
        if (count > 0) {
            throw BusinessException.conflict("该用户已是任务执行人");
        }

        // 计算技能匹配度
        BigDecimal matchScore = calculateSkillMatch(task, userId);

        TaskAssignee assignee = new TaskAssignee();
        assignee.setTaskId(taskId);
        assignee.setUserId(userId);
        assignee.setAssignmentNote(assignmentNote);
        assignee.setSkillMatchScore(matchScore);
        assignee.setStatus("ASSIGNED");
        taskAssigneeMapper.insert(assignee);

        // 发送通知
        sendNotification(userId, "新任务指派",
                "你被指派了任务：「" + task.getTitle() + "」" +
                        (assignmentNote != null ? "，分工说明：" + assignmentNote : ""));
    }

    @Override
    @Transactional
    public void removeAssignee(Long taskId, Long userId) {
        Task task = getById(taskId);
        if (task == null) throw BusinessException.notFound("任务不存在");

        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId != null && !currentUserId.equals(task.getCreatorId())) {
            throw BusinessException.forbidden("只有任务创建者可以移除执行人");
        }

        taskAssigneeMapper.delete(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, taskId)
                        .eq(TaskAssignee::getUserId, userId));
    }

    @Override
    @Transactional
    public void acceptTask(Long taskId, boolean accept) {
        Long currentUserId = UserContext.getCurrentUserId();
        TaskAssignee assignee = taskAssigneeMapper.selectOne(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, taskId)
                        .eq(TaskAssignee::getUserId, currentUserId));
        if (assignee == null) {
            throw BusinessException.notFound("你未被指派到此任务");
        }
        assignee.setStatus(accept ? "ACCEPTED" : "REJECTED");
        if (accept) {
            assignee.setAcceptedAt(LocalDateTime.now());
            // 接受后任务进入进行中
            Task task = getById(taskId);
            if (task != null && "TODO".equals(task.getStatus())) {
                task.setStatus("IN_PROGRESS");
                updateById(task);
            }
        }
        taskAssigneeMapper.updateById(assignee);
    }

    @Override
    public List<TaskAssignee> getAssignees(Long taskId) {
        return taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, taskId));
    }

    // ==================== 成果提交 ====================

    @Override
    @Transactional
    public TaskSubmission submitResult(SubmissionRequest request) {
        if (request.getTaskId() == null) throw BusinessException.badRequest("请指定任务");

        Task task = getById(request.getTaskId());
        if (task == null) throw BusinessException.notFound("任务不存在");

        Long currentUserId = UserContext.getCurrentUserId();

        // 校验是否为任务执行人
        Long assigneeCount = taskAssigneeMapper.selectCount(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, request.getTaskId())
                        .eq(TaskAssignee::getUserId, currentUserId));
        if (assigneeCount == 0) {
            throw BusinessException.forbidden("只有任务执行人可以提交成果");
        }

        TaskSubmission submission = new TaskSubmission();
        submission.setTaskId(request.getTaskId());
        submission.setSubmitterId(currentUserId);
        submission.setTitle(request.getTitle() != null ? request.getTitle() : "阶段性成果");
        submission.setDescription(request.getDescription());
        submission.setFileUrls(request.getFileUrls() != null ? toJsonArray(request.getFileUrls()) : null);
        submission.setStatus("PENDING_REVIEW");
        taskSubmissionMapper.insert(submission);

        // 更新任务状态为待验收
        if (!"SUBMITTED".equals(task.getStatus()) && !"REVIEWING".equals(task.getStatus())) {
            task.setStatus("SUBMITTED");
            updateById(task);
        }

        // 通知任务创建者
        User submitter = userMapper.selectById(currentUserId);
        sendNotification(task.getCreatorId(), "新成果提交",
                (submitter != null ? submitter.getUsername() : "成员") +
                        " 提交了任务「" + task.getTitle() + "」的成果，请验收");

        return submission;
    }

    @Override
    public List<TaskSubmission> getSubmissions(Long taskId) {
        return taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskId, taskId)
                        .orderByDesc(TaskSubmission::getCreatedAt));
    }

    @Override
    @Transactional
    public void reviewSubmission(Long submissionId, ReviewRequest request) {
        TaskSubmission submission = taskSubmissionMapper.selectById(submissionId);
        if (submission == null) throw BusinessException.notFound("成果提交记录不存在");

        Task task = getById(submission.getTaskId());
        if (task == null) throw BusinessException.notFound("关联任务不存在");

        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId != null && !currentUserId.equals(task.getCreatorId())) {
            throw BusinessException.forbidden("只有任务创建者可以验收成果");
        }

        if (!"APPROVED".equals(request.getAction()) && !"REJECTED".equals(request.getAction())) {
            throw BusinessException.badRequest("验收操作只能是 APPROVED 或 REJECTED");
        }

        submission.setStatus(request.getAction());
        submission.setReviewComment(request.getComment());
        submission.setReviewerId(currentUserId);
        submission.setReviewedAt(LocalDateTime.now());
        taskSubmissionMapper.updateById(submission);

        // 通知提交人
        String actionText = "APPROVED".equals(request.getAction()) ? "通过" : "退回";
        sendNotification(submission.getSubmitterId(), "成果验收" + actionText,
                "你的成果「" + submission.getTitle() + "」已被" + actionText +
                        (request.getComment() != null ? "，意见：" + request.getComment() : ""));
    }

    // ==================== 评论 ====================

    @Override
    @Transactional
    public TaskComment addComment(Long taskId, String content, List<Long> mentionedUserIds) {
        if (content == null || content.trim().isEmpty()) {
            throw BusinessException.badRequest("评论内容不能为空");
        }

        Task task = getById(taskId);
        if (task == null) throw BusinessException.notFound("任务不存在");

        Long currentUserId = UserContext.getCurrentUserId();

        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setUserId(currentUserId);
        comment.setContent(content);
        comment.setMentionedUserIds(mentionedUserIds != null ? toJsonArray(mentionedUserIds) : null);
        taskCommentMapper.insert(comment);

        // 通知被@的用户
        if (mentionedUserIds != null && !mentionedUserIds.isEmpty()) {
            User commenter = userMapper.selectById(currentUserId);
            for (Long uid : mentionedUserIds) {
                if (!uid.equals(currentUserId)) {
                    sendNotification(uid, "评论提及",
                            (commenter != null ? commenter.getUsername() : "有人") +
                                    " 在任务「" + task.getTitle() + "」中@了你");
                }
            }
        }

        return comment;
    }

    @Override
    public List<TaskComment> getComments(Long taskId) {
        return taskCommentMapper.selectList(
                new LambdaQueryWrapper<TaskComment>()
                        .eq(TaskComment::getTaskId, taskId)
                        .orderByAsc(TaskComment::getCreatedAt));
    }

    // ==================== 私有辅助方法 ====================

    private BigDecimal calculateSkillMatch(Task task, Long userId) {
        if (task.getRequiredSkills() == null || task.getRequiredSkills().isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<String> required = Arrays.stream(task.getRequiredSkills().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (required.isEmpty()) return BigDecimal.ZERO;

        List<UserSkill> userSkills = userSkillMapper.selectList(
                new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId));
        Set<String> userSkillNames = userSkills.stream()
                .map(UserSkill::getSkillName)
                .collect(Collectors.toSet());

        long matched = required.stream().filter(userSkillNames::contains).count();
        return BigDecimal.valueOf(matched)
                .divide(BigDecimal.valueOf(required.size()), 2, RoundingMode.HALF_UP);
    }

    private void sendNotification(Long userId, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType("TASK");
        n.setIsRead(0);
        notificationMapper.insert(n);
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        // 简单解析 ["a","b"] 格式
        json = json.trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            String inner = json.substring(1, json.length() - 1).trim();
            if (inner.isEmpty()) return Collections.emptyList();
            return Arrays.stream(inner.split(","))
                    .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseJsonLongArray(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        json = json.trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            String inner = json.substring(1, json.length() - 1).trim();
            if (inner.isEmpty()) return Collections.emptyList();
            return Arrays.stream(inner.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String toJsonArray(List<?> list) {
        if (list == null || list.isEmpty()) return "[]";
        if (list.get(0) instanceof String) {
            return "[" + list.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]";
        }
        return "[" + list.stream().map(Object::toString).collect(Collectors.joining(",")) + "]";
    }
}
