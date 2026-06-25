package com.platform.module.team.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TimelineEvent {
    private String type;
    private String description;
    private LocalDateTime timestamp;
}
