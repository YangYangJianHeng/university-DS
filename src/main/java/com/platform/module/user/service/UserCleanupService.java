package com.platform.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.achievement.mapper.AchievementMapper;
import com.platform.module.admin.entity.AuditLog;
import com.platform.module.admin.mapper.AuditLogMapper;
import com.platform.module.ai.entity.AIChatMessage;
import com.platform.module.ai.entity.AIChatSession;
import com.platform.module.ai.mapper.AIChatMessageMapper;
import com.platform.module.ai.mapper.AIChatSessionMapper;
import com.platform.module.badge.entity.Certificate;
import com.platform.module.badge.mapper.CertificateMapper;
import com.platform.module.calendar.entity.CalendarEvent;
import com.platform.module.calendar.mapper.CalendarEventMapper;
import com.platform.module.collaboration.entity.Message;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.entity.TaskAssignee;
import com.platform.module.collaboration.entity.TaskComment;
import com.platform.module.collaboration.entity.TaskSubmission;
import com.platform.module.collaboration.mapper.MessageMapper;
import com.platform.module.collaboration.mapper.TaskAssigneeMapper;
import com.platform.module.collaboration.mapper.TaskCommentMapper;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.collaboration.mapper.TaskSubmissionMapper;
import com.platform.module.notification.entity.Notification;
import com.platform.module.notification.mapper.NotificationMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.recruit.entity.Application;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.ApplicationMapper;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.teacher.entity.Teacher;
import com.platform.module.teacher.mapper.TeacherMapper;
import com.platform.module.team.entity.Team;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.entity.TeamTeacher;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.team.mapper.TeamTeacherMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.entity.UserExperience;
import com.platform.module.user.entity.UserSkill;
import com.platform.module.user.mapper.UserExperienceMapper;
import com.platform.module.user.mapper.UserMapper;
import com.platform.module.user.mapper.UserSkillMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户清理服务 — 负责注销账户时清理所有关联数据
 * 被 UserServiceImpl.deleteAccount 和 AdminServiceImpl.deleteUser 复用
 */
@Service
public class UserCleanupService {

    private static final Logger log = LoggerFactory.getLogger(UserCleanupService.class);

    private final UserMapper userMapper;
    private final UserSkillMapper userSkillMapper;
    private final UserExperienceMapper userExperienceMapper;
    private final NotificationMapper notificationMapper;
    private final MessageMapper messageMapper;
    private final ApplicationMapper applicationMapper;
    private final CalendarEventMapper calendarEventMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final TaskCommentMapper taskCommentMapper;
    private final AIChatMessageMapper aiChatMessageMapper;
    private final AIChatSessionMapper aiChatSessionMapper;
    private final CertificateMapper certificateMapper;
    private final TeacherMapper teacherMapper;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final RecruitMapper recruitMapper;
    private final AchievementMapper achievementMapper;
    private final TeamTeacherMapper teamTeacherMapper;
    private final AuditLogMapper auditLogMapper;

    public UserCleanupService(UserMapper userMapper,
                              UserSkillMapper userSkillMapper,
                              UserExperienceMapper userExperienceMapper,
                              NotificationMapper notificationMapper,
                              MessageMapper messageMapper,
                              ApplicationMapper applicationMapper,
                              CalendarEventMapper calendarEventMapper,
                              TaskAssigneeMapper taskAssigneeMapper,
                              TaskSubmissionMapper taskSubmissionMapper,
                              TaskCommentMapper taskCommentMapper,
                              AIChatMessageMapper aiChatMessageMapper,
                              AIChatSessionMapper aiChatSessionMapper,
                              CertificateMapper certificateMapper,
                              TeacherMapper teacherMapper,
                              TeamMapper teamMapper,
                              TeamMemberMapper teamMemberMapper,
                              ProjectMapper projectMapper,
                              TaskMapper taskMapper,
                              RecruitMapper recruitMapper,
                              AchievementMapper achievementMapper,
                              TeamTeacherMapper teamTeacherMapper,
                              AuditLogMapper auditLogMapper) {
        this.userMapper = userMapper;
        this.userSkillMapper = userSkillMapper;
        this.userExperienceMapper = userExperienceMapper;
        this.notificationMapper = notificationMapper;
        this.messageMapper = messageMapper;
        this.applicationMapper = applicationMapper;
        this.calendarEventMapper = calendarEventMapper;
        this.taskAssigneeMapper = taskAssigneeMapper;
        this.taskSubmissionMapper = taskSubmissionMapper;
        this.taskCommentMapper = taskCommentMapper;
        this.aiChatMessageMapper = aiChatMessageMapper;
        this.aiChatSessionMapper = aiChatSessionMapper;
        this.certificateMapper = certificateMapper;
        this.teacherMapper = teacherMapper;
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
        this.recruitMapper = recruitMapper;
        this.achievementMapper = achievementMapper;
        this.teamTeacherMapper = teamTeacherMapper;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 完全清理用户所有关联数据并删除用户
     * 操作顺序：先清理从表 → 再清理主表 → 最后删除 user 记录
     */
    @Transactional
    public void cleanupAndDeleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        log.info("开始清理用户 {} (phone={}) 的所有关联数据", userId, user.getPhone());

        // ---- 1. 个人数据 ----
        userSkillMapper.delete(new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId));
        userExperienceMapper.delete(new LambdaQueryWrapper<UserExperience>().eq(UserExperience::getUserId, userId));
        notificationMapper.delete(new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId));
        messageMapper.delete(new LambdaQueryWrapper<Message>().eq(Message::getSenderId, userId));
        applicationMapper.delete(new LambdaQueryWrapper<Application>().eq(Application::getApplicantId, userId));
        calendarEventMapper.delete(new LambdaQueryWrapper<CalendarEvent>().eq(CalendarEvent::getUserId, userId));

        // ---- 2. 协作数据 ----
        taskAssigneeMapper.delete(new LambdaQueryWrapper<TaskAssignee>().eq(TaskAssignee::getUserId, userId));
        taskSubmissionMapper.delete(new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getSubmitterId, userId).or().eq(TaskSubmission::getReviewerId, userId));
        taskCommentMapper.delete(new LambdaQueryWrapper<TaskComment>().eq(TaskComment::getUserId, userId));

        // ---- 3. AI聊天 ----
        // 先删消息再删会话
        List<AIChatSession> sessions = aiChatSessionMapper.selectList(
                new LambdaQueryWrapper<AIChatSession>().eq(AIChatSession::getUserId, userId));
        for (AIChatSession session : sessions) {
            aiChatMessageMapper.delete(new LambdaQueryWrapper<AIChatMessage>().eq(AIChatMessage::getSessionId, session.getId()));
        }
        aiChatSessionMapper.delete(new LambdaQueryWrapper<AIChatSession>().eq(AIChatSession::getUserId, userId));

        // ---- 4. 证书 ----
        certificateMapper.delete(new LambdaQueryWrapper<Certificate>().eq(Certificate::getUserId, userId));

        // ---- 5. 导师身份（如果是）----
        teacherMapper.delete(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        // 删除用户作为导师的团队邀请（team_teacher 有 teacher_id → user 的外键）
        teamTeacherMapper.delete(new LambdaQueryWrapper<TeamTeacher>().eq(TeamTeacher::getTeacherId, userId));

        // ---- 6. 先查用户发布的招募（后续步骤要用到 recruitId 列表）----
        List<Recruit> recruits = recruitMapper.selectList(
                new LambdaQueryWrapper<Recruit>().eq(Recruit::getPublisherId, userId));
        List<Long> recruitIds = recruits.stream().map(Recruit::getId).toList();

        // ---- 7. 团队 ----
        // 7a. 清理引用了用户招募的团队（先删这些团队及其子数据，否则后面删 recruit 会外键冲突）
        if (!recruitIds.isEmpty()) {
            List<Team> recruitTeams = teamMapper.selectList(
                    new LambdaQueryWrapper<Team>().in(Team::getRecruitId, recruitIds));
            for (Team team : recruitTeams) {
                deleteTeamCascade(team);
            }
        }
        // 7b. 用户为队长的团队（排除已在 7a 中删除的）
        List<Team> ledTeams = teamMapper.selectList(
                new LambdaQueryWrapper<Team>().eq(Team::getLeaderId, userId));
        for (Team team : ledTeams) {
            deleteTeamCascade(team);
        }
        // 7c. 用户作为普通成员的所有团队
        teamMemberMapper.delete(new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));

        // ---- 8. 招募及其申请（team 依赖已清理完，现在可以安全删除）----
        for (Recruit recruit : recruits) {
            applicationMapper.delete(new LambdaQueryWrapper<Application>().eq(Application::getRecruitId, recruit.getId()));
        }
        recruitMapper.delete(new LambdaQueryWrapper<Recruit>().eq(Recruit::getPublisherId, userId));

        // ---- 9. 用户作为项目负责人的项目 ----
        List<Project> ledProjects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getLeaderId, userId));
        for (Project project : ledProjects) {
            deleteTasksByProjectId(project.getId());
            achievementMapper.delete(new LambdaQueryWrapper<Achievement>().eq(Achievement::getProjectId, project.getId()));
        }
        projectMapper.delete(new LambdaQueryWrapper<Project>().eq(Project::getLeaderId, userId));

        // ---- 10. 提交的成果 ----
        achievementMapper.delete(new LambdaQueryWrapper<Achievement>().eq(Achievement::getSubmitterId, userId));

        // ---- 11. 审计日志和用户本身 ----
        auditLogMapper.delete(new LambdaQueryWrapper<AuditLog>().eq(AuditLog::getOperatorId, userId));
        userMapper.deleteById(userId);
        log.info("用户 {} 已完全清理并从数据库删除", userId);
    }

    /**
     * 级联删除团队及其下的项目、任务、成果、团队成员
     */
    private void deleteTeamCascade(Team team) {
        Long teamId = team.getId();
        // 删除团队下的项目及其任务
        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getTeamId, teamId));
        for (Project project : projects) {
            deleteTasksByProjectId(project.getId());
            achievementMapper.delete(new LambdaQueryWrapper<Achievement>().eq(Achievement::getProjectId, project.getId()));
        }
        projectMapper.delete(new LambdaQueryWrapper<Project>().eq(Project::getTeamId, teamId));
        // 删除直接关联到团队的成果（achievement 有 team_id 外键）
        achievementMapper.delete(new LambdaQueryWrapper<Achievement>().eq(Achievement::getTeamId, teamId));
        // 删除团队聊天消息（message 有 team_id 外键）
        messageMapper.delete(new LambdaQueryWrapper<Message>().eq(Message::getTeamId, teamId));
        // 删除导师邀请记录（team_teacher 有 team_id 外键）
        teamTeacherMapper.delete(new LambdaQueryWrapper<TeamTeacher>().eq(TeamTeacher::getTeamId, teamId));
        teamMemberMapper.delete(new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId));
        teamMapper.deleteById(teamId);
    }

    /**
     * 级联删除项目下的任务：先删 task_comment → task_submission → task_assignee → task
     */
    private void deleteTasksByProjectId(Long projectId) {
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().eq(Task::getProjectId, projectId));
        for (Task task : tasks) {
            taskCommentMapper.delete(new LambdaQueryWrapper<TaskComment>().eq(TaskComment::getTaskId, task.getId()));
            taskSubmissionMapper.delete(new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, task.getId()));
            taskAssigneeMapper.delete(new LambdaQueryWrapper<TaskAssignee>().eq(TaskAssignee::getTaskId, task.getId()));
        }
        taskMapper.delete(new LambdaQueryWrapper<Task>().eq(Task::getProjectId, projectId));
    }
}
