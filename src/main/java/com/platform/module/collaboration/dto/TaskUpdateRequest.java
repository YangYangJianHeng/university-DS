package com.platform.module.collaboration.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskUpdateRequest {
    private String title;
    private String description;
    private String requiredSkills;
    private String priority;
    private String status;
    /** 截止日期，格式 yyyy-MM-dd HH:mm:ss */
    private String deadline;

    /** 新增的执行人列表 */
    private List<AssigneeInfo> addAssignees;

    /** 要移除的执行人ID列表（换人） */
    private List<Long> removeAssigneeIds;

    @Data
    public static class AssigneeInfo {
        private Long userId;
        private String assignmentNote;
    }
}
