package com.platform.module.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("project")
public class Project {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teamId;
    private Long leaderId;
    private String name;
    private String description;
    private String status;
    private LocalDate startedAt;
    private LocalDate endedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
