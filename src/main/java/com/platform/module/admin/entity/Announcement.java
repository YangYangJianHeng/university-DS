package com.platform.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
