package com.platform.module.collaboration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("task_assignee")
public class TaskAssignee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
    private String assignmentNote;
    private BigDecimal skillMatchScore;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime assignedAt;

    private LocalDateTime acceptedAt;
}
