package com.platform.module.team.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TimelineEvent {
    private String type;
    private String description;
    private LocalDateTime timestamp;
    /** 关联项目名称 */
    private String projectName;
    /** 关联任务ID */
    private Long taskId;
    /** 关联任务标题 */
    private String taskTitle;
}
