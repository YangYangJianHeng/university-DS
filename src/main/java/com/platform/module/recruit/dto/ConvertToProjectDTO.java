package com.platform.module.recruit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 招募转项目请求DTO
 */
@Data
public class ConvertToProjectDTO {

    /** 项目名称（默认使用招募标题） */
    private String projectName;

    /** 项目描述（默认使用招募描述） */
    private String projectDescription;

    /** 是否继续招募：0=停止，1=继续 */
    @NotNull(message = "请选择是否继续招募")
    private Integer continueRecruit;

    /** 为团队成员批量创建的任务列表 */
    private List<TaskAssignment> tasks;

    @Data
    public static class TaskAssignment {
        /** 任务标题 */
        @NotBlank(message = "任务标题不能为空")
        private String title;

        /** 任务描述 */
        private String description;

        /** 优先级：LOW/MEDIUM/HIGH/URGENT */
        private String priority;

        /** 截止日期 */
        private String deadline;
    }
}
