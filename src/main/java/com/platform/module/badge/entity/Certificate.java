package com.platform.module.badge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("certificate")
public class Certificate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long badgeId;
    private String badgeName;
    private String userName;
    private String studentId;
    private String imagePath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime awardedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
