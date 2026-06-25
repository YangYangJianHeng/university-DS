package com.platform.module.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.notification.entity.Notification;
import com.platform.module.notification.mapper.NotificationMapper;
import com.platform.module.user.mapper.UserMapper;
import com.platform.module.user.entity.UserExperience;
import com.platform.module.user.entity.UserSkill;
import com.platform.module.user.mapper.UserExperienceMapper;
import com.platform.module.user.mapper.UserSkillMapper;
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
    private final NotificationMapper notificationMapper;
    private final RecruitMapper recruitMapper;
    private final TeamMapper teamMapper;
    private final UserMapper userMapper;
    private final UserExperienceMapper userExperienceMapper;
    private final UserSkillMapper userSkillMapper;

    public DashboardController(TeamMemberMapper teamMemberMapper,
                               TaskMapper taskMapper,
                               NotificationMapper notificationMapper,
                               RecruitMapper recruitMapper,
                               TeamMapper teamMapper,
                               UserMapper userMapper,
                               UserExperienceMapper userExperienceMapper,
                               UserSkillMapper userSkillMapper) {
        this.teamMemberMapper = teamMemberMapper;
        this.taskMapper = taskMapper;
        this.notificationMapper = notificationMapper;
        this.recruitMapper = recruitMapper;
        this.teamMapper = teamMapper;
        this.userMapper = userMapper;
        this.userExperienceMapper = userExperienceMapper;
        this.userSkillMapper = userSkillMapper;
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

        // Task count (assigned to me)
        long taskCount = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getAssigneeId, userId));
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

        // Recent recruits
        List<Recruit> recruits = recruitMapper.selectList(
                new LambdaQueryWrapper<Recruit>()
                        .orderByDesc(Recruit::getCreatedAt)
                        .last("LIMIT 5"));
        data.put("recentRecruits", recruits);

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

        Map<String, Long> skillCounts = skills.stream()
                .collect(Collectors.groupingBy(UserSkill::getSkillName, Collectors.counting()));

        List<Map<String, Object>> radarData = skillCounts.entrySet().stream().map(entry -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue().intValue() * 20); // scale to 0-100
            return item;
        }).collect(Collectors.toList());

        return Result.success(radarData);
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

        long teamCount = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        stats.put("totalTeams", teamCount);

        long taskCount = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getAssigneeId, userId));
        stats.put("totalTasks", taskCount);

        long completedTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getAssigneeId, userId)
                        .eq(Task::getStatus, "DONE"));
        stats.put("completedTasks", completedTasks);

        return Result.success(stats);
    }

    @GetMapping("/profile/badges")
    @Operation(summary = "Get achievement badges (mock)")
    public Result<List<Map<String, Object>>> badges() {
        List<Map<String, Object>> badgeList = new ArrayList<>();

        Map<String, Object> badge1 = new HashMap<>();
        badge1.put("name", "新手入门");
        badge1.put("description", "完成第一个项目");
        badge1.put("icon", "badge_newbie");
        badge1.put("unlocked", true);
        badgeList.add(badge1);

        Map<String, Object> badge2 = new HashMap<>();
        badge2.put("name", "团队之星");
        badge2.put("description", "参与3个以上团队");
        badge2.put("icon", "badge_team");
        badge2.put("unlocked", false);
        badgeList.add(badge2);

        Map<String, Object> badge3 = new HashMap<>();
        badge3.put("name", "竞赛达人");
        badge3.put("description", "获得省级以上竞赛奖项");
        badge3.put("icon", "badge_competition");
        badge3.put("unlocked", false);
        badgeList.add(badge3);

        return Result.success(badgeList);
    }
}
