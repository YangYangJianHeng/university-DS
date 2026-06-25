package com.platform.module.collaboration.controller;

import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.collaboration.dto.*;
import com.platform.module.collaboration.entity.*;
import com.platform.module.collaboration.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "任务中心")
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // ==================== 任务管理 ====================

    @PostMapping("/task")
    @Operation(summary = "创建任务（负责人专属，支持多人指派）")
    public Result<Task> createTask(@RequestBody TaskCreateRequest request) {
        return Result.success(taskService.createTask(request));
    }

    @GetMapping("/task/{id}")
    @Operation(summary = "获取任务详情（含执行人、成果、评论）")
    public Result<TaskDetailVO> detail(@PathVariable Long id) {
        return Result.success(taskService.getTaskDetail(id));
    }

    @PutMapping("/task/{id}")
    @Operation(summary = "更新任务")
    public Result<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id);
        return Result.success(taskService.updateTask(task));
    }

    @PutMapping("/task/{id}/status")
    @Operation(summary = "更新任务状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        taskService.updateStatus(id, request.get("status"));
        return Result.success();
    }

    @GetMapping("/task/list")
    @Operation(summary = "获取项目下任务列表")
    public Result<List<Task>> listByProject(@RequestParam(required = false) Long projectId) {
        if (projectId != null) {
            return Result.success(taskService.getByProject(projectId));
        }
        return Result.success(taskService.list());
    }

    @GetMapping("/task/my")
    @Operation(summary = "我的任务（我参与的所有任务）")
    public Result<List<Task>> myTasks() {
        return Result.success(taskService.getMyTasks(UserContext.getCurrentUserId()));
    }

    @GetMapping("/task/managed")
    @Operation(summary = "我管理的任务（负责人视角，支持多项目管理）")
    public Result<List<Task>> managedTasks() {
        return Result.success(taskService.getManagedTasks(UserContext.getCurrentUserId()));
    }

    // ==================== 人员指派 ====================

    @GetMapping("/task/{id}/recommend")
    @Operation(summary = "智能推荐匹配成员（基于技能标签，Top-5）")
    public Result<List<TaskDetailVO.AssigneeVO>> recommendAssignees(@PathVariable Long id) {
        return Result.success(taskService.recommendAssignees(id));
    }

    @PostMapping("/task/{id}/assignee")
    @Operation(summary = "添加任务执行人")
    public Result<Void> addAssignee(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String note = (String) request.get("assignmentNote");
        taskService.addAssignee(id, userId, note);
        return Result.success();
    }

    @DeleteMapping("/task/{id}/assignee/{userId}")
    @Operation(summary = "移除任务执行人")
    public Result<Void> removeAssignee(@PathVariable Long id, @PathVariable Long userId) {
        taskService.removeAssignee(id, userId);
        return Result.success();
    }

    @PutMapping("/task/{id}/accept")
    @Operation(summary = "接受/拒绝任务")
    public Result<Void> acceptTask(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        taskService.acceptTask(id, request.getOrDefault("accept", true));
        return Result.success();
    }

    @GetMapping("/task/{id}/assignees")
    @Operation(summary = "获取任务执行人列表")
    public Result<List<TaskAssignee>> getAssignees(@PathVariable Long id) {
        return Result.success(taskService.getAssignees(id));
    }

    // ==================== 成果提交 ====================

    @PostMapping("/task/submission")
    @Operation(summary = "提交阶段性成果")
    public Result<TaskSubmission> submitResult(@RequestBody SubmissionRequest request) {
        return Result.success(taskService.submitResult(request));
    }

    @GetMapping("/task/{id}/submissions")
    @Operation(summary = "获取任务成果提交记录")
    public Result<List<TaskSubmission>> getSubmissions(@PathVariable Long id) {
        return Result.success(taskService.getSubmissions(id));
    }

    @PutMapping("/task/submission/{id}/review")
    @Operation(summary = "验收成果（通过/退回）")
    public Result<Void> reviewSubmission(@PathVariable Long id, @RequestBody ReviewRequest request) {
        taskService.reviewSubmission(id, request);
        return Result.success();
    }

    // ==================== 评论 ====================

    @PostMapping("/task/{id}/comment")
    @Operation(summary = "添加评论")
    public Result<TaskComment> addComment(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        @SuppressWarnings("unchecked")
        List<Long> mentionedUserIds = (List<Long>) request.get("mentionedUserIds");
        return Result.success(taskService.addComment(id, content, mentionedUserIds));
    }

    @GetMapping("/task/{id}/comments")
    @Operation(summary = "获取任务评论列表")
    public Result<List<TaskComment>> getComments(@PathVariable Long id) {
        return Result.success(taskService.getComments(id));
    }
}
