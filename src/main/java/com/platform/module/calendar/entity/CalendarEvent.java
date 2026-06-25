package com.platform.module.calendar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("calendar_event")
public class CalendarEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String description;
    private LocalDateTime remindTime;
    private Long sourceId;
    private Integer isReminded;
}
