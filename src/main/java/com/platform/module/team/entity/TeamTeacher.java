package com.platform.module.team.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("team_teacher")
public class TeamTeacher {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teamId;
    private Long teacherId;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime invitedAt;
    private LocalDateTime approvedAt;
}
