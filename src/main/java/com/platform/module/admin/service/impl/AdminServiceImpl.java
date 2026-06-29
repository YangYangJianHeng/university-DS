package com.platform.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.module.admin.entity.Announcement;
import com.platform.module.admin.entity.AuditLog;
import com.platform.module.admin.mapper.AnnouncementMapper;
import com.platform.module.admin.mapper.AuditLogMapper;
import com.platform.module.admin.service.AdminService;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.achievement.mapper.AchievementMapper;
import com.platform.module.badge.entity.Badge;
import com.platform.module.badge.entity.Certificate;
import com.platform.module.badge.mapper.BadgeMapper;
import com.platform.module.badge.service.CertificateService;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.teacher.entity.Teacher;
import com.platform.module.teacher.mapper.TeacherMapper;
import com.platform.module.competition.entity.Competition;
import com.platform.module.team.entity.Team;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.mapper.UserMapper;
import org.springframework.transaction.annotation.Transactional;
import com.platform.module.competition.mapper.CompetitionMapper;
import org.springframework.stereotype.Service;

import com.platform.module.user.vo.UserVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final AuditLogMapper auditLogMapper;
    private final RecruitMapper recruitMapper;
    private final TeamMapper teamMapper;
    private final ProjectMapper projectMapper;
    private final CompetitionMapper competitionMapper;
    private final TaskMapper taskMapper;
    private final TeacherMapper teacherMapper;
    private final AnnouncementMapper announcementMapper;
    private final AchievementMapper achievementMapper;
    private final BadgeMapper badgeMapper;
    private final CertificateService certificateService;

    public AdminServiceImpl(UserMapper userMapper, AuditLogMapper auditLogMapper,
                            RecruitMapper recruitMapper, TeamMapper teamMapper,
                            ProjectMapper projectMapper, CompetitionMapper competitionMapper,
                            TaskMapper taskMapper, TeacherMapper teacherMapper,
                            AnnouncementMapper announcementMapper,
                            AchievementMapper achievementMapper,
                            BadgeMapper badgeMapper,
                            CertificateService certificateService) {
        this.userMapper = userMapper;
        this.auditLogMapper = auditLogMapper;
        this.recruitMapper = recruitMapper;
        this.teamMapper = teamMapper;
        this.projectMapper = projectMapper;
        this.competitionMapper = competitionMapper;
        this.taskMapper = taskMapper;
        this.teacherMapper = teacherMapper;
        this.announcementMapper = announcementMapper;
        this.achievementMapper = achievementMapper;
        this.badgeMapper = badgeMapper;
        this.certificateService = certificateService;
    }

    @Override
    public IPage<UserVO> listUsers(int page, int size) {
        IPage<User> userPage = userMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));

        // 转换为 UserVO，不暴露密码字段
        IPage<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream().map(this::toUserVO).collect(Collectors.toList()));
        return voPage;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setStudentId(user.getStudentId());
        vo.setUsername(user.getUsername());
        vo.setMajor(user.getMajor());
        vo.setGrade(user.getGrade());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    @Override
    @CacheEvict(value = {"admin", "publicStats", "competition", "recruit"}, allEntries = true)
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("User not found");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public IPage<AuditLog> listAuditLogs(String status, int page, int size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AuditLog::getAfterStatus, status);
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return auditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void processAudit(Long auditId, String action, Long operatorId) {
        AuditLog auditLog = auditLogMapper.selectById(auditId);
        if (auditLog == null) {
            throw BusinessException.notFound("Audit log not found");
        }
        if ("APPROVE".equals(action)) {
            auditLog.setAfterStatus("APPROVED");
        } else if ("REJECT".equals(action)) {
            auditLog.setAfterStatus("REJECTED");
        } else {
            throw BusinessException.badRequest("Invalid action: " + action);
        }
        auditLogMapper.updateById(auditLog);
    }

    @Override
    @Cacheable(value = "admin", key = "'dashboard'")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // User stats
        long totalUsers = userMapper.selectCount(null);
        long activeUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));

        // Team / Project / Competition totals
        long totalTeams = teamMapper.selectCount(null);
        long totalProjects = projectMapper.selectCount(null);
        long totalCompetitions = competitionMapper.selectCount(null);

        // Audit stats
        long pendingAudits = auditLogMapper.selectCount(
                new LambdaQueryWrapper<AuditLog>().eq(AuditLog::getAfterStatus, "PENDING"));

        long pendingRecruits = recruitMapper.selectCount(
                new LambdaQueryWrapper<Recruit>().eq(Recruit::getAuditStatus, "PENDING"));

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalTeams", totalTeams);
        stats.put("totalProjects", totalProjects);
        stats.put("totalCompetitions", totalCompetitions);
        stats.put("pendingAudits", pendingAudits);
        stats.put("pendingRecruits", pendingRecruits);
        stats.put("timestamp", LocalDateTime.now().toString());

        return stats;
    }

    @Override
    @Cacheable(value = "admin", key = "'weeklyReport'")
    public Map<String, Object> getWeeklyReport() {
        Map<String, Object> report = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);

        report.put("period", "本周");

        // 真实统计数据
        long newUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, weekAgo));
        report.put("newUsers", newUsers);

        long totalProjects = projectMapper.selectCount(null);
        report.put("totalProjects", totalProjects);

        long activeProjects = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().eq(Project::getStatus, "IN_PROGRESS"));
        report.put("activeProjects", activeProjects);

        long completedTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getStatus, "DONE"));
        report.put("completedTasks", completedTasks);

        long totalTasks = taskMapper.selectCount(null);
        report.put("totalTasks", totalTasks);

        long totalTeams = teamMapper.selectCount(null);
        report.put("totalTeams", totalTeams);

        long totalUsers = userMapper.selectCount(null);
        report.put("totalUsers", totalUsers);

        report.put("generatedAt", now.toString());
        return report;
    }

    @Override
    @CacheEvict(value = "admin", key = "'announcements'")
    public void createAnnouncement(String title, String content, String type) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setType(type != null ? type : "SYSTEM");
        announcementMapper.insert(announcement);
    }

    @Override
    @Cacheable(value = "admin", key = "'announcements'")
    public List<Map<String, Object>> listAnnouncements() {
        List<Announcement> list = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getCreatedAt));
        return list.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("title", a.getTitle());
            map.put("content", a.getContent());
            map.put("type", a.getType());
            map.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<Recruit> listPendingRecruits(int page, int size) {
        return recruitMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Recruit>()
                        .eq(Recruit::getAuditStatus, "PENDING")
                        .orderByDesc(Recruit::getCreatedAt));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    @CacheEvict(value = {"recruit", "admin", "publicStats"}, allEntries = true)
    public void auditRecruit(Long recruitId, String action, Long operatorId) {
        Recruit recruit = recruitMapper.selectById(recruitId);
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }
        if ("APPROVE".equals(action)) {
            recruit.setAuditStatus("APPROVED");
        } else if ("REJECT".equals(action)) {
            recruit.setAuditStatus("REJECTED");
            // 注意: 不修改 recruit.status，该字段控制招募展示状态（OPEN/CLOSED）
            // auditStatus 控制审核状态（PENDING/APPROVED/REJECTED）
        } else {
            throw BusinessException.badRequest("无效操作: " + action);
        }
        recruitMapper.updateById(recruit);

        // Write audit log
        AuditLog log = new AuditLog();
        log.setTargetType("RECRUIT");
        log.setTargetId(recruitId);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setBeforeStatus("PENDING");
        log.setAfterStatus("APPROVE".equals(action) ? "APPROVED" : "REJECTED");
        auditLogMapper.insert(log);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateUserRole(Long userId, String role) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (!List.of("USER", "ADMIN", "TEACHER", "REVIEWER").contains(role)) {
            throw BusinessException.badRequest("无效角色: " + role);
        }
        String oldRole = user.getRole();
        user.setRole(role);
        userMapper.updateById(user);

        // 设置为导师时，自动创建 Teacher 记录；取消导师时，删除 Teacher 记录
        if ("TEACHER".equals(role) && !"TEACHER".equals(oldRole)) {
            Teacher existingTeacher = teacherMapper.selectOne(
                    new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
            if (existingTeacher == null) {
                Teacher teacher = new Teacher();
                teacher.setUserId(userId);
                teacher.setDepartment("待完善");
                teacher.setTitle("教师");
                teacher.setMaxTeams(5);
                teacher.setCurrentTeams(0);
                teacher.setIsVerified(1);
                teacher.setRating(java.math.BigDecimal.ZERO);
                teacherMapper.insert(teacher);
            }
        } else if (!"TEACHER".equals(role) && "TEACHER".equals(oldRole)) {
            // 取消导师角色时，删除对应的 Teacher 记录
            teacherMapper.delete(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        }
    }

    // ---- Project Management ----

    @Override
    public IPage<Project> listAllProjects(int page, int size) {
        return projectMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Project>().orderByDesc(Project::getCreatedAt));
    }

    @Override
    @CacheEvict(value = {"admin", "publicStats"}, allEntries = true)
    public Project updateProject(Long id, String name, String description, String status) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        if (name != null && !name.trim().isEmpty()) project.setName(name.trim());
        if (description != null) project.setDescription(description);
        if (status != null) project.setStatus(status);
        projectMapper.updateById(project);
        return project;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    @CacheEvict(value = {"admin", "publicStats"}, allEntries = true)
    public void deleteProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        // 删除项目下的所有任务
        taskMapper.delete(new LambdaQueryWrapper<Task>().eq(Task::getProjectId, id));
        projectMapper.deleteById(id);
    }

    // ---- Task Management ----

    @Override
    public IPage<Task> listAllTasks(int page, int size) {
        return taskMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Task>().orderByDesc(Task::getCreatedAt));
    }

    @Override
    public Task updateTask(Long id, String title, String description, String priority, String status) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        if (title != null && !title.trim().isEmpty()) task.setTitle(title.trim());
        if (description != null) task.setDescription(description);
        // 任务已改为多人指派模式（TaskAssignee表），不再有单一assigneeId字段
        if (priority != null) task.setPriority(priority);
        if (status != null) task.setStatus(status);
        taskMapper.updateById(task);
        return task;
    }

    @Override
    public void deleteTask(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        taskMapper.deleteById(id);
    }

    // ---- Team Management ----

    @Override
    public IPage<Team> listAllTeams(int page, int size) {
        return teamMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Team>().orderByDesc(Team::getCreatedAt));
    }

    @Override
    public Team updateTeam(Long id, String name, String status) {
        Team team = teamMapper.selectById(id);
        if (team == null) {
            throw BusinessException.notFound("团队不存在");
        }
        if (name != null && !name.trim().isEmpty()) team.setName(name.trim());
        if (status != null) team.setStatus(status);
        teamMapper.updateById(team);
        return team;
    }

    @Override
    @CacheEvict(value = {"admin", "publicStats"}, allEntries = true)
    public void deleteTeam(Long id) {
        Team team = teamMapper.selectById(id);
        if (team == null) {
            throw BusinessException.notFound("团队不存在");
        }
        // 删除团队下的所有项目
        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getTeamId, id));
        for (Project p : projects) {
            taskMapper.delete(new LambdaQueryWrapper<Task>().eq(Task::getProjectId, p.getId()));
        }
        projectMapper.delete(new LambdaQueryWrapper<Project>().eq(Project::getTeamId, id));
        teamMapper.deleteById(id);
    }

    // ---- Teacher Management ----

    @Override
    public IPage<Teacher> listAllTeachers(int page, int size) {
        return teacherMapper.selectPage(new Page<>(page, size), null);
    }

    @Override
    public Teacher updateTeacher(Long id, String department, String title, String researchFields, Integer isVerified) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw BusinessException.notFound("导师不存在");
        }
        if (department != null) teacher.setDepartment(department);
        if (title != null) teacher.setTitle(title);
        if (researchFields != null) teacher.setResearchFields(researchFields);
        if (isVerified != null) teacher.setIsVerified(isVerified);
        teacherMapper.updateById(teacher);
        return teacher;
    }

    @Override
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw BusinessException.notFound("导师不存在");
        }
        teacherMapper.deleteById(id);
    }

    // ---- Competition Management ----

    @Override
    public IPage<Competition> listAllCompetitions(int page, int size) {
        return competitionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Competition>().orderByDesc(Competition::getRegisterStart));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"competition", "admin", "publicStats"}, allEntries = true)
    public Competition createCompetition(Competition competition) {
        competition.setViewCount(0);
        competition.setCollectCount(0);
        competitionMapper.insert(competition);
        return competition;
    }

    @Override
    @CacheEvict(value = {"competition", "admin", "publicStats"}, allEntries = true)
    public Competition updateCompetition(Long id, Competition competition) {
        Competition existing = competitionMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("竞赛不存在");
        }
        if (competition.getName() != null) existing.setName(competition.getName());
        if (competition.getLevel() != null) existing.setLevel(competition.getLevel());
        if (competition.getCategory() != null) existing.setCategory(competition.getCategory());
        if (competition.getOrganizer() != null) existing.setOrganizer(competition.getOrganizer());
        if (competition.getDescription() != null) existing.setDescription(competition.getDescription());
        if (competition.getStatus() != null) existing.setStatus(competition.getStatus());
        if (competition.getRegisterStart() != null) existing.setRegisterStart(competition.getRegisterStart());
        if (competition.getRegisterEnd() != null) existing.setRegisterEnd(competition.getRegisterEnd());
        if (competition.getEventStart() != null) existing.setEventStart(competition.getEventStart());
        if (competition.getEventEnd() != null) existing.setEventEnd(competition.getEventEnd());
        if (competition.getOfficialUrl() != null) existing.setOfficialUrl(competition.getOfficialUrl());
        competitionMapper.updateById(existing);
        return existing;
    }

    @Override
    @CacheEvict(value = {"competition", "admin", "publicStats"}, allEntries = true)
    public void deleteCompetition(Long id) {
        Competition competition = competitionMapper.selectById(id);
        if (competition == null) {
            throw BusinessException.notFound("竞赛不存在");
        }
        competitionMapper.deleteById(id);
    }

    // ---- Achievement Management ----

    @Override
    public IPage<Achievement> listAllAchievements(int page, int size) {
        return achievementMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Achievement>().orderByDesc(Achievement::getCreatedAt));
    }

    @Override
    public Achievement updateAchievement(Long id, Achievement achievement) {
        Achievement existing = achievementMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("成果不存在");
        }
        if (achievement.getTitle() != null) existing.setTitle(achievement.getTitle());
        if (achievement.getType() != null) existing.setType(achievement.getType());
        if (achievement.getDescription() != null) existing.setDescription(achievement.getDescription());
        if (achievement.getAwardLevel() != null) existing.setAwardLevel(achievement.getAwardLevel());
        if (achievement.getFileUrls() != null) existing.setFileUrls(achievement.getFileUrls());
        if (achievement.getIsPublic() != null) existing.setIsPublic(achievement.getIsPublic());
        if (achievement.getIsFeatured() != null) existing.setIsFeatured(achievement.getIsFeatured());
        achievementMapper.updateById(existing);
        return existing;
    }

    @Override
    public void deleteAchievement(Long id) {
        Achievement achievement = achievementMapper.selectById(id);
        if (achievement == null) {
            throw BusinessException.notFound("成果不存在");
        }
        achievementMapper.deleteById(id);
    }

    // ---- Badge Management ----

    @Override
    public IPage<Badge> listAllBadges(int page, int size) {
        return badgeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Badge>().orderByAsc(Badge::getSortOrder));
    }

    @Override
    public Badge createBadge(Badge badge) {
        if (badge.getSortOrder() == null) badge.setSortOrder(999);
        badgeMapper.insert(badge);
        return badge;
    }

    @Override
    public Badge updateBadge(Long id, Badge badge) {
        Badge existing = badgeMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("徽章不存在");
        }
        if (badge.getName() != null) existing.setName(badge.getName());
        if (badge.getDescription() != null) existing.setDescription(badge.getDescription());
        if (badge.getIcon() != null) existing.setIcon(badge.getIcon());
        if (badge.getConditionType() != null) existing.setConditionType(badge.getConditionType());
        if (badge.getConditionValue() != null) existing.setConditionValue(badge.getConditionValue());
        if (badge.getCategory() != null) existing.setCategory(badge.getCategory());
        if (badge.getSortOrder() != null) existing.setSortOrder(badge.getSortOrder());
        badgeMapper.updateById(existing);
        return existing;
    }

    @Override
    public void deleteBadge(Long id) {
        Badge badge = badgeMapper.selectById(id);
        if (badge == null) {
            throw BusinessException.notFound("徽章不存在");
        }
        badgeMapper.deleteById(id);
    }

    // ---- Certificate Management ----

    @Override
    public List<Certificate> syncCertificates(Long badgeId) {
        return certificateService.syncCertificates(badgeId);
    }

    @Override
    public List<Certificate> listAllCertificates(int page, int size) {
        return certificateService.listAllCertificates(page, size);
    }

    @Override
    public long countAllCertificates() {
        return certificateService.countAllCertificates();
    }

    @Override
    public void deleteCertificate(Long id) {
        certificateService.deleteCertificate(id);
    }

    @Override
    public List<Certificate> regenerateAllCertificates() {
        return certificateService.regenerateAllCertificates();
    }
}
