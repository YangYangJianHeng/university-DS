package com.platform.module.project.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.common.util.UserContext;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.entity.TaskAssignee;
import com.platform.module.collaboration.entity.TaskComment;
import com.platform.module.collaboration.entity.TaskSubmission;
import com.platform.module.collaboration.mapper.TaskAssigneeMapper;
import com.platform.module.collaboration.mapper.TaskCommentMapper;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.collaboration.mapper.TaskSubmissionMapper;
import com.platform.module.file.entity.FileEntity;
import com.platform.module.file.mapper.FileMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.project.service.ProjectService;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.team.entity.Team;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.entity.TeamTeacher;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.team.mapper.TeamTeacherMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.entity.UserSkill;
import com.platform.module.user.mapper.UserMapper;
import com.platform.module.user.mapper.UserSkillMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamTeacherMapper teamTeacherMapper;
    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final TaskCommentMapper taskCommentMapper;
    private final FileMapper fileMapper;
    private final UserMapper userMapper;
    private final UserSkillMapper userSkillMapper;
    private final RecruitMapper recruitMapper;

    public ProjectServiceImpl(TeamMapper teamMapper, TeamMemberMapper teamMemberMapper,
                              TeamTeacherMapper teamTeacherMapper,
                              TaskMapper taskMapper, TaskAssigneeMapper taskAssigneeMapper,
                              TaskSubmissionMapper taskSubmissionMapper, TaskCommentMapper taskCommentMapper,
                              FileMapper fileMapper,
                              UserMapper userMapper, UserSkillMapper userSkillMapper,
                              RecruitMapper recruitMapper) {
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.teamTeacherMapper = teamTeacherMapper;
        this.taskMapper = taskMapper;
        this.taskAssigneeMapper = taskAssigneeMapper;
        this.taskSubmissionMapper = taskSubmissionMapper;
        this.taskCommentMapper = taskCommentMapper;
        this.fileMapper = fileMapper;
        this.userMapper = userMapper;
        this.userSkillMapper = userSkillMapper;
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
        // 已关闭的项目不允许任何编辑
        if ("CLOSED".equals(project.getStatus())) {
            throw BusinessException.badRequest("项目已关闭，无法编辑。请先重新开启项目");
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
        if ("CLOSED".equals(project.getStatus())) {
            throw BusinessException.badRequest("项目已关闭，无法添加里程碑");
        }
        if (!isProjectMemberOrAdmin(project)) {
            throw BusinessException.forbidden("无权限操作此项目");
        }
        JSONArray milestones = parseMilestones(project.getMilestones());
        JSONObject newMilestone = new JSONObject();
        newMilestone.put("id", System.currentTimeMillis());
        newMilestone.put("name", milestone.get("name"));
        newMilestone.put("description", milestone.get("description"));
        newMilestone.put("deadline", milestone.get("deadline"));
        newMilestone.put("status", "PENDING");
        milestones.add(newMilestone);
        project.setMilestones(JSON.toJSONString(milestones));
        updateById(project);
    }

    @Override
    public void updateMilestone(Long projectId, Long milestoneId, Map<String, Object> milestone) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        if ("CLOSED".equals(project.getStatus())) {
            throw BusinessException.badRequest("项目已关闭，无法更新里程碑");
        }
        if (!isProjectMemberOrAdmin(project)) {
            throw BusinessException.forbidden("无权限操作此项目");
        }
        JSONArray milestones = parseMilestones(project.getMilestones());
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
        project.setMilestones(JSON.toJSONString(milestones));
        updateById(project);
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = {"admin", "publicStats"}, allEntries = true)
    public void archive(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        if ("CLOSED".equals(project.getStatus())) {
            throw BusinessException.badRequest("项目已关闭，无法归档");
        }
        if (!isProjectMemberOrAdmin(project)) {
            throw BusinessException.forbidden("无权限归档此项目");
        }
        project.setStatus("ARCHIVED");
        updateById(project);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    @org.springframework.cache.annotation.CacheEvict(value = {"admin", "publicStats"}, allEntries = true)
    public void closeProject(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        if ("CLOSED".equals(project.getStatus())) {
            throw BusinessException.badRequest("项目已经处于关闭状态");
        }

        // 仅项目队长或管理员可以关闭项目
        Long currentUserId = UserContext.getCurrentUserId();
        String role = UserContext.getCurrentUserRole();
        if (!"ADMIN".equals(role) && !project.getLeaderId().equals(currentUserId)) {
            throw BusinessException.forbidden("仅项目队长可以关闭项目");
        }

        // 1. 关闭项目
        project.setStatus("CLOSED");
        updateById(project);

        // 2. 同步关闭关联团队
        Team team = teamMapper.selectById(project.getTeamId());
        if (team != null && !"CLOSED".equals(team.getStatus())) {
            team.setStatus("CLOSED");
            teamMapper.updateById(team);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    @org.springframework.cache.annotation.CacheEvict(value = {"admin", "publicStats"}, allEntries = true)
    public void reopenProject(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        if (!"CLOSED".equals(project.getStatus())) {
            throw BusinessException.badRequest("只有已关闭的项目可以重新开启");
        }

        // 仅项目队长或管理员可以重新开启项目
        Long currentUserId = UserContext.getCurrentUserId();
        String role = UserContext.getCurrentUserRole();
        if (!"ADMIN".equals(role) && !project.getLeaderId().equals(currentUserId)) {
            throw BusinessException.forbidden("仅项目队长可以重新开启项目");
        }

        // 1. 开启项目
        project.setStatus("IN_PROGRESS");
        updateById(project);

        // 2. 同步开启关联团队
        Team team = teamMapper.selectById(project.getTeamId());
        if (team != null && "CLOSED".equals(team.getStatus())) {
            team.setStatus("ACTIVE");
            teamMapper.updateById(team);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }

        // 权限校验：只有项目创建者或ADMIN可以删除
        Long currentUserId = UserContext.getCurrentUserId();
        String role = UserContext.getCurrentUserRole();
        if (!"ADMIN".equals(role) && !project.getLeaderId().equals(currentUserId)) {
            throw BusinessException.forbidden("只有项目创建者或管理员可以删除项目");
        }

        Long teamId = project.getTeamId();

        // 1. 级联删除任务相关数据（按项目ID）
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().eq(Task::getProjectId, projectId));
        List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());
        if (!taskIds.isEmpty()) {
            // 删除任务执行人关联
            taskAssigneeMapper.delete(
                    new LambdaQueryWrapper<TaskAssignee>().in(TaskAssignee::getTaskId, taskIds));
            // 删除任务提交记录
            taskSubmissionMapper.delete(
                    new LambdaQueryWrapper<TaskSubmission>().in(TaskSubmission::getTaskId, taskIds));
            // 删除任务评论
            taskCommentMapper.delete(
                    new LambdaQueryWrapper<TaskComment>().in(TaskComment::getTaskId, taskIds));
            // 删除任务
            taskMapper.delete(
                    new LambdaQueryWrapper<Task>().eq(Task::getProjectId, projectId));
        }

        // 2. 删除项目关联文件
        fileMapper.delete(
                new LambdaQueryWrapper<FileEntity>().eq(FileEntity::getProjectId, projectId));

        // 3. 删除项目本身
        removeById(projectId);

        // 4. 级联删除关联团队相关数据
        if (teamId != null) {
            // 删除团队成员
            teamMemberMapper.delete(
                    new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId));
            // 删除团队导师关联
            teamTeacherMapper.delete(
                    new LambdaQueryWrapper<TeamTeacher>().eq(TeamTeacher::getTeamId, teamId));
            // 删除团队关联文件
            fileMapper.delete(
                    new LambdaQueryWrapper<FileEntity>().eq(FileEntity::getTeamId, teamId));
            // 删除团队
            teamMapper.deleteById(teamId);
        }
    }

    @Override
    public List<Map<String, Object>> getGanttData(Long projectId) {
        Project project = getById(projectId);
        if (project == null) {
            throw BusinessException.notFound("Project not found");
        }
        List<Map<String, Object>> ganttData = new ArrayList<>();
        JSONArray milestones = parseMilestones(project.getMilestones());

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
        JSONArray milestones = parseMilestones(project.getMilestones());
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
        int statusScore = "ARCHIVED".equals(project.getStatus()) || "CLOSED".equals(project.getStatus()) ? 0 :
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

    private JSONArray parseMilestones(String milestonesJson) {
        if (milestonesJson == null || milestonesJson.trim().isEmpty()) {
            return new JSONArray();
        }
        try {
            return JSON.parseArray(milestonesJson);
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
            case "ARCHIVED", "CLOSED" -> 100;
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
        if ("CLOSED".equals(project.getStatus())) {
            throw BusinessException.badRequest("项目已关闭，无法添加成员");
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

        // 批量查询用户信息
        List<Long> userIds = members.stream()
                .map(TeamMember::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = new HashMap<>();
        Map<Long, List<Map<String, Object>>> skillsMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
            // 批量查询技能
            List<UserSkill> allSkills = userSkillMapper.selectList(
                    new LambdaQueryWrapper<UserSkill>().in(UserSkill::getUserId, userIds));
            for (UserSkill skill : allSkills) {
                Map<String, Object> skillMap = new HashMap<>();
                skillMap.put("id", skill.getId());
                skillMap.put("skillName", skill.getSkillName());
                skillMap.put("level", skill.getLevel());
                skillsMap.computeIfAbsent(skill.getUserId(), k -> new ArrayList<>()).add(skillMap);
            }
        }

        return members.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            User user = userMap.get(m.getUserId());
            map.put("id", m.getId());
            map.put("userId", m.getUserId());
            map.put("userName", user != null ? user.getUsername() : "Unknown");
            map.put("avatar", user != null ? user.getAvatar() : null);
            map.put("major", user != null ? user.getMajor() : null);
            map.put("grade", user != null ? user.getGrade() : null);
            map.put("bio", user != null ? user.getBio() : null);
            map.put("role", m.getRole());
            map.put("joinedAt", m.getJoinedAt());
            map.put("skills", skillsMap.getOrDefault(m.getUserId(), new ArrayList<>()));
            return map;
        }).collect(Collectors.toList());
    }
}
