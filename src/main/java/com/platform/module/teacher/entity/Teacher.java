package com.platform.module.teacher.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("teacher")
public class Teacher {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String department;
    private String title;
    private String researchFields;
    private Integer maxTeams;
    private Integer currentTeams;
    private Integer isVerified;
    private BigDecimal rating;
}
