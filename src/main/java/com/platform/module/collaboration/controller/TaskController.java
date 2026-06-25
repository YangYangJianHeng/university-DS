package com.platform.module.collaboration.controller;

import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.collaboration.entity.Message;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.mapper.MessageMapper;
import com.platform.module.collaboration.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Tag(name = "Task & Collaboration")
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;
    private final MessageMapper messageMapper;

    // In-memory vote storage for simplicity (no Vote entity)
    private final Map<Long, Map<String, Object>> votes = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> voteRecords = new ConcurrentHashMap<>();
    private long voteIdCounter = 1;

    public TaskController(TaskService taskService, MessageMapper messageMapper) {
        this.taskService = taskService;
        this.messageMapper = messageMapper;
    }

    // ---- Task APIs ----

    @PostMapping("/task")
    @Operation(summary = "Create task")
    public Result<Task> createTask(@RequestBody Task task) {
        return Result.success(taskService.create(task));
    }

    @GetMapping("/task/{id}")
    @Operation(summary = "Get task detail")
    public Result<Task> detail(@PathVariable Long id) {
        Task task = taskService.getById(id);
        if (task == null) {
            return Result.notFound("Task not found");
        }
        return Result.success(task);
    }

    @PutMapping("/task/{id}")
    @Operation(summary = "Update task")
    public Result<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id);
        return Result.success(taskService.updateTask(task));
    }

    @PutMapping("/task/{id}/status")
    @Operation(summary = "Update task status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        taskService.updateStatus(id, status);
        return Result.success();
    }

    @GetMapping("/task/list")
    @Operation(summary = "List tasks by project (if projectId is null, return all)")
    public Result<List<Task>> listByProject(@RequestParam(required = false) Long projectId) {
        if (projectId != null) {
            return Result.success(taskService.getByProject(projectId));
        }
        return Result.success(taskService.list());
    }

    @GetMapping("/task/my")
    @Operation(summary = "Get my tasks")
    public Result<List<Task>> myTasks() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(taskService.getMyTasks(userId));
    }

    // ---- Vote APIs (in-memory) ----

    @PostMapping("/vote")
    @Operation(summary = "Create vote")
    public Result<Map<String, Object>> createVote(@RequestBody Map<String, Object> request) {
        Long voteId = voteIdCounter++;
        Map<String, Object> vote = new HashMap<>();
        vote.put("id", voteId);
        vote.put("title", request.get("title"));
        vote.put("options", request.get("options"));
        vote.put("deadline", request.get("deadline"));
        vote.put("status", "ACTIVE");
        vote.put("createdBy", UserContext.getCurrentUserId());
        vote.put("createdAt", LocalDateTime.now().toString());
        votes.put(voteId, vote);
        voteRecords.put(voteId, new ArrayList<>());

        Map<String, Object> result = new HashMap<>(vote);
        return Result.success(result);
    }

    @PostMapping("/vote/{id}/cast")
    @Operation(summary = "Cast vote")
    public Result<Void> castVote(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> vote = votes.get(id);
        if (vote == null) {
            return Result.notFound("Vote not found");
        }
        Long userId = UserContext.getCurrentUserId();
        List<Long> records = voteRecords.get(id);
        if (records.contains(userId)) {
            return Result.conflict("Already voted");
        }
        records.add(userId);
        return Result.success();
    }

    // ---- Weekly Report ----

    @GetMapping("/team/{teamId}/weekly-report")
    @Operation(summary = "Get weekly report")
    public Result<Map<String, Object>> weeklyReport(@PathVariable Long teamId) {
        Map<String, Object> report = new HashMap<>();
        report.put("teamId", teamId);

        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        report.put("weekStart", weekStart.toString());
        report.put("weekEnd", weekEnd.toString());

        // Count messages this week
        long messageCount = messageMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Message>()
                        .eq(Message::getTeamId, teamId)
                        .ge(Message::getCreatedAt, weekStart.atStartOfDay()));
        report.put("messageCount", messageCount);

        // Count tasks
        long taskCount = taskService.count(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Task>()
                        .eq(Task::getProjectId, teamId));
        report.put("taskCount", taskCount);

        report.put("generatedAt", LocalDateTime.now().toString());
        return Result.success(report);
    }
}
