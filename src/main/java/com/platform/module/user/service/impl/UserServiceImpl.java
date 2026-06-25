package com.platform.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.util.JwtUtil;
import com.platform.common.util.UserContext;
import com.platform.module.user.dto.ChangePasswordDTO;
import com.platform.module.user.dto.LoginDTO;
import com.platform.module.user.dto.RegisterDTO;
import com.platform.module.user.dto.UpdateProfileDTO;
import com.platform.module.user.entity.User;
import com.platform.module.user.entity.UserSkill;
import com.platform.module.user.mapper.UserMapper;
import com.platform.module.user.mapper.UserSkillMapper;
import com.platform.module.user.service.UserService;
import com.platform.module.user.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserSkillMapper userSkillMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper,
                           UserSkillMapper userSkillMapper,
                           JwtUtil jwtUtil,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userSkillMapper = userSkillMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // Check if phone already exists
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (count > 0) {
            throw BusinessException.conflict("Phone number already registered");
        }

        // Create new user
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setUsername(dto.getUsername());
        user.setStudentId(dto.getStudentId());
        user.setMajor(dto.getMajor());
        user.setGrade(dto.getGrade());
        user.setRole("USER");
        user.setStatus(1);

        userMapper.insert(user);
    }

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        // Find user by phone
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));

        if (user == null) {
            throw BusinessException.badRequest("User not found");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw BusinessException.forbidden("Account has been disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("Invalid password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // Build user info
        UserVO userVO = buildUserVO(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userVO);

        return result;
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("User not found");
        }
        return buildUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("User not found");
        }

        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getMajor() != null) {
            user.setMajor(dto.getMajor());
        }
        if (dto.getGrade() != null) {
            user.setGrade(dto.getGrade());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }

        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw BusinessException.badRequest("原密码不正确");
        }

        // 新密码不能与原密码相同
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSkill(Long userId, String skillName, String level) {
        // 检查是否已存在同名技能
        Long count = userSkillMapper.selectCount(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillName, skillName));
        if (count > 0) {
            throw BusinessException.conflict("该技能已存在");
        }
        if (skillName == null || skillName.trim().isEmpty()) {
            throw BusinessException.badRequest("技能名称不能为空");
        }

        UserSkill userSkill = new UserSkill();
        userSkill.setUserId(userId);
        userSkill.setSkillName(skillName.trim());
        userSkill.setLevel(level != null ? level : "BEGINNER");

        userSkillMapper.insert(userSkill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSkill(Long userId, Long skillId) {
        UserSkill userSkill = userSkillMapper.selectById(skillId);
        if (userSkill == null) {
            throw BusinessException.notFound("Skill not found");
        }
        if (!userSkill.getUserId().equals(userId)) {
            throw BusinessException.forbidden("Cannot delete skill belonging to another user");
        }

        userSkillMapper.deleteById(skillId);
    }

    @Override
    public List<Map<String, Object>> getSkills(Long userId) {
        List<UserSkill> skills = userSkillMapper.selectList(
                new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId));
        return skills.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("skillName", s.getSkillName());
            map.put("level", s.getLevel());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("User not found");
        }
        return buildUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("User not found");
        }

        // Validate file
        if (file.isEmpty()) {
            throw BusinessException.badRequest("文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw BusinessException.badRequest("只能上传图片文件");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw BusinessException.badRequest("图片大小不能超过5MB");
        }

        try {
            // Create upload directory
            String uploadDir = "uploads/avatars/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Build avatar URL (relative path, will be served by static resource mapping or nginx)
            String avatarUrl = "/uploads/avatars/" + filename;

            // Update user avatar
            user.setAvatar(avatarUrl);
            userMapper.updateById(user);

            return avatarUrl;
        } catch (IOException e) {
            throw BusinessException.error("头像上传失败: " + e.getMessage());
        }
    }

    private UserVO buildUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setStudentId(user.getStudentId());
        vo.setUsername(user.getUsername());
        vo.setMajor(user.getMajor());
        vo.setGrade(user.getGrade());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());

        // Fetch skills for this user
        List<UserSkill> skills = userSkillMapper.selectList(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, user.getId()));
        vo.setSkills(skills);

        return vo;
    }
}
