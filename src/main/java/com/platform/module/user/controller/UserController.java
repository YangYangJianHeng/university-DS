package com.platform.module.user.controller;

import com.platform.common.annotation.RateLimit;
import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.user.dto.ChangePasswordDTO;
import com.platform.module.user.dto.LoginDTO;
import com.platform.module.user.dto.RegisterDTO;
import com.platform.module.user.dto.UpdateProfileDTO;
import com.platform.module.user.service.UserService;
import com.platform.module.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    public UserController(UserService userService, StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    @RateLimit(prefix = "rate:register", window = 60, maxRequests = 5, message = "注册过于频繁，请1分钟后再试")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        // Validate captcha
        if (!CaptchaController.validateCaptcha(redisTemplate, dto.getCaptchaKey(), dto.getCaptchaCode())) {
            return Result.badRequest("验证码错误或已过期");
        }
        userService.register(dto);
        return Result.success("Registration successful", null);
    }

    @RateLimit(prefix = "rate:login", window = 60, maxRequests = 10, message = "登录过于频繁，请1分钟后再试")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        // Validate captcha
        if (!CaptchaController.validateCaptcha(redisTemplate, dto.getCaptchaKey(), dto.getCaptchaCode())) {
            return Result.badRequest("验证码错误或已过期");
        }
        Map<String, Object> result = userService.login(dto);
        return Result.success(result);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        UserContext.clear();
        return Result.success();
    }

    @GetMapping("/profile")
    public Result<UserVO> getProfile() {
        Long userId = UserContext.getCurrentUserId();
        UserVO userVO = userService.getProfile(userId);
        return Result.success(userVO);
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UpdateProfileDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        userService.updateProfile(userId, dto);
        return Result.success("Profile updated successfully", null);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        userService.changePassword(userId, dto);
        return Result.success("Password changed successfully", null);
    }

    @GetMapping("/skills")
    public Result<List<Map<String, Object>>> getSkills() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(userService.getSkills(userId));
    }

    @PostMapping("/skills")
    public Result<Void> addSkill(@RequestBody Map<String, String> params) {
        Long userId = UserContext.getCurrentUserId();
        String skillName = params.get("skillName");
        String level = params.get("level");
        userService.addSkill(userId, skillName, level);
        return Result.success("Skill added successfully", null);
    }

    @DeleteMapping("/skills/{id}")
    public Result<Void> deleteSkill(@PathVariable("id") Long skillId) {
        Long userId = UserContext.getCurrentUserId();
        userService.deleteSkill(userId, skillId);
        return Result.success("Skill deleted successfully", null);
    }

    @GetMapping("/profile/{userId}")
    public Result<UserVO> getUserProfile(@PathVariable("userId") Long userId) {
        UserVO userVO = userService.getUserById(userId);
        return Result.success(userVO);
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getCurrentUserId();
        String avatarUrl = userService.uploadAvatar(userId, file);
        Map<String, String> result = new HashMap<>();
        result.put("avatar", avatarUrl);
        result.put("url", avatarUrl);
        return Result.success(result);
    }
}
