package com.platform.module.teacher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.teacher.entity.Teacher;

import java.util.List;
import java.util.Map;

public interface TeacherService extends IService<Teacher> {

    Teacher register(Long userId, String department, String title, String researchFields, Integer maxTeams);

    Teacher updateTeacher(Long id, String department, String title, String researchFields, Integer maxTeams);

    void inviteTeacher(Long teamId, Long teacherId);

    void approveInvitation(Long invitationId);

    List<Map<String, Object>> getMyTeams(Long teacherId);

    void addGuidanceLog(Long teacherId, Long teamId, String content);

    List<Map<String, Object>> getGuidanceLogs(Long teacherId);
}
