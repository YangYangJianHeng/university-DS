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

    /** 关闭项目（同时关闭关联团队），关闭后不可编辑任何状态 */
    void closeProject(Long projectId);

    /** 重新开启已关闭的项目（同时开启关联团队） */
    void reopenProject(Long projectId);

    /** 删除项目（同时级联删除关联团队及任务等数据），仅管理员和项目创建者可操作 */
    void deleteProject(Long projectId);

    List<Map<String, Object>> getGanttData(Long projectId);

    Map<String, Object> getRiskAssessment(Long projectId);

    List<Project> getMyProjects(Long userId);

    void addProjectMember(Long projectId, Long userId);

    List<Map<String, Object>> getProjectMembers(Long projectId);
}
