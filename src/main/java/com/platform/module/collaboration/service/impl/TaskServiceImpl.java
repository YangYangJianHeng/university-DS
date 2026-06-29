package com.platform.module.collaboration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.common.util.UserContext;
import com.platform.module.collaboration.dto.*;
import com.platform.module.collaboration.entity.*;
import com.platform.module.collaboration.mapper.*;
import com.platform.module.collaboration.service.TaskService;
import com.platform.module.file.entity.FileEntity;
import com.platform.module.file.mapper.FileMapper;
import com.platform.module.notification.entity.Notification;
import com.platform.module.notification.mapper.NotificationMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.teacher.entity.Teacher;
import com.platform.module.teacher.mapper.TeacherMapper;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.entity.TeamTeacher;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.team.mapper.TeamTeacherMapper;
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
    private final TeamTeacherMapper teamTeacherMapper;
    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final UserSkillMapper userSkillMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final TaskCommentMapper taskCommentMapper;
    private final NotificationMapper notificationMapper;
    private final FileMapper fileMapper;

    private static final Set<String> VALID_STATUSES = Set.of("TODO", "IN_PROGRESS", "SUBMITTED", "REVIEWING", "DONE", "OVERDUE");
    private static final Set<String> VALID_PRIORITIES = Set.of("HIGH", "MEDIUM", "LOW");

    public TaskServiceImpl(ProjectMapper projectMapper,
                           TeamMemberMapper teamMemberMapper,
                           TeamTeacherMapper teamTeacherMapper,
                           TeacherMapper teacherMapper,
                           UserMapper userMapper,
                           UserSkillMapper userSkillMapper,
                           TaskAssigneeMapper taskAssigneeMapper,
                           TaskSubmissionMapper taskSubmissionMapper,
                           TaskCommentMapper taskCommentMapper,
                           NotificationMapper notificationMapper,
                           FileMapper fileMapper) {
        this.projectMapper = projectMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.teamTeacherMapper = teamTeacherMapper;
        this.teacherMapper = teacherMapper;
        this.userMapper = userMapper;
        this.userSkillMapper = userSkillMapper;
        this.taskAssigneeMapper = taskAssigneeMapper;
        this.taskSubmissionMapper = taskSubmissionMapper;
        this.taskCommentMapper = taskCommentMapper;
        this.notificationMapper = notificationMapper;
        this.fileMapper = fileMapper;
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

        // 权限校验：项目负责人、ADMIN、或该团队已审核通过的导师 可以创建任务
        Long currentUserId = UserContext.getCurrentUserId();
        String currentRole = UserContext.getCurrentUserRole();
        if (currentUserId != null && !currentUserId.equals(project.getLeaderId())
                && !"ADMIN".equals(currentRole)) {
            // 检查是否是团队的导师（需审核通过）
            boolean isTeacher = isApprovedTeacher(project.getTeamId(), currentUserId);
            if (!isTeacher) {
                throw BusinessException.forbidden("只有项目负责人、管理员或已审核的导师可以创建任务");
            }
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

        // 关联附件文件到任务（将上传好的文件绑定到此任务）
        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            for (Long fileId : request.getFileIds()) {
                FileEntity file = fileMapper.selectById(fileId);
                if (file != null && file.getIsDeleted() == 0) {
                    file.setTaskId(task.getId());
                    fileMapper.updateById(file);
                }
            }
        }

        // 通知项目所有团队成员：新任务发布
        notifyProjectMembers(project, task, currentUserId);

        // 指派执行人（内部有各自的指派通知）
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

        Long currentUserId = UserContext.getCurrentUserId();

        // ✨ 状态自动流转逻辑
        // 1. 执行人首次查看 → 待处理→进行中
        if (currentUserId != null && "TODO".equals(task.getStatus())) {
            boolean isAssignee = taskAssigneeMapper.selectCount(
                    new LambdaQueryWrapper<TaskAssignee>()
                            .eq(TaskAssignee::getTaskId, taskId)
                            .eq(TaskAssignee::getUserId, currentUserId)) > 0;
            if (isAssignee) {
                task.setStatus("IN_PROGRESS");
                updateById(task);
                // 同时标记执行人为已接受
                TaskAssignee assignee = taskAssigneeMapper.selectOne(
                        new LambdaQueryWrapper<TaskAssignee>()
                                .eq(TaskAssignee::getTaskId, taskId)
                                .eq(TaskAssignee::getUserId, currentUserId));
                if (assignee != null && "ASSIGNED".equals(assignee.getStatus())) {
                    assignee.setStatus("ACCEPTED");
                    assignee.setAcceptedAt(LocalDateTime.now());
                    taskAssigneeMapper.updateById(assignee);
                }
            }
        }

        // 2. 任务发布人查看已提交成果 → 待验收→验收中
        if (currentUserId != null && "SUBMITTED".equals(task.getStatus())
                && currentUserId.equals(task.getCreatorId())) {
            task.setStatus("REVIEWING");
            updateById(task);
        }

        // 重新获取任务以确保状态是最新的
        task = getById(taskId);

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

        // 成果提交列表（带可见性过滤）
        List<TaskSubmission> submissions = getSubmissions(taskId);
        List<TaskDetailVO.SubmissionVO> submissionVOs = new ArrayList<>();
        for (TaskSubmission s : submissions) {
            // ✨ 可见性过滤：PENDING_REVIEW / REJECTED 仅提交者和创建者可见；APPROVED 全队可见
            if ("PENDING_REVIEW".equals(s.getStatus()) || "REJECTED".equals(s.getStatus())) {
                if (currentUserId != null
                        && !currentUserId.equals(s.getSubmitterId())
                        && !currentUserId.equals(task.getCreatorId())) {
                    continue; // 跳过不可见的提交
                }
            }

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
                    List<String> urls = parseJsonArray(s.getFileUrls());
                    svo.setFileUrls(urls);
                    // ✨ 构建文件元数据供前端展示（预览/下载地址、文件名、类型）
                    List<TaskDetailVO.FileInfoVO> fileInfos = buildFileInfos(urls);
                    svo.setFileInfos(fileInfos);
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
    public Task updateTask(Long taskId, TaskUpdateRequest request) {
        Task existing = getById(taskId);
        if (existing == null) {
            throw BusinessException.notFound("任务不存在");
        }
        // 权限校验：只有任务创建者可以更新（含导师）
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId != null && !currentUserId.equals(existing.getCreatorId())) {
            throw BusinessException.forbidden("只有任务创建者可以修改任务");
        }

        boolean changed = false;

        if (request.getTitle() != null) {
            if (request.getTitle().trim().isEmpty()) {
                throw BusinessException.badRequest("任务标题不能为空");
            }
            existing.setTitle(request.getTitle());
            changed = true;
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
            changed = true;
        }
        if (request.getRequiredSkills() != null) {
            existing.setRequiredSkills(request.getRequiredSkills());
            changed = true;
        }
        if (request.getPriority() != null) {
            if (!VALID_PRIORITIES.contains(request.getPriority())) {
                throw BusinessException.badRequest("无效的优先级: " + request.getPriority());
            }
            existing.setPriority(request.getPriority());
            changed = true;
        }
        if (request.getDeadline() != null && !request.getDeadline().isEmpty()) {
            try {
                LocalDateTime deadline = parseFlexibleDeadline(request.getDeadline());
                existing.setDeadline(deadline);
                changed = true;
            } catch (Exception e) {
                throw BusinessException.badRequest("截止日期格式错误，请使用：yyyy-MM-dd HH:mm:ss");
            }
        }
        if (request.getStatus() != null) {
            if (!VALID_STATUSES.contains(request.getStatus())) {
                throw BusinessException.badRequest("无效的任务状态: " + request.getStatus());
            }
            existing.setStatus(request.getStatus());
            changed = true;
        }

        if (changed) {
            updateById(existing);
        }

        // ===== 处理执行人变更（换人功能）=====
        // 先移除指定的执行人
        if (request.getRemoveAssigneeIds() != null && !request.getRemoveAssigneeIds().isEmpty()) {
            for (Long userId : request.getRemoveAssigneeIds()) {
                removeAssignee(taskId, userId);
            }
        }

        // 再添加新的执行人
        if (request.getAddAssignees() != null && !request.getAddAssignees().isEmpty()) {
            for (TaskUpdateRequest.AssigneeInfo info : request.getAddAssignees()) {
                if (info.getUserId() != null) {
                    // 检查是否已经是指派人
                    Long count = taskAssigneeMapper.selectCount(
                            new LambdaQueryWrapper<TaskAssignee>()
                                    .eq(TaskAssignee::getTaskId, taskId)
                                    .eq(TaskAssignee::getUserId, info.getUserId()));
                    if (count == 0) {
                        addAssignee(taskId, info.getUserId(), info.getAssignmentNote());
                    }
                }
            }
        }

        // 通知所有执行人任务有更新
        if (changed) {
            notifyAssigneesAboutUpdate(existing, request.getTitle() != null ? request.getTitle() : null,
                    request.getDescription() != null ? request.getDescription() : null);
        }

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
        // 只有任务创建者或管理员可以手动更新任务状态
        if (currentUserId != null && !currentUserId.equals(task.getCreatorId())) {
            // 允许有管理员权限的用户通过
            throw BusinessException.forbidden("只有任务创建者可以更新任务状态");
        }
        task.setStatus(status);
        updateById(task);

        // 通知所有执行人状态变更
        notifyAssigneesStatusChange(task);
    }

    @Override
    @Transactional
    public void completeTask(Long taskId) {
        Task task = getById(taskId);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        // 只有验收中状态可以完成
        if (!"REVIEWING".equals(task.getStatus())) {
            throw BusinessException.badRequest("只有验收中的任务可以标记为完成");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        // 只有任务创建者可以完成
        if (currentUserId != null && !currentUserId.equals(task.getCreatorId())) {
            throw BusinessException.forbidden("只有任务创建者可以完成验收");
        }
        task.setStatus("DONE");
        updateById(task);

        // 通知所有执行人
        List<TaskAssignee> assignees = getAssignees(taskId);
        for (TaskAssignee a : assignees) {
            sendNotification(a.getUserId(), "任务已完成",
                    "任务「" + task.getTitle() + "」已通过验收，标记为已完成");
        }
    }

    @Override
    public List<Task> getByProject(Long projectId) {
        List<Task> tasks = list(new LambdaQueryWrapper<Task>()
                .eq(Task::getProjectId, projectId)
                .orderByDesc(Task::getCreatedAt));
        checkAndMarkOverdue(tasks);
        enrichTasksWithUserInfo(tasks);
        return tasks;
    }

    @Override
    public List<Task> getMyTasks(Long userId) {
        // 获取用户被指派的所有任务
        List<TaskAssignee> assignments = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>().eq(TaskAssignee::getUserId, userId));
        if (assignments.isEmpty()) return Collections.emptyList();

        List<Long> taskIds = assignments.stream().map(TaskAssignee::getTaskId).collect(Collectors.toList());
        List<Task> tasks = list(new LambdaQueryWrapper<Task>()
                .in(Task::getId, taskIds)
                .orderByDesc(Task::getCreatedAt));

        // 过滤掉已关闭项目的任务
        tasks = filterOutClosedProjectTasks(tasks);

        // 自动检测逾期：截止日期已过且状态为待处理或进行中
        checkAndMarkOverdue(tasks);
        enrichTasksWithUserInfo(tasks);
        return tasks;
    }

    @Override
    public List<Task> getManagedTasks(Long leaderId) {
        // 1. 获取用户作为负责人的项目（排除已关闭和已归档的）
        List<Project> managedProjects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getLeaderId, leaderId)
                        .ne(Project::getStatus, "CLOSED")
                        .ne(Project::getStatus, "ARCHIVED"));
        if (managedProjects.isEmpty()) return Collections.emptyList();

        List<Long> projectIds = managedProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());

        // 2. 只获取这些项目中由用户创建的任务
        List<Task> tasks = list(new LambdaQueryWrapper<Task>()
                .in(Task::getProjectId, projectIds)
                .eq(Task::getCreatorId, leaderId)
                .orderByDesc(Task::getCreatedAt));

        // 自动检测逾期：截止日期已过且状态为待处理或进行中
        checkAndMarkOverdue(tasks);
        enrichTasksWithUserInfo(tasks);
        return tasks;
    }

    @Override
    public List<Task> getTeamTasks(Long userId) {
        // 1. 获取用户所在的所有团队（排除已关闭的团队）
        List<TeamMember> memberships = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        if (memberships.isEmpty()) return Collections.emptyList();

        List<Long> teamIds = memberships.stream()
                .map(TeamMember::getTeamId)
                .distinct()
                .collect(Collectors.toList());

        // 2. 获取这些团队的所有项目（排除已关闭的项目）
        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .in(Project::getTeamId, teamIds)
                        .ne(Project::getStatus, "CLOSED"));
        if (projects.isEmpty()) return Collections.emptyList();

        List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());

        // 3. 获取这些项目的所有任务
        List<Task> tasks = list(new LambdaQueryWrapper<Task>()
                .in(Task::getProjectId, projectIds)
                .orderByDesc(Task::getCreatedAt));

        // 4. 自动检测逾期
        checkAndMarkOverdue(tasks);
        enrichTasksWithUserInfo(tasks);
        return tasks;
    }

    @Override
    public List<ProjectTasksVO> getTeamTasksGrouped(Long userId) {
        // 1. 获取用户所在的所有团队
        List<TeamMember> memberships = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        if (memberships.isEmpty()) return Collections.emptyList();

        List<Long> teamIds = memberships.stream()
                .map(TeamMember::getTeamId)
                .distinct()
                .collect(Collectors.toList());

        // 2. 获取这些团队的所有项目（排除已关闭的项目）
        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .in(Project::getTeamId, teamIds)
                        .ne(Project::getStatus, "CLOSED"));
        if (projects.isEmpty()) return Collections.emptyList();

        // 构建 projectId → Project 映射
        Map<Long, Project> projectMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());

        // 3. 获取这些项目的所有任务
        List<Task> tasks = list(new LambdaQueryWrapper<Task>()
                .in(Task::getProjectId, projectIds)
                .orderByDesc(Task::getCreatedAt));

        // 4. 自动检测逾期
        checkAndMarkOverdue(tasks);

        // 5. 批量获取创建者名称和头像
        Map<Long, String> creatorNameMap = new HashMap<>();
        Map<Long, String> creatorAvatarMap = new HashMap<>();
        if (!tasks.isEmpty()) {
            List<Long> creatorIds = tasks.stream()
                    .map(Task::getCreatorId)
                    .distinct()
                    .collect(Collectors.toList());
            for (User u : userMapper.selectBatchIds(creatorIds)) {
                creatorNameMap.put(u.getId(), u.getUsername());
                creatorAvatarMap.put(u.getId(), u.getAvatar());
            }
        }

        // 6. 批量获取所有任务的执行人数
        Map<Long, Integer> assigneeCountMap = new HashMap<>();
        if (!tasks.isEmpty()) {
            List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());
            List<TaskAssignee> allAssignees = taskAssigneeMapper.selectList(
                    new LambdaQueryWrapper<TaskAssignee>().in(TaskAssignee::getTaskId, taskIds));
            for (TaskAssignee a : allAssignees) {
                assigneeCountMap.merge(a.getTaskId(), 1, Integer::sum);
            }
        }

        // 7. 按项目分组
        Map<Long, List<ProjectTasksVO.TaskVO>> grouped = tasks.stream()
                .collect(Collectors.groupingBy(Task::getProjectId,
                        Collectors.mapping(task -> {
                            ProjectTasksVO.TaskVO vo = new ProjectTasksVO.TaskVO();
                            vo.setId(task.getId());
                            vo.setProjectId(task.getProjectId());
                            vo.setCreatorId(task.getCreatorId());
                            vo.setCreatorName(creatorNameMap.getOrDefault(task.getCreatorId(), null));
                            vo.setCreatorAvatar(creatorAvatarMap.getOrDefault(task.getCreatorId(), null));
                            vo.setTitle(task.getTitle());
                            vo.setDescription(task.getDescription());
                            vo.setRequiredSkills(task.getRequiredSkills());
                            vo.setPriority(task.getPriority());
                            vo.setStatus(task.getStatus());
                            vo.setDeadline(task.getDeadline());
                            vo.setCreatedAt(task.getCreatedAt());
                            vo.setAssigneeCount(assigneeCountMap.getOrDefault(task.getId(), 0));
                            return vo;
                        }, Collectors.toList())));

        // 8. 构建响应
        return projects.stream().map(project -> {
            ProjectTasksVO vo = new ProjectTasksVO();
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getName());
            vo.setTeamId(project.getTeamId());
            vo.setTasks(grouped.getOrDefault(project.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 自动检测并标记逾期任务
     * 截止日期已过 且 状态为TODO/IN_PROGRESS → 标记为OVERDUE
     */
    private void checkAndMarkOverdue(List<Task> tasks) {
        LocalDateTime now = LocalDateTime.now();
        for (Task task : tasks) {
            if (task.getDeadline() != null
                    && task.getDeadline().isBefore(now)
                    && ("TODO".equals(task.getStatus()) || "IN_PROGRESS".equals(task.getStatus()))) {
                task.setStatus("OVERDUE");
                updateById(task);
            }
        }
    }

    /**
     * 过滤掉已关闭项目的任务
     */
    private List<Task> filterOutClosedProjectTasks(List<Task> tasks) {
        if (tasks.isEmpty()) return tasks;
        // 收集所有 projectId
        List<Long> projectIds = tasks.stream()
                .map(Task::getProjectId)
                .distinct()
                .collect(Collectors.toList());
        // 查询这些项目中已关闭的
        List<Project> closedProjects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .in(Project::getId, projectIds)
                        .eq(Project::getStatus, "CLOSED"));
        if (closedProjects.isEmpty()) return tasks;
        // 收集已关闭的项目ID集合
        Set<Long> closedProjectIds = closedProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toSet());
        // 过滤掉属于已关闭项目的任务
        return tasks.stream()
                .filter(t -> !closedProjectIds.contains(t.getProjectId()))
                .collect(Collectors.toList());
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

        // 权限校验：任务创建者 或 用户自指派（团队管理人员可为自己分配任务）
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId != null && !currentUserId.equals(task.getCreatorId())) {
            // 允许用户为自己分配任务（自指派），需验证是该项目的团队成员
            if (currentUserId.equals(userId)) {
                Project project = projectMapper.selectById(task.getProjectId());
                if (project == null) {
                    throw BusinessException.notFound("所属项目不存在");
                }
                // 验证是团队成员或导师
                boolean isMember = teamMemberMapper.selectCount(
                        new LambdaQueryWrapper<TeamMember>()
                                .eq(TeamMember::getTeamId, project.getTeamId())
                                .eq(TeamMember::getUserId, currentUserId)) > 0;
                boolean isTeacher = isApprovedTeacher(project.getTeamId(), currentUserId);
                if (!isMember && !isTeacher) {
                    throw BusinessException.forbidden("你不在该项目所属团队中，无法为自己分配任务");
                }
                // 自指派通过，继续执行
            } else {
                throw BusinessException.forbidden("只有任务创建者可以指派执行人");
            }
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
        // 权限：任务创建者 或 执行人自己申请退出
        if (currentUserId != null && !currentUserId.equals(task.getCreatorId())
                && !currentUserId.equals(userId)) {
            throw BusinessException.forbidden("只有任务创建者可以移除执行人，或执行人自行退出");
        }

        taskAssigneeMapper.delete(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, taskId)
                        .eq(TaskAssignee::getUserId, userId));

        // 通知被移除的执行人
        sendNotification(userId, "任务取消指派",
                "你已被移出任务「" + task.getTitle() + "」");
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
        Task task = getById(taskId);
        if (accept) {
            assignee.setAcceptedAt(LocalDateTime.now());
            // 接受后任务进入进行中
            if (task != null && "TODO".equals(task.getStatus())) {
                task.setStatus("IN_PROGRESS");
                updateById(task);
            }
        }
        taskAssigneeMapper.updateById(assignee);

        // 通知任务创建者
        if (task != null) {
            User acceptor = userMapper.selectById(currentUserId);
            String actionText = accept ? "接受了" : "拒绝了";
            sendNotification(task.getCreatorId(), "任务" + (accept ? "接受" : "拒绝"),
                    (acceptor != null ? acceptor.getUsername() : "成员") +
                            " " + actionText + " 任务「" + task.getTitle() + "」");
        }
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
            // 也允许导师验收
            Project project = projectMapper.selectById(task.getProjectId());
            if (project == null || !isApprovedTeacher(project.getTeamId(), currentUserId)) {
                throw BusinessException.forbidden("只有任务创建者或导师可以验收成果");
            }
        }

        if (!"APPROVED".equals(request.getAction()) && !"REJECTED".equals(request.getAction())) {
            throw BusinessException.badRequest("验收操作只能是 APPROVED 或 REJECTED");
        }

        submission.setStatus(request.getAction());
        submission.setReviewComment(request.getComment());
        submission.setReviewerId(currentUserId);
        submission.setReviewedAt(LocalDateTime.now());
        taskSubmissionMapper.updateById(submission);

        // ✨ 更新任务状态
        if ("APPROVED".equals(request.getAction())) {
            // 验收通过 → 任务完成
            task.setStatus("DONE");
            updateById(task);
            // 通知所有执行人任务已完成
            List<TaskAssignee> assignees = getAssignees(task.getId());
            for (TaskAssignee a : assignees) {
                sendNotification(a.getUserId(), "任务已完成",
                        "你参与的任务「" + task.getTitle() + "」已通过验收，标记为已完成");
            }
        } else {
            // 退回 → 任务回到进行中，执行人修改后重新提交
            task.setStatus("IN_PROGRESS");
            updateById(task);
            // 通知所有执行人任务被退回
            List<TaskAssignee> assignees = getAssignees(task.getId());
            for (TaskAssignee a : assignees) {
                sendNotification(a.getUserId(), "成果退回",
                        "你提交的成果「" + submission.getTitle() + "」已被退回，任务「" + task.getTitle() + "」已回到进行中");
            }
        }

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

    /** 通知项目所在团队的所有成员：新任务发布 */
    private void notifyProjectMembers(Project project, Task task, Long creatorId) {
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, project.getTeamId()));
        for (TeamMember m : members) {
            // 不通知创建者本人
            if (m.getUserId().equals(creatorId)) continue;
            sendNotification(m.getUserId(), "新任务发布",
                    "项目「" + project.getName() + "」发布了新任务：「" + task.getTitle() + "」");
        }
    }

    /** 通知所有执行人任务内容有更新 */
    /**
     * 灵活解析 deadline 字符串，兼容多种格式（前端 el-date-picker 可能发送不同格式）
     */
    private LocalDateTime parseFlexibleDeadline(String deadlineStr) {
        // 尝试的格式列表（按优先级排列）
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",      // value-format 正常输出
                "yyyy-MM-dd'T'HH:mm:ss",    // ISO 格式（Jackson LocalDateTime 默认序列化）
                "yyyy-MM-dd'T'HH:mm:ss.SSS",// 带毫秒的 ISO
                "yyyy-MM-dd HH:mm",         // 无秒格式
                "yyyy-MM-dd'T'HH:mm",       // ISO 无秒
        };

        for (String pattern : patterns) {
            try {
                return LocalDateTime.parse(deadlineStr,
                        java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // 尝试下一个格式
            }
        }

        throw new IllegalArgumentException("无法解析日期: " + deadlineStr);
    }

    private void notifyAssigneesAboutUpdate(Task existing, String titleChanged, String descChanged) {
        List<TaskAssignee> assignees = getAssignees(existing.getId());
        if (assignees.isEmpty()) return;

        String detail = "基本信息";
        if (titleChanged != null) detail = "任务信息";
        if (descChanged != null) detail = "任务描述";

        for (TaskAssignee a : assignees) {
            sendNotification(a.getUserId(), "任务更新",
                    "你参与的任务「" + existing.getTitle() + "」" + detail + "有更新，请及时查看");
        }
    }

    /** 通知所有执行人任务状态变更 */
    private void notifyAssigneesStatusChange(Task task) {
        List<TaskAssignee> assignees = getAssignees(task.getId());
        if (assignees.isEmpty()) return;

        String statusText = statusLabel(task.getStatus());
        for (TaskAssignee a : assignees) {
            sendNotification(a.getUserId(), "任务状态变更",
                    "任务「" + task.getTitle() + "」状态变更为：" + statusText);
        }
    }

    private String statusLabel(String status) {
        switch (status) {
            case "TODO": return "待处理";
            case "IN_PROGRESS": return "进行中";
            case "SUBMITTED": return "待验收";
            case "REVIEWING": return "验收中";
            case "DONE": return "已完成";
            case "OVERDUE": return "已逾期";
            default: return status;
        }
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

    /**
     * 从文件URL列表构建文件元数据（供前端展示：预览/下载地址、文件名、类型）
     * URL格式预期: /api/file/{id} 或 /api/file/{id}/preview 或 /api/file/{id}/download
     */
    private List<TaskDetailVO.FileInfoVO> buildFileInfos(List<String> fileUrls) {
        List<TaskDetailVO.FileInfoVO> infos = new ArrayList<>();
        if (fileUrls == null) return infos;

        java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile("/file/(\\d+)");
        for (String url : fileUrls) {
            TaskDetailVO.FileInfoVO info = new TaskDetailVO.FileInfoVO();
            java.util.regex.Matcher m = idPattern.matcher(url);
            if (m.find()) {
                Long fileId = Long.valueOf(m.group(1));
                FileEntity file = fileMapper.selectById(fileId);
                if (file != null) {
                    info.setName(file.getName());
                    info.setMimeType(file.getMimeType());
                    info.setFileSize(file.getFileSize());
                    info.setPreviewUrl("/api/file/" + fileId + "/preview");
                    info.setDownloadUrl("/api/file/" + fileId + "/download");
                } else {
                    info.setName("未知文件");
                    info.setPreviewUrl(url);
                    info.setDownloadUrl(url);
                }
            } else {
                // 无法解析ID，直接用原URL
                info.setName(extractFileName(url));
                info.setPreviewUrl(url);
                info.setDownloadUrl(url);
            }
            infos.add(info);
        }
        return infos;
    }

    private String extractFileName(String url) {
        if (url == null) return "文件";
        String name = url.substring(url.lastIndexOf('/') + 1);
        // 去掉query参数
        int q = name.indexOf('?');
        if (q > 0) name = name.substring(0, q);
        return name;
    }

    /**
     * 检查当前用户是否是指定团队的已审核导师
     */
    private boolean isApprovedTeacher(Long teamId, Long userId) {
        // 找到用户对应的 teacher 记录
        Teacher teacher = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (teacher == null) return false;

        // 检查 team_teacher 关联
        Long count = teamTeacherMapper.selectCount(
                new LambdaQueryWrapper<TeamTeacher>()
                        .eq(TeamTeacher::getTeamId, teamId)
                        .eq(TeamTeacher::getTeacherId, teacher.getId())
                        .eq(TeamTeacher::getStatus, "APPROVED"));
        return count > 0;
    }

    @Override
    public List<Task> getTeacherTeamTasks(Long userId) {
        // 找到用户对应的 teacher
        Teacher teacher = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (teacher == null) return Collections.emptyList();

        // 获取导师审核通过的所有团队
        List<TeamTeacher> relations = teamTeacherMapper.selectList(
                new LambdaQueryWrapper<TeamTeacher>()
                        .eq(TeamTeacher::getTeacherId, teacher.getId())
                        .eq(TeamTeacher::getStatus, "APPROVED"));
        if (relations.isEmpty()) return Collections.emptyList();

        List<Long> teamIds = relations.stream()
                .map(TeamTeacher::getTeamId)
                .distinct()
                .collect(Collectors.toList());

        // 获取这些团队的所有项目（排除已关闭的项目）
        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .in(Project::getTeamId, teamIds)
                        .ne(Project::getStatus, "CLOSED"));
        if (projects.isEmpty()) return Collections.emptyList();

        List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());

        // 获取所有任务
        List<Task> tasks = list(new LambdaQueryWrapper<Task>()
                .in(Task::getProjectId, projectIds)
                .orderByDesc(Task::getCreatedAt));

        checkAndMarkOverdue(tasks);
        enrichTasksWithUserInfo(tasks);
        return tasks;
    }

    @Override
    public List<Project> getTeacherProjects(Long userId) {
        Teacher teacher = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (teacher == null) return Collections.emptyList();

        List<TeamTeacher> relations = teamTeacherMapper.selectList(
                new LambdaQueryWrapper<TeamTeacher>()
                        .eq(TeamTeacher::getTeacherId, teacher.getId())
                        .eq(TeamTeacher::getStatus, "APPROVED"));
        if (relations.isEmpty()) return Collections.emptyList();

        List<Long> teamIds = relations.stream()
                .map(TeamTeacher::getTeamId)
                .distinct()
                .collect(Collectors.toList());

        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .in(Project::getTeamId, teamIds)
                        .ne(Project::getStatus, "CLOSED")
                        .orderByDesc(Project::getCreatedAt));
    }

    /**
     * 批量注入任务的创建人信息和执行人信息（姓名 + 头像）
     * 避免前端 N+1 请求逐个查询用户信息
     */
    private void enrichTasksWithUserInfo(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return;

        // 1. 收集所有 creatorId → 批量查用户表
        Set<Long> creatorIds = tasks.stream()
                .map(Task::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> creatorMap = new HashMap<>();
        if (!creatorIds.isEmpty()) {
            List<User> creators = userMapper.selectBatchIds(creatorIds);
            for (User u : creators) {
                creatorMap.put(u.getId(), u);
            }
        }

        // 2. 收集所有 taskId → 批量查 task_assignee 表
        List<Long> taskIds = tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());
        Map<Long, List<Long>> taskAssigneeUserIds = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<TaskAssignee> allAssignees = taskAssigneeMapper.selectList(
                    new LambdaQueryWrapper<TaskAssignee>().in(TaskAssignee::getTaskId, taskIds));
            for (TaskAssignee a : allAssignees) {
                taskAssigneeUserIds.computeIfAbsent(a.getTaskId(), k -> new ArrayList<>()).add(a.getUserId());
            }
        }

        // 3. 收集所有 assignee userId → 批量查用户表
        Set<Long> assigneeUserIds = taskAssigneeUserIds.values().stream()
                .flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, User> assigneeUserMap = new HashMap<>();
        if (!assigneeUserIds.isEmpty()) {
            List<User> assigneeUsers = userMapper.selectBatchIds(assigneeUserIds);
            for (User u : assigneeUsers) {
                assigneeUserMap.put(u.getId(), u);
            }
        }

        // 4. 回填到每个 Task
        for (Task task : tasks) {
            // 创建者信息
            if (task.getCreatorId() != null) {
                User creator = creatorMap.get(task.getCreatorId());
                if (creator != null) {
                    task.setCreatorName(creator.getUsername());
                    task.setCreatorAvatar(creator.getAvatar());
                }
            }

            // 执行人信息
            List<Long> uids = taskAssigneeUserIds.getOrDefault(task.getId(), Collections.emptyList());
            task.setAssigneeCount(uids.size());

            List<String> names = new ArrayList<>();
            List<String> avatars = new ArrayList<>();
            for (Long uid : uids) {
                User user = assigneeUserMap.get(uid);
                if (user != null) {
                    names.add(user.getUsername());
                    if (user.getAvatar() != null) {
                        avatars.add(user.getAvatar());
                    }
                }
            }
            task.setAssigneeNames(names.isEmpty() ? null : String.join(", ", names));
            task.setAssigneeAvatars(avatars);
        }
    }
}
