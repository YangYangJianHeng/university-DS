package com.platform.module.achievement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("achievement")
public class Achievement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long teamId;
    private String title;
    private String type;
    private String description;
    private String awardLevel;
    private String fileUrls;
    private Integer isPublic;
    private Integer isFeatured;
    private Integer viewCount;
    private Integer likeCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
