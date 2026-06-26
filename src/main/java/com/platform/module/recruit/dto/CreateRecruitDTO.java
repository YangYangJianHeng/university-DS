package com.platform.module.recruit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateRecruitDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String projectIcon;

    private String projectName;

    private String description;

    private String projectType;

    private List<String> requiredSkills;

    @Positive(message = "最大成员数必须为正数")
    private Integer maxMembers;

    private LocalDateTime deadline;
}
