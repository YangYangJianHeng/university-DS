package com.platform.module.recruit.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateRecruitDTO {

    private String title;

    private String projectIcon;

    private String projectName;

    private String description;

    private String projectType;

    private List<String> requiredSkills;

    private Integer maxMembers;

    private LocalDateTime deadline;
}
