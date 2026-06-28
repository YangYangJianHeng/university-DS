package com.platform.module.team.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamMemberVO {
    private Long id;
    private Long teamId;
    private Long userId;
    private String userName;
    private String avatar;
    private String role;
    private LocalDateTime joinedAt;
}
