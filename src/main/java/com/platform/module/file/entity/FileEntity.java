package com.platform.module.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file")
public class FileEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teamId;
    private Long projectId;
    private Long taskId;
    private Long parentId;
    private String name;
    private String type;
    private String fileKey;
    private Long fileSize;
    private String mimeType;
    private Integer version;
    private Long uploaderId;
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
