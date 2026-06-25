package com.platform.module.project.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.common.util.UserContext;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.project.service.ProjectService;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.team.entity.Team;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserMapper userMapper;
    private final RecruitMapper recruitMapper;

    public ProjectServiceImpl(TeamMapper teamMapper, TeamMemberMapper teamMemberMapper,
                              UserMapper userMapper, RecruitMapper recruitMapper) {
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.userMapper = userMapper;
        this.recruitMapper = recruitMapper;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Project create(Long teamId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw BusinessException.badRequest("项目名称不能为空");
        }

        Long currentUserId = UserContext.getCurrentUserId();

        // 如果没有指定团队，自动创建团队（项目名称作为团队名）
        if (teamId == null) {
            Team team = new Team();
            team.setName(name.trim() + " 团队");
            team.setLeaderId(currentUserId);
            team.setStatus("ACTIVE");
            teamMapper.insert(team);
            teamId = team.getId();

            // 创建者自动加入为队长
            TeamMember leader = new TeamMember();
            leader.setTeamId(teamId);
            leader.setUserId(currentUserId);
            leader.setRole("LEADER");
            teamMemberMapper.insert(leader);
        } else {
            // 验证团队是否存在
            Team team = teamMapper.selectById(teamId);
            if (team == null) {
                throw BusinessException.notFound("所属团队不存在");
            }
            // 权限校验：只有团队成员可以创建项目
            if (currentUserId != null) {
                Long memberCount = teamMemberMapper.selectCount(
                        new LambdaQueryWrapper<TeamMember>()
                                .eq(TeamMember::getTeamId, teamId)
                                .eq(TeamMember::getUserId, currentUserId));
                if (memberCount == 0) {
                    throw BusinessException.forbidden("只有团队成员可以创建项目");
                }
            }
        }

        Project project = new Project();
        project.setTeamId(teamId);
        project.setLeaderId(currentUserId);
        project.setName(name.trim());
        project.setDescription(description);
        project.setStatus("PLANNING");
        project.setStartedAt(LocalDate.now());
        save(project);
        return project;
    }

    @Override
    public Project update(Long id, String name, String description, String status) {
        Project project = getById(id);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        // 权限校验：只有团队队长或ADMIN可以更新项目
        if (!isProjectMemberOrAdmin(project)) {
            throw BusinessException.forbidden("无权限修改此项目");
        }
        // 状态机校验
        if (status != null && !List.of("PLANNING", "IN_PROGRESS", "REVIEW", "COMPLETED", "ARCHIVED").contains(status)) {
            throw BusinessException.badRequest("无效的项目状态: " + status);
        }
        if (name != null) project.setName(name);
        if (description != null) project.setDescription(description);
        if (status != null) project.setStatus(status);
        updateById(project);
        return project;
    }

    @Override
    public void addMilestone(Long projectId, Map<String, Object> milestone) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        if (!isProjectMemberOrAdmin(project)) {
            throw BusinessException.forbidden("无权限操作此项目");
        }
        JSONArray milestones = parseMilestones(project.getDescription());
        JSONObject newMilestone = new JSONObject();
        newMilestone.put("id", System.currentTimeMillis());
        newMilestone.put("name", milestone.get("name"));
        newMilestone.put("description", milestone.get("description"));
        newMilestone.put("deadline", milestone.get("deadline"));
        newMilestone.put("status", "PENDING");
        milestones.add(newMilestone);
        project.setDescription(JSON.toJSONString(milestones));
        updateById(project);
    }

    @Override
    public void updateMilestone(Long projectId, Long milestoneId, Map<String, Object> milestone) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        if (!isProjectMemberOrAdmin(project)) {
            throw BusinessException.forbidden("无权限操作此项目");
        }
        JSONArray milestones = parseMilestones(project.getDescription());
        for (int i = 0; i < milestones.size(); i++) {
            JSONObject ms = milestones.getJSONObject(i);
            if (ms.getLong("id").equals(milestoneId)) {
                if (milestone.containsKey("name")) ms.put("name", milestone.get("name"));
                if (milestone.containsKey("description")) ms.put("description", milestone.get("description"));
                if (milestone.containsKey("deadline")) ms.put("deadline", milestone.get("deadline"));
                if (milestone.containsKey("status")) ms.put("status", milestone.get("status"));
                break;
            }
        }
        project.setDescription(JSON.toJSONString(milestones));
        updateById(project);
    }

    @Override
    public void archive(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        if (!isProjectMemberOrAdmin(project)) {
            throw BusinessException.forbidden("无权限归档此项目");
        }
        project.setStatus("ARCHIVED");
        updateById(project);
    }

    @Override
    public List<Map<String, Object>> getGanttData(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        List<Map<String, Object>> ganttData = new ArrayList<>();
        JSONArray milestones = parseMilestones(project.getDescription());

        Map<String, Object> projectTask = new HashMap<>();
        projectTask.put("id", "project-" + projectId);
        projectTask.put("name", project.getName());
        projectTask.put("start", project.getStartedAt() != null ? project.getStartedAt().toString() : LocalDate.now().toString());
        projectTask.put("end", project.getEndedAt() != null ? project.getEndedAt().toString() : LocalDate.now().plusMonths(3).toString());
        projectTask.put("progress", calculateProgress(project.getStatus()));
        ganttData.add(projectTask);

        for (int i = 0; i < milestones.size(); i++) {
            JSONObject ms = milestones.getJSONObject(i);
            Map<String, Object> task = new HashMap<>();
            task.put("id", "milestone-" + ms.getLong("id"));
            task.put("name", ms.getString("name"));
            // 使用里程碑中的deadline如果有的话
            if (ms.containsKey("deadline") && ms.get("deadline") != null) {
                String deadline = ms.getString("deadline");
                task.put("end", deadline);
                task.put("start", project.getStartedAt() != null ? project.getStartedAt().toString() : LocalDate.now().toString());
            } else {
                task.put("start", LocalDate.now().plusDays(i * 15).toString());
                task.put("end", LocalDate.now().plusDays(i * 15 + 10).toString());
            }
            task.put("progress", "COMPLETED".equals(ms.getString("status")) ? 100 : 0);
            ganttData.add(task);
        }
        return ganttData;
    }

    @Override
    public Map<String, Object> getRiskAssessment(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        // 基于实际数据计算风险评估
        Map<String, Object> risk = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        int totalScore = 0;

        // 进度风险：基于状态和里程碑完成情况
        JSONArray milestones = parseMilestones(project.getDescription());
        long completedMilestones = 0;
        for (int i = 0; i < milestones.size(); i++) {
            JSONObject ms = milestones.getJSONObject(i);
            if ("COMPLETED".equals(ms.getString("status"))) {
                completedMilestones++;
            }
        }
        int progressScore = milestones.isEmpty() ? 0 : (int) ((1.0 - (double) completedMilestones / milestones.size()) * 50);
        totalScore += progressScore;
        items.add(Map.of(
                "type", "进度风险",
                "level", progressScore > 30 ? "HIGH" : progressScore > 15 ? "MEDIUM" : "LOW",
                "description", milestones.isEmpty() ? "暂无里程碑" :
                        String.format("里程碑完成率: %d/%d", completedMilestones, milestones.size())
        ));

        // 时间风险：基于结束日期
        int timeScore = 0;
        if (project.getEndedAt() != null) {
            long daysLeft = project.getEndedAt().toEpochDay() - LocalDate.now().toEpochDay();
            timeScore = daysLeft < 7 ? 40 : daysLeft < 30 ? 20 : 5;
            totalScore += timeScore;
            items.add(Map.of(
                    "type", "时间风险",
                    "level", timeScore > 30 ? "HIGH" : timeScore > 15 ? "MEDIUM" : "LOW",
                    "description", String.format("距离截止日期还有 %d 天", daysLeft)
            ));
        }

        // 状态风险
        int statusScore = "ARCHIVED".equals(project.getStatus()) ? 0 :
                "PLANNING".equals(project.getStatus()) ? 15 : 5;
        totalScore += statusScore;
        items.add(Map.of(
                "type", "状态风险",
                "level", statusScore > 10 ? "MEDIUM" : "LOW",
                "description", "项目当前状态: " + project.getStatus()
        ));

        risk.put("overall", totalScore > 60 ? "HIGH" : totalScore > 30 ? "MEDIUM" : "LOW");
        risk.put("score", Math.min(totalScore, 100));
        risk.put("items", items);
        return risk;
    }

    @Override
    public List<Project> getMyProjects(Long userId) {
        // 查找用户作为成员的所有团队
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        List<Long> teamIds = members.stream().map(TeamMember::getTeamId).distinct().collect(Collectors.toList());

        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 查询这些团队的所有项目
        return list(new LambdaQueryWrapper<Project>()
                .in(Project::getTeamId, teamIds)
                .orderByDesc(Project::getCreatedAt));
    }

    /**
     * 检查当前用户是否为项目所属团队成员或管理员
     */
    private boolean isProjectMemberOrAdmin(Project project) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) return false;

        // ADMIN can do anything
        String role = UserContext.getCurrentUserRole();
        if ("ADMIN".equals(role)) return true;

        // Check if user is a member of the project's team
        Long count = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, project.getTeamId())
                        .eq(TeamMember::getUserId, currentUserId));
        return count > 0;
    }

    private JSONArray parseMilestones(String description) {
        try {
            return JSON.parseArray(description);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private int calculateProgress(String status) {
        if (status == null) return 0;
        return switch (status) {
            case "COMPLETED" -> 100;
            case "IN_PROGRESS" -> 50;
            case "REVIEW" -> 80;
            case "ARCHIVED" -> 100;
            default -> 0;
        };
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void addProjectMember(Long projectId, Long userId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        if (!isProjectMemberOrAdmin(project)) {
            throw BusinessException.forbidden("无权限添加成员");
        }

        // 检查是否已是项目对应团队的成员
        Long existingCount = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, project.getTeamId())
                        .eq(TeamMember::getUserId, userId));
        if (existingCount > 0) {
            throw BusinessException.conflict("该用户已是项目团队成员");
        }

        // 添加为团队普通成员
        TeamMember member = new TeamMember();
        member.setTeamId(project.getTeamId());
        member.setUserId(userId);
        member.setRole("MEMBER");
        teamMemberMapper.insert(member);
    }

    @Override
    public List<Map<String, Object>> getProjectMembers(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }

        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, project.getTeamId()));

        // 批量查询用户名
        List<Long> userIds = members.stream()
                .map(TeamMember::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userNameMap.put(user.getId(), user.getUsername());
            }
        }

        Map<Long, String> finalUserNameMap = userNameMap;
        return members.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("userId", m.getUserId());
            map.put("userName", finalUserNameMap.getOrDefault(m.getUserId(), "Unknown"));
            map.put("role", m.getRole());
            map.put("joinedAt", m.getJoinedAt());
            return map;
        }).collect(Collectors.toList());
    }
}
