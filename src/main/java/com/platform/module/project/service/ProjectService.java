package com.platform.module.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.project.entity.Project;

import java.util.List;
import java.util.Map;

public interface ProjectService extends IService<Project> {

    Project create(Long teamId, String name, String description);

    Project update(Long id, String name, String description, String status);

    void addMilestone(Long projectId, Map<String, Object> milestone);

    void updateMilestone(Long projectId, Long milestoneId, Map<String, Object> milestone);

    void archive(Long projectId);

    List<Map<String, Object>> getGanttData(Long projectId);

    Map<String, Object> getRiskAssessment(Long projectId);

    List<Project> getMyProjects(Long userId);

    void addProjectMember(Long projectId, Long userId);

    List<Map<String, Object>> getProjectMembers(Long projectId);
}
