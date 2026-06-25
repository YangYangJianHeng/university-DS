package com.platform.module.recruit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recruit")
public class Recruit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long publisherId;
    private String title;
    private String description;
    private String projectType;
    private String requiredSkills;
    private Integer maxMembers;
    private Integer currentMembers;
    private LocalDateTime deadline;
    private String status;
    private String auditStatus;
    private Integer viewCount;

    // 转为项目后的关联
    private Long projectId;
    private Long teamId;
    private Integer continueRecruit;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
