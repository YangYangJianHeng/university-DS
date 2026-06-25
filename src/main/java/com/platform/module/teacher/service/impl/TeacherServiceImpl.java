package com.platform.module.teacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.module.teacher.entity.Teacher;
import com.platform.module.teacher.mapper.TeacherMapper;
import com.platform.module.teacher.service.TeacherService;
import com.platform.module.team.entity.TeamTeacher;
import com.platform.module.team.mapper.TeamTeacherMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    private final TeamTeacherMapper teamTeacherMapper;

    public TeacherServiceImpl(TeamTeacherMapper teamTeacherMapper) {
        this.teamTeacherMapper = teamTeacherMapper;
    }

    @Override
    public Teacher register(Long userId, String department, String title, String researchFields, Integer maxTeams) {
        Teacher teacher = new Teacher();
        teacher.setUserId(userId);
        teacher.setDepartment(department);
        teacher.setTitle(title);
        teacher.setResearchFields(researchFields);
        teacher.setMaxTeams(maxTeams != null ? maxTeams : 5);
        teacher.setCurrentTeams(0);
        teacher.setIsVerified(0);
        save(teacher);
        return teacher;
    }

    @Override
    public Teacher updateTeacher(Long id, String department, String title, String researchFields, Integer maxTeams) {
        Teacher teacher = getById(id);
        if (teacher == null) {
            throw BusinessException.notFound("Teacher not found");
        }
        if (department != null) teacher.setDepartment(department);
        if (title != null) teacher.setTitle(title);
        if (researchFields != null) teacher.setResearchFields(researchFields);
        if (maxTeams != null) teacher.setMaxTeams(maxTeams);
        updateById(teacher);
        return teacher;
    }

    @Override
    public void inviteTeacher(Long teamId, Long teacherId) {
        Teacher teacher = getById(teacherId);
        if (teacher == null) {
            throw BusinessException.notFound("Teacher not found");
        }
        TeamTeacher teamTeacher = new TeamTeacher();
        teamTeacher.setTeamId(teamId);
        teamTeacher.setTeacherId(teacherId);
        teamTeacher.setStatus("PENDING");
        teamTeacherMapper.insert(teamTeacher);
    }

    @Override
    public void approveInvitation(Long invitationId) {
        TeamTeacher teamTeacher = teamTeacherMapper.selectById(invitationId);
        if (teamTeacher == null) {
            throw BusinessException.notFound("Invitation not found");
        }
        teamTeacher.setStatus("APPROVED");
        teamTeacher.setApprovedAt(LocalDateTime.now());
        teamTeacherMapper.updateById(teamTeacher);

        Teacher teacher = getById(teamTeacher.getTeacherId());
        if (teacher != null) {
            teacher.setCurrentTeams(teacher.getCurrentTeams() != null ? teacher.getCurrentTeams() + 1 : 1);
            updateById(teacher);
        }
    }

    @Override
    public List<Map<String, Object>> getMyTeams(Long teacherId) {
        List<TeamTeacher> relations = teamTeacherMapper.selectList(
                new LambdaQueryWrapper<TeamTeacher>()
                        .eq(TeamTeacher::getTeacherId, teacherId)
                        .eq(TeamTeacher::getStatus, "APPROVED"));
        return relations.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("teamTeacherId", r.getId());
            map.put("teamId", r.getTeamId());
            map.put("status", r.getStatus());
            map.put("invitedAt", r.getInvitedAt());
            map.put("approvedAt", r.getApprovedAt());
            return map;
        }).collect(Collectors.toList());
    }

    // 内存存储指导日志（简化实现）
    private final List<Map<String, Object>> guidanceLogs = new ArrayList<>();

    @Override
    public void addGuidanceLog(Long teacherId, Long teamId, String content) {
        Teacher teacher = getById(teacherId);
        if (teacher == null) {
            throw BusinessException.notFound("Teacher not found");
        }
        Map<String, Object> log = new HashMap<>();
        log.put("id", System.currentTimeMillis());
        log.put("teacherId", teacherId);
        log.put("teamId", teamId);
        log.put("content", content);
        log.put("createdAt", LocalDateTime.now().toString());
        guidanceLogs.add(log);
    }

    @Override
    public List<Map<String, Object>> getGuidanceLogs(Long teacherId) {
        Teacher teacher = getById(teacherId);
        if (teacher == null) {
            throw BusinessException.notFound("Teacher not found");
        }
        return guidanceLogs.stream()
                .filter(log -> teacherId.equals(log.get("teacherId")))
                .collect(Collectors.toList());
    }
}
