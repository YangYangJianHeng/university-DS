package com.platform.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("user_experience")
public class UserExperience {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long projectId;
    private String role;
    private String achievement;
    private LocalDate startedAt;
    private LocalDate endedAt;
}
