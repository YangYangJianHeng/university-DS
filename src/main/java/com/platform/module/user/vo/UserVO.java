package com.platform.module.user.vo;

import com.platform.module.user.entity.UserSkill;
import lombok.Data;

import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String phone;
    private String studentId;
    private String username;
    private String major;
    private String grade;
    private String avatar;
    private String bio;
    private String role;
    private Integer status;
    private java.time.LocalDateTime createdAt;
    private List<UserSkill> skills;

    // 导师特有字段（仅 TEACHER 角色时有值）
    private String department;       // 院系
    private String title;            // 职称
    private String researchFields;   // 研究方向
    private Integer isVerified;      // 是否已认证
    private java.math.BigDecimal rating;   // 评分
    private Integer maxTeams;        // 最大指导团队数
    private Integer currentTeams;    // 当前指导团队数
}
