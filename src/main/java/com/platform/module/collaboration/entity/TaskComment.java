package com.platform.module.collaboration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_comment")
public class TaskComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
    private String content;
    private String mentionedUserIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
