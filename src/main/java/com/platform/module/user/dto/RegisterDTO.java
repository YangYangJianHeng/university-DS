package com.platform.module.user.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String phone;
    private String password;
    private String username;
    private String studentId;
    private String major;
    private String grade;
}
