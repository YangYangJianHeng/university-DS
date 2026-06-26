package com.platform.module.collaboration.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskCreateRequest {
    private Long projectId;
    private String title;
    private String description;
    private String requiredSkills;
    private String priority;
    private LocalDateTime deadline;

    /** 指派执行人列表 */
    private List<AssigneeInfo> assignees;

    @Data
    public static class AssigneeInfo {
        private Long userId;
        private String assignmentNote;
    }
}
