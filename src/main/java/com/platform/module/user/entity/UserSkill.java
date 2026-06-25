package com.platform.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("user_skill")
public class UserSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String skillName;
    private String level;
}
