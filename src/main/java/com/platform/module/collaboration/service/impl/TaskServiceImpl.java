package com.platform.module.collaboration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.module.collaboration.entity.Message;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.mapper.MessageMapper;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.collaboration.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private final MessageMapper messageMapper;

    public TaskServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public Task create(Task task) {
        task.setStatus("TODO");
        save(task);
        return task;
    }

    @Override
    public Task updateTask(Task task) {
        Task existing = getById(task.getId());
        if (existing == null) {
            throw BusinessException.notFound("Task not found");
        }
        if (task.getTitle() != null) existing.setTitle(task.getTitle());
        if (task.getDescription() != null) existing.setDescription(task.getDescription());
        if (task.getAssigneeId() != null) existing.setAssigneeId(task.getAssigneeId());
        if (task.getPriority() != null) existing.setPriority(task.getPriority());
        if (task.getDeadline() != null) existing.setDeadline(task.getDeadline());
        if (task.getStatus() != null) existing.setStatus(task.getStatus());
        updateById(existing);
        return existing;
    }

    @Override
    public void updateStatus(Long id, String status) {
        Task task = getById(id);
        if (task == null) {
            throw BusinessException.notFound("Task not found");
        }
        task.setStatus(status);
        updateById(task);
    }

    @Override
    public List<Task> getByProject(Long projectId) {
        return list(new LambdaQueryWrapper<Task>()
                .eq(Task::getProjectId, projectId)
                .orderByDesc(Task::getCreatedAt));
    }

    @Override
    public List<Task> getMyTasks(Long userId) {
        return list(new LambdaQueryWrapper<Task>()
                .eq(Task::getAssigneeId, userId)
                .orderByDesc(Task::getCreatedAt));
    }
}
