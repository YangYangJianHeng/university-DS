package com.platform.module.collaboration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.collaboration.dto.*;
import com.platform.module.collaboration.entity.*;
import com.platform.module.project.entity.Project;

import java.util.List;

public interface TaskService extends IService<Task> {

    // ========== 任务管理 ==========

    /** 创建任务（负责人专属，支持多人指派） */
    Task createTask(TaskCreateRequest request);

    /** 获取任务详情（含执行人、成果、评论） */
    TaskDetailVO getTaskDetail(Long taskId);

    /** 更新任务（支持执行人变更：换人功能） */
    Task updateTask(Long taskId, TaskUpdateRequest request);

    /** 更新任务状态 */
    void updateStatus(Long id, String status);

    /** 完成任务（验收中→已完成，仅创建者可操作） */
    void completeTask(Long taskId);

    /** 获取项目下所有任务 */
    List<Task> getByProject(Long projectId);

    /** 获取我的任务（我参与的所有任务） */
    List<Task> getMyTasks(Long userId);

    /** 获取负责人管理的所有项目的任务概览 */
    List<Task> getManagedTasks(Long leaderId);

    /** 获取用户所在团队的所有任务（所有团队成员可见） */
    List<Task> getTeamTasks(Long userId);

    /** 获取用户所在团队的任务按项目分组 */
    List<ProjectTasksVO> getTeamTasksGrouped(Long userId);

    /** 获取导师所指导团队的所有任务 */
    List<Task> getTeacherTeamTasks(Long userId);

    /** 获取导师所指导团队的项目列表 */
    List<Project> getTeacherProjects(Long userId);

    // ========== 人员指派 ==========

    /** 智能推荐匹配成员（基于技能标签） */
    List<TaskDetailVO.AssigneeVO> recommendAssignees(Long taskId);

    /** 添加任务执行人 */
    void addAssignee(Long taskId, Long userId, String assignmentNote);

    /** 移除任务执行人 */
    void removeAssignee(Long taskId, Long userId);

    /** 执行人接受/拒绝任务 */
    void acceptTask(Long taskId, boolean accept);

    /** 获取任务的所有执行人 */
    List<TaskAssignee> getAssignees(Long taskId);

    // ========== 成果提交 ==========

    /** 提交阶段性成果 */
    TaskSubmission submitResult(SubmissionRequest request);

    /** 获取任务的所有成果提交记录 */
    List<TaskSubmission> getSubmissions(Long taskId);

    /** 验收成果 */
    void reviewSubmission(Long submissionId, ReviewRequest request);

    // ========== 评论 ==========

    /** 添加评论 */
    TaskComment addComment(Long taskId, String content, List<Long> mentionedUserIds);

    /** 获取任务评论列表 */
    List<TaskComment> getComments(Long taskId);
}
