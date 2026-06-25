package com.platform.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetType;
    private Long targetId;
    private Long operatorId;
    private String action;
    private String reason;
    private String beforeStatus;
    private String afterStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
