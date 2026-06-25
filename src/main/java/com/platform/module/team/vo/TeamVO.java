package com.platform.module.team.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamVO {
    private Long id;
    private String name;
    private Long leaderId;
    private String leaderName;
    private Long recruitId;
    private String status;
    private Integer memberCount;
    private LocalDateTime createdAt;
}
