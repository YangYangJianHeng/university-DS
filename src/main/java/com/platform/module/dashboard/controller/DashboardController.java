package com.platform.module.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.entity.TaskAssignee;
import com.platform.module.collaboration.mapper.TaskAssigneeMapper;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.achievement.mapper.AchievementMapper;
import com.platform.module.notification.entity.Notification;
import com.platform.module.notification.mapper.NotificationMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.user.mapper.UserMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.entity.UserExperience;
import com.platform.module.user.entity.UserSkill;
import com.platform.module.user.mapper.UserExperienceMapper;
import com.platform.module.user.mapper.UserSkillMapper;
import com.platform.module.badge.service.BadgeService;
import com.platform.module.badge.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Dashboard & Profile")
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final TeamMemberMapper teamMemberMapper;
    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final NotificationMapper notificationMapper;
    private final RecruitMapper recruitMapper;
    private final TeamMapper teamMapper;
    private final UserMapper userMapper;
    private final UserExperienceMapper userExperienceMapper;
    private final UserSkillMapper userSkillMapper;
    private final ProjectMapper projectMapper;
    private final AchievementMapper achievementMapper;
    private final BadgeService badgeService;
    private final CertificateService certificateService;

    public DashboardController(TeamMemberMapper teamMemberMapper,
                               TaskMapper taskMapper,
                               TaskAssigneeMapper taskAssigneeMapper,
                               NotificationMapper notificationMapper,
                               RecruitMapper recruitMapper,
                               TeamMapper teamMapper,
                               UserMapper userMapper,
                               UserExperienceMapper userExperienceMapper,
                               UserSkillMapper userSkillMapper,
                               ProjectMapper projectMapper,
                               AchievementMapper achievementMapper,
                               BadgeService badgeService,
                               CertificateService certificateService) {
        this.teamMemberMapper = teamMemberMapper;
        this.taskMapper = taskMapper;
        this.taskAssigneeMapper = taskAssigneeMapper;
        this.notificationMapper = notificationMapper;
        this.recruitMapper = recruitMapper;
        this.teamMapper = teamMapper;
        this.userMapper = userMapper;
        this.userExperienceMapper = userExperienceMapper;
        this.userSkillMapper = userSkillMapper;
        this.projectMapper = projectMapper;
        this.achievementMapper = achievementMapper;
        this.badgeService = badgeService;
        this.certificateService = certificateService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard aggregated data")
    public Result<Map<String, Object>> dashboard() {
        Long userId = UserContext.getCurrentUserId();

        Map<String, Object> data = new HashMap<>();

        // Team count (user's teams)
        long teamCount = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        data.put("teamCount", teamCount);

        // Task count (assigned to me via TaskAssignee table)
        long taskCount = taskAssigneeMapper.selectCount(
                new LambdaQueryWrapper<TaskAssignee>().eq(TaskAssignee::getUserId, userId));
        data.put("taskCount", taskCount);

        // Unread notification count
        long unreadCount = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
        data.put("unreadNotificationCount", unreadCount);

        // Active recruit count
        long activeRecruitCount = recruitMapper.selectCount(
                new LambdaQueryWrapper<Recruit>()
                        .eq(Recruit::getStatus, "OPEN"));
        data.put("activeRecruitCount", activeRecruitCount);

        // Recent recruits (limit 5, only OPEN + APPROVED — hide rejected/pending/closed)
        List<Recruit> recruits = recruitMapper.selectPage(
                new Page<>(1, 5),
                new LambdaQueryWrapper<Recruit>()
                        .eq(Recruit::getStatus, "OPEN")
                        .eq(Recruit::getAuditStatus, "APPROVED")
                        .orderByDesc(Recruit::getCreatedAt))
                .getRecords();
        // Batch lookup publisher names
        Set<Long> publisherIds = recruits.stream().map(Recruit::getPublisherId).collect(Collectors.toSet());
        Map<Long, String> usernameMap = Collections.emptyMap();
        if (!publisherIds.isEmpty()) {
            List<User> publishers = userMapper.selectBatchIds(publisherIds);
            usernameMap = publishers.stream().collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        }
        Map<Long, String> finalUsernameMap = usernameMap;
        List<Map<String, Object>> recentRecruits = recruits.stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("title", r.getTitle());
            item.put("status", r.getStatus());
            item.put("createdAt", r.getCreatedAt());
            item.put("publisherId", r.getPublisherId());
            item.put("publisherName", finalUsernameMap.getOrDefault(r.getPublisherId(), "未知用户"));
            return item;
        }).collect(Collectors.toList());
        data.put("recentRecruits", recentRecruits);

        return Result.success(data);
    }

    @GetMapping("/profile/experience")
    @Operation(summary = "Get user experience")
    public Result<List<UserExperience>> experience() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(userExperienceMapper.selectList(
                new LambdaQueryWrapper<UserExperience>().eq(UserExperience::getUserId, userId)));
    }

    @GetMapping("/profile/skill-radar")
    @Operation(summary = "Get skill radar data")
    public Result<List<Map<String, Object>>> skillRadar() {
        Long userId = UserContext.getCurrentUserId();
        List<UserSkill> skills = userSkillMapper.selectList(
                new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId));

        List<Map<String, Object>> radarData = skills.stream().map(skill -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", skill.getSkillName());
            item.put("value", skillLevelToValue(skill.getLevel()));
            item.put("level", skill.getLevel());
            return item;
        }).collect(Collectors.toList());

        return Result.success(radarData);
    }

    /** 将技能等级字符串映射为 0-100 的数值 */
    private int skillLevelToValue(String level) {
        if (level == null) return 10;
        return switch (level) {
            case "入门" -> 20;
            case "熟练" -> 40;
            case "掌握" -> 60;
            case "精通" -> 80;
            case "大师" -> 100;
            default -> 20;
        };
    }

    @PostMapping("/profile/resume/export")
    @Operation(summary = "Export resume (mock)")
    public Result<Map<String, Object>> exportResume() {
        Map<String, Object> result = new HashMap<>();
        result.put("url", "/api/profile/resume/download/mock-resume.pdf");
        result.put("format", "PDF");
        result.put("generatedAt", java.time.LocalDateTime.now().toString());
        return Result.success(result);
    }

    @GetMapping("/profile/statistics")
    @Operation(summary = "Get participation statistics")
    public Result<Map<String, Object>> statistics() {
        Long userId = UserContext.getCurrentUserId();
        Map<String, Object> stats = new HashMap<>();

        // 用户所在的团队
        List<Long> userTeamIds = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId))
                .stream().map(TeamMember::getTeamId).distinct().collect(Collectors.toList());

        long teamCount = userTeamIds.size();
        stats.put("totalTeams", teamCount);

        // 用户所在团队的项目数
        long projectCount = userTeamIds.isEmpty() ? 0 : projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().in(Project::getTeamId, userTeamIds));
        stats.put("totalProjects", projectCount);

        // 用户参与的任务数 + 已完成任务数（一次查询，避免重复DB调用）
        List<TaskAssignee> userAssignments = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>().eq(TaskAssignee::getUserId, userId));
        long taskCount = userAssignments.size();
        stats.put("totalTasks", taskCount);

        List<Long> userTaskIds = userAssignments.stream()
                .map(TaskAssignee::getTaskId).collect(Collectors.toList());
        long completedTasks = userTaskIds.isEmpty() ? 0 : taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .in(Task::getId, userTaskIds)
                        .eq(Task::getStatus, "DONE"));
        stats.put("completedTasks", completedTasks);

        // 用户提交的成果数
        long achievementCount = achievementMapper.selectCount(
                new LambdaQueryWrapper<Achievement>().eq(Achievement::getSubmitterId, userId));
        stats.put("totalAchievements", achievementCount);

        // 图表数据：labels + values
        List<String> categories = List.of("参与团队", "参与项目", "分配任务", "完成任务", "成果提交");
        List<Long> values = List.of(teamCount, projectCount, taskCount, completedTasks, achievementCount);
        stats.put("categories", categories);
        stats.put("values", values);

        return Result.success(stats);
    }

    @GetMapping("/profile/badges")
    @Operation(summary = "Get achievement badges (database-driven)")
    public Result<List<Map<String, Object>>> badges() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(badgeService.getUserBadges(userId));
    }

    @GetMapping("/profile/certificates")
    @Operation(summary = "Get user certificates")
    public Result<List<com.platform.module.badge.entity.Certificate>> certificates() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(certificateService.getUserCertificates(userId));
    }

    @GetMapping("/profile/certificates/check-new")
    @Operation(summary = "Check and auto-generate certificates for newly unlocked badges")
    public Result<Map<String, Object>> checkNewCertificates() {
        Long userId = UserContext.getCurrentUserId();
        List<Map<String, Object>> badges = badgeService.getUserBadges(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("newCertificates", new java.util.ArrayList<>());
        result.put("totalUnlocked", badges.stream().filter(b -> Boolean.TRUE.equals(b.get("unlocked"))).count());

        // 一次性获取所有现有证书，避免N+1查询
        List<com.platform.module.badge.entity.Certificate> existingCerts =
                certificateService.getUserCertificates(userId);
        Set<Long> certBadgeIds = existingCerts.stream()
                .map(com.platform.module.badge.entity.Certificate::getBadgeId)
                .collect(Collectors.toSet());

        // 自动为每个已解锁但未生成证书的徽章生成证书
        List<Map<String, Object>> newCerts = new java.util.ArrayList<>();
        for (Map<String, Object> badge : badges) {
            if (Boolean.TRUE.equals(badge.get("unlocked"))) {
                Long badgeId = Long.valueOf(badge.get("id").toString());
                if (!certBadgeIds.contains(badgeId)) {
                    var newCert = certificateService.generateCertificate(userId, badgeId);
                    if (newCert != null) {
                        Map<String, Object> certMap = certificateService.getCertificateViewData(newCert.getId());
                        newCerts.add(certMap);
                    }
                }
            }
        }
        result.put("newCertificates", newCerts);
        return Result.success(result);
    }
}
