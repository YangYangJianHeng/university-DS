package com.platform.module.recruit.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecruitVO {

    private Long id;
    private Long publisherId;
    private String publisherName;
    private String title;
    private String description;
    private String projectType;
    private List<String> requiredSkills;
    private Integer maxMembers;
    private Integer currentMembers;
    private LocalDateTime deadline;
    private String status;
    private String auditStatus;
    private Integer viewCount;
    private LocalDateTime createdAt;
}
