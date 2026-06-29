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
    private String icon;
    private String description;
    /** 里程碑JSON数组，格式: [{"id":..., "name":"...", "description":"...", "deadline":"...", "status":"..."}] */
    private String milestones;
    private String status;
    private LocalDate startedAt;
    private LocalDate endedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ========== 瞬态字段（非数据库，仅返回前端用） ==========
    @TableField(exist = false)
    private String teamName;

    @TableField(exist = false)
    private String leaderName;

    @TableField(exist = false)
    private Long taskCount;

    @TableField(exist = false)
    private Long memberCount;
}
