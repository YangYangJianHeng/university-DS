package com.platform.module.collaboration.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.entity.TaskAssignee;
import com.platform.module.collaboration.entity.TaskSubmission;
import com.platform.module.collaboration.entity.TaskComment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDetailVO {
    private Long id;
    private Long projectId;
    private Long creatorId;
    private String creatorName;
    private String title;
    private String description;
    private String requiredSkills;
    private String priority;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime deadline;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;

    private List<AssigneeVO> assignees;
    private List<SubmissionVO> submissions;
    private List<CommentVO> comments;

    @Data
    public static class AssigneeVO {
        private Long id;
        private Long userId;
        private String userName;
        private String userAvatar;
        private String assignmentNote;
        private String skillMatchScore;
        private String status;
        private LocalDateTime assignedAt;
    }

    @Data
    public static class SubmissionVO {
        private Long id;
        private Long submitterId;
        private String submitterName;
        private String title;
        private String description;
        private List<String> fileUrls;
        /** 文件元数据（含预览/下载地址、文件名、类型） */
        private List<FileInfoVO> fileInfos;
        private String status;
        private String reviewComment;
        private Long reviewerId;
        private String reviewerName;
        private LocalDateTime reviewedAt;
        private LocalDateTime createdAt;
    }

    @Data
    public static class FileInfoVO {
        private String previewUrl;
        private String downloadUrl;
        private String name;
        private String mimeType;
        private Long fileSize;
    }

    @Data
    public static class CommentVO {
        private Long id;
        private Long userId;
        private String userName;
        private String userAvatar;
        private String content;
        private List<Long> mentionedUserIds;
        private LocalDateTime createdAt;
    }
}
