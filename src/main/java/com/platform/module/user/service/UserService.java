package com.platform.module.user.service;

import com.platform.module.user.dto.ChangePasswordDTO;
import com.platform.module.user.dto.LoginDTO;
import com.platform.module.user.dto.RegisterDTO;
import com.platform.module.user.dto.UpdateProfileDTO;
import com.platform.module.user.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UserService {

    void register(RegisterDTO dto);

    Map<String, Object> login(LoginDTO dto);

    UserVO getProfile(Long userId);

    void updateProfile(Long userId, UpdateProfileDTO dto);

    void changePassword(Long userId, ChangePasswordDTO dto);

    void addSkill(Long userId, String skillName, String level);

    void deleteSkill(Long userId, Long skillId);

    List<Map<String, Object>> getSkills(Long userId);

    UserVO getUserById(Long userId);

    String uploadAvatar(Long userId, MultipartFile file);

    /**
     * 更新导师信息（院系、职称、研究方向）
     */
    void updateTeacherProfile(Long userId, String department, String title, String researchFields);

    /**
     * 注销账户（普通用户和导师可用）- 清理所有关联数据并删除账户
     */
    void deleteAccount(Long userId);
}
