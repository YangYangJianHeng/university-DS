package com.platform.module.collaboration.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime deadline;

    /** 指派执行人列表 */
    private List<AssigneeInfo> assignees;

    /** 附件文件ID列表（先上传文件，再传ID过来） */
    private List<Long> fileIds;

    @Data
    public static class AssigneeInfo {
        private Long userId;
        private String assignmentNote;
    }
}
