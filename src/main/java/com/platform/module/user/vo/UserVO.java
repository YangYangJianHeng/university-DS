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
    private List<UserSkill> skills;
}
