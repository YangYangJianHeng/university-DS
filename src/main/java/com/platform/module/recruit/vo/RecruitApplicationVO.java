package com.platform.module.recruit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecruitApplicationVO {
    private Long id;
    private Long recruitId;
    private Long applicantId;
    private String applicantName;
    private String applicantAvatar;
    private String status;
    private LocalDateTime appliedAt;
}
