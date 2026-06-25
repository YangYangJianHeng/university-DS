package com.platform.module.collaboration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.collaboration.dto.*;
import com.platform.module.collaboration.entity.*;

import java.util.List;

public interface TaskService extends IService<Task> {

    // ========== 任务管理 ==========

    /** 创建任务（负责人专属，支持多人指派） */
    Task createTask(TaskCreateRequest request);

    /** 获取任务详情（含执行人、成果、评论） */
    TaskDetailVO getTaskDetail(Long taskId);

    /** 更新任务 */
    Task updateTask(Task task);

    /** 更新任务状态 */
    void updateStatus(Long id, String status);

    /** 获取项目下所有任务 */
    List<Task> getByProject(Long projectId);

    /** 获取我的任务（我参与的所有任务） */
    List<Task> getMyTasks(Long userId);

    /** 获取负责人管理的所有项目的任务概览 */
    List<Task> getManagedTasks(Long leaderId);

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
