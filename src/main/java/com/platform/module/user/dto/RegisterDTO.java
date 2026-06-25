package com.platform.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需要6-32位")
    private String password;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 30, message = "用户名长度需要2-30位")
    private String username;

    private String studentId;
    private String major;
    private String grade;
}
