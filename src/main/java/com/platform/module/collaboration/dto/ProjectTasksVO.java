package com.platform.module.collaboration.dto;

import com.platform.module.collaboration.entity.Task;
import lombok.Data;

import java.util.List;

@Data
public class ProjectTasksVO {
    private Long projectId;
    private String projectName;
    private Long teamId;
    private String teamName;
    private List<TaskVO> tasks;

    @Data
    public static class TaskVO {
        private Long id;
        private Long projectId;
        private Long creatorId;
        private String creatorName;
        private String creatorAvatar;
        private String title;
        private String description;
        private String requiredSkills;
        private String priority;
        private String status;
        private java.time.LocalDateTime deadline;
        private java.time.LocalDateTime createdAt;
        private int assigneeCount;
    }
}
