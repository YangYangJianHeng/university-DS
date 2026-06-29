package com.platform.module.teacher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.teacher.entity.Teacher;

import java.util.List;
import java.util.Map;

public interface TeacherService extends IService<Teacher> {

    Teacher register(Long userId, String department, String title, String researchFields, Integer maxTeams);

    Teacher updateTeacher(Long id, String department, String title, String researchFields, Integer maxTeams);

    /** 查询导师列表（关联用户表，包含姓名） */
    List<Map<String, Object>> listWithUserInfo(String department);

    void inviteTeacher(Long teamId, Long teacherId, String message);

    void approveInvitation(Long invitationId);

    /** 查询待处理的导师邀请（按 teacherId=userId 关联的 Teacher 记录） */
    List<Map<String, Object>> getPendingInvitations(Long userId);

    List<Map<String, Object>> getMyTeams(Long teacherId);

    void addGuidanceLog(Long teacherId, Long teamId, String content);

    List<Map<String, Object>> getGuidanceLogs(Long teacherId);
}
