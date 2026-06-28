package com.platform.module.recruit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime deadline;
}
