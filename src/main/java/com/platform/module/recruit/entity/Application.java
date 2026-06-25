package com.platform.module.recruit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("application")
public class Application {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recruitId;
    private Long applicantId;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime appliedAt;
}
