package com.platform.module.user.dto;

import lombok.Data;

@Data
public class UpdateProfileDTO {
    private String username;
    private String major;
    private String grade;
    private String avatar;
    private String bio;
}
