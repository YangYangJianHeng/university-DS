package com.platform.module.collaboration.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("task")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long creatorId;
    private String title;
    private String description;
    private String requiredSkills;
    private String priority;
    private String status;

    private LocalDateTime deadline;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ========== 以下为 transient 展示字段，不映射数据库 ==========

    /** 创建者姓名 */
    @TableField(exist = false)
    private String creatorName;

    /** 创建者头像URL */
    @TableField(exist = false)
    private String creatorAvatar;

    /** 执行人数 */
    @TableField(exist = false)
    private Integer assigneeCount;

    /** 执行人姓名列表（逗号拼接） */
    @TableField(exist = false)
    private String assigneeNames;

    /** 执行人头像URL列表 */
    @TableField(exist = false)
    private List<String> assigneeAvatars;
}
