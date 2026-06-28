package com.platform.module.badge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.achievement.mapper.AchievementMapper;
import com.platform.module.badge.entity.Badge;
import com.platform.module.badge.mapper.BadgeMapper;
import com.platform.module.badge.service.BadgeService;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.entity.TaskAssignee;
import com.platform.module.collaboration.mapper.TaskAssigneeMapper;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.user.entity.UserExperience;
import com.platform.module.user.entity.UserSkill;
import com.platform.module.user.mapper.UserExperienceMapper;
import com.platform.module.user.mapper.UserSkillMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BadgeServiceImpl extends ServiceImpl<BadgeMapper, Badge> implements BadgeService {

    private final TeamMemberMapper teamMemberMapper;
    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final UserSkillMapper userSkillMapper;
    private final AchievementMapper achievementMapper;
    private final ProjectMapper projectMapper;
    private final RecruitMapper recruitMapper;
    private final UserExperienceMapper userExperienceMapper;

    public BadgeServiceImpl(TeamMemberMapper teamMemberMapper,
                            TaskMapper taskMapper,
                            TaskAssigneeMapper taskAssigneeMapper,
                            UserSkillMapper userSkillMapper,
                            AchievementMapper achievementMapper,
                            ProjectMapper projectMapper,
                            RecruitMapper recruitMapper,
                            UserExperienceMapper userExperienceMapper) {
        this.teamMemberMapper = teamMemberMapper;
        this.taskMapper = taskMapper;
        this.taskAssigneeMapper = taskAssigneeMapper;
        this.userSkillMapper = userSkillMapper;
        this.achievementMapper = achievementMapper;
        this.projectMapper = projectMapper;
        this.recruitMapper = recruitMapper;
        this.userExperienceMapper = userExperienceMapper;
    }

    @Override
    public List<Map<String, Object>> getUserBadges(Long userId) {
        List<Badge> allBadges = this.list(new LambdaQueryWrapper<Badge>().orderByAsc(Badge::getSortOrder));

        // 预查用户数据
        UserStats stats = computeUserStats(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Badge badge : allBadges) {
            boolean unlocked = evaluateCondition(badge, stats);
            Map<String, Object> map = new HashMap<>();
            map.put("id", badge.getId());
            map.put("name", badge.getName());
            map.put("description", badge.getDescription());
            map.put("icon", badge.getIcon());
            map.put("unlocked", unlocked);
            result.add(map);
        }
        return result;
    }

    @Override
    @CacheEvict(value = "badge", allEntries = true)
    public Badge createBadge(Badge badge) {
        this.save(badge);
        return badge;
    }

    @Override
    @CacheEvict(value = "badge", allEntries = true)
    public Badge updateBadge(Long id, Badge badge) {
        badge.setId(id);
        this.updateById(badge);
        return this.getById(id);
    }

    // ===== 条件评估 =====

    private boolean evaluateCondition(Badge badge, UserStats stats) {
        String type = badge.getConditionType();
        int val = badge.getConditionValue() != null ? badge.getConditionValue() : 0;

        return switch (type) {
            case "COMPLETED_TASKS" -> stats.completedTasks > val;
            case "TEAM_COUNT"       -> stats.teamCount >= val;
            case "SKILL_COUNT"      -> stats.skillCount >= val;
            case "ACHIEVEMENT_COUNT" -> stats.achievementCount > val;
            case "COMPETITION_COUNT" -> stats.competitionCount > val;
            case "MASTER_SKILL"     -> stats.hasMasterSkill;
            case "HAS_HONOR"        -> stats.hasHonor;
            case "RECRUIT_COUNT"    -> stats.recruitCount > val;
            case "PROJECT_COUNT"    -> stats.projectCount >= val;
            case "EXPERIENCE_COUNT" -> stats.experienceCount >= val;
            case "ALL_ROUNDER"      -> stats.teamCount >= 2 && stats.completedTasks >= 3 && stats.skillCount >= 3 && stats.achievementCount > 0;
            case "NEWBIE"           -> stats.completedTasks > 0 || stats.teamCount > 0;
            default -> false;
        };
    }

    private UserStats computeUserStats(Long userId) {
        UserStats s = new UserStats();

        List<Long> userTeamIds = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId))
                .stream().map(TeamMember::getTeamId).distinct().collect(Collectors.toList());
        s.teamCount = userTeamIds.size();

        s.projectCount = userTeamIds.isEmpty() ? 0 : projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().in(Project::getTeamId, userTeamIds));

        List<Long> userTaskIds = taskAssigneeMapper.selectList(
                        new LambdaQueryWrapper<TaskAssignee>().eq(TaskAssignee::getUserId, userId))
                .stream().map(TaskAssignee::getTaskId).collect(Collectors.toList());
        s.completedTasks = userTaskIds.isEmpty() ? 0 : taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().in(Task::getId, userTaskIds).eq(Task::getStatus, "DONE"));

        s.skillCount = userSkillMapper.selectCount(
                new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId));
        s.hasMasterSkill = userSkillMapper.selectCount(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getLevel, "大师")) > 0;

        s.achievementCount = achievementMapper.selectCount(
                new LambdaQueryWrapper<Achievement>().eq(Achievement::getSubmitterId, userId));
        s.competitionCount = achievementMapper.selectCount(
                new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getSubmitterId, userId)
                        .eq(Achievement::getType, "competition"));
        s.hasHonor = achievementMapper.selectCount(
                new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getSubmitterId, userId)
                        .isNotNull(Achievement::getAwardLevel)) > 0;

        s.recruitCount = recruitMapper.selectCount(
                new LambdaQueryWrapper<Recruit>().eq(Recruit::getPublisherId, userId));

        s.experienceCount = userExperienceMapper.selectCount(
                new LambdaQueryWrapper<UserExperience>().eq(UserExperience::getUserId, userId));

        return s;
    }

    private static class UserStats {
        long teamCount;
        long projectCount;
        long completedTasks;
        long skillCount;
        boolean hasMasterSkill;
        long achievementCount;
        long competitionCount;
        boolean hasHonor;
        long recruitCount;
        long experienceCount;
    }
}
