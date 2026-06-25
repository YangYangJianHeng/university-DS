package com.platform.module.competition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("competition")
public class Competition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String level;
    private String category;
    private LocalDate registerStart;
    private LocalDate registerEnd;
    private LocalDate eventStart;
    private LocalDate eventEnd;
    private String organizer;
    private String officialUrl;
    private String description;
    private String schedule;
    private Integer viewCount;
    private Integer collectCount;
    private String status;
}
