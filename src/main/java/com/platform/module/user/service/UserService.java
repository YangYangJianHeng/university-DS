package com.platform.module.user.service;

import com.platform.module.user.dto.LoginDTO;
import com.platform.module.user.dto.RegisterDTO;
import com.platform.module.user.dto.UpdateProfileDTO;
import com.platform.module.user.vo.UserVO;

import java.util.Map;

public interface UserService {

    void register(RegisterDTO dto);

    Map<String, Object> login(LoginDTO dto);

    UserVO getProfile(Long userId);

    void updateProfile(Long userId, UpdateProfileDTO dto);

    void addSkill(Long userId, String skillName, String level);

    void deleteSkill(Long userId, Long skillId);

    UserVO getUserById(Long userId);
}
