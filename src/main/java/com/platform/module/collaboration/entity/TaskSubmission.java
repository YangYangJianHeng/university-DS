package com.platform.module.collaboration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_submission")
public class TaskSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long submitterId;
    private String title;
    private String description;
    private String fileUrls;
    private String status;
    private String reviewComment;
    private Long reviewerId;
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
