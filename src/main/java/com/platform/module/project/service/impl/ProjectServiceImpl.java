package com.platform.module.project.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.project.service.ProjectService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Override
    public Project create(Long teamId, String name, String description) {
        Project project = new Project();
        project.setTeamId(teamId);
        project.setName(name);
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
            task.put("start", LocalDate.now().plusDays(i * 15).toString());
            task.put("end", LocalDate.now().plusDays(i * 15 + 10).toString());
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
        Map<String, Object> risk = new HashMap<>();
        risk.put("overall", "LOW");
        risk.put("score", 25);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("type", "进度风险");
        item1.put("level", "LOW");
        item1.put("description", "项目进度正常，里程碑按计划完成");
        items.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("type", "技术风险");
        item2.put("level", "MEDIUM");
        item2.put("description", "项目中使用了新的技术栈，需要关注学习成本");
        items.add(item2);

        Map<String, Object> item3 = new HashMap<>();
        item3.put("type", "人员风险");
        item3.put("level", "LOW");
        item3.put("description", "团队成员稳定，无离职风险");
        items.add(item3);

        risk.put("items", items);
        return risk;
    }

    @Override
    public List<Project> getMyProjects(Long userId) {
        return list(new LambdaQueryWrapper<Project>()
                .eq(Project::getTeamId, userId)
                .or(w -> w.apply("team_id IN (SELECT team_id FROM team_member WHERE user_id = {0})", userId))
                .orderByDesc(Project::getCreatedAt));
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
}
