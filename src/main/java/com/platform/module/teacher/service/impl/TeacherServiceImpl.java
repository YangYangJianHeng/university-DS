package com.platform.module.teacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.module.notification.entity.Notification;
import com.platform.module.notification.mapper.NotificationMapper;
import com.platform.module.teacher.entity.Teacher;
import com.platform.module.teacher.mapper.TeacherMapper;
import com.platform.module.teacher.service.TeacherService;
import com.platform.module.team.entity.Team;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.entity.TeamTeacher;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.team.mapper.TeamTeacherMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    private final TeamTeacherMapper teamTeacherMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamMapper teamMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;

    public TeacherServiceImpl(TeamTeacherMapper teamTeacherMapper,
                              TeamMemberMapper teamMemberMapper,
                              TeamMapper teamMapper,
                              UserMapper userMapper,
                              NotificationMapper notificationMapper) {
        this.teamTeacherMapper = teamTeacherMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.teamMapper = teamMapper;
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
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
    public List<Map<String, Object>> listWithUserInfo(String department) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        if (department != null && !department.isEmpty()) {
            wrapper.like(Teacher::getDepartment, department);
        }
        wrapper.orderByDesc(Teacher::getRating);

        List<Teacher> teachers = list(wrapper);
        return teachers.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("userId", t.getUserId());
            map.put("department", t.getDepartment());
            map.put("title", t.getTitle());
            map.put("researchFields", t.getResearchFields());
            map.put("maxTeams", t.getMaxTeams());
            map.put("currentTeams", t.getCurrentTeams() != null ? t.getCurrentTeams() : 0);
            map.put("isVerified", t.getIsVerified());
            map.put("rating", t.getRating());

            // 关联用户表获取姓名
            User user = userMapper.selectById(t.getUserId());
            if (user != null) {
                map.put("name", user.getUsername());
                map.put("avatar", user.getAvatar());
            } else {
                map.put("name", "未知导师");
            }
            return map;
        }).collect(Collectors.toList());
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
    @Transactional(rollbackFor = Exception.class)
    public void inviteTeacher(Long teamId, Long teacherId, String message) {
        Teacher teacher = getById(teacherId);
        if (teacher == null) {
            throw BusinessException.notFound("Teacher not found");
        }
        // 检查是否已有待处理的邀请
        Long existing = teamTeacherMapper.selectCount(
                new LambdaQueryWrapper<TeamTeacher>()
                        .eq(TeamTeacher::getTeamId, teamId)
                        .eq(TeamTeacher::getTeacherId, teacherId)
                        .eq(TeamTeacher::getStatus, "PENDING"));
        if (existing > 0) {
            throw BusinessException.conflict("已向该导师发送过邀请，请等待回复");
        }
        TeamTeacher teamTeacher = new TeamTeacher();
        teamTeacher.setTeamId(teamId);
        teamTeacher.setTeacherId(teacherId);
        teamTeacher.setStatus("PENDING");
        teamTeacher.setMessage(message);
        teamTeacherMapper.insert(teamTeacher);

        // 发送通知给教师
        Team team = teamMapper.selectById(teamId);
        String teamName = team != null ? team.getName() : "未知团队";
        Notification notification = new Notification();
        notification.setUserId(teacher.getUserId());
        notification.setType("TEACHER_INVITE");
        notification.setTitle("🔔 新导师邀请");
        notification.setContent("团队「" + teamName + "」邀请您担任指导老师" +
                (message != null && !message.isEmpty() ? "，留言：" + message : ""));
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

            // 将教师加入 team_member 表，角色为 TEACHER，获得项目管理和任务发布权限
            Long userId = teacher.getUserId();
            Long existingMember = teamMemberMapper.selectCount(
                    new LambdaQueryWrapper<TeamMember>()
                            .eq(TeamMember::getTeamId, teamTeacher.getTeamId())
                            .eq(TeamMember::getUserId, userId));
            if (existingMember == 0) {
                TeamMember member = new TeamMember();
                member.setTeamId(teamTeacher.getTeamId());
                member.setUserId(userId);
                member.setRole("TEACHER");
                teamMemberMapper.insert(member);
            }

            // 通知教师邀请已通过
            Team team = teamMapper.selectById(teamTeacher.getTeamId());
            String teamName = team != null ? team.getName() : "未知团队";
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType("TEACHER_APPROVED");
            notification.setTitle("✅ 导师邀请已通过");
            notification.setContent("您已正式成为团队「" + teamName + "」的指导老师，可以管理项目和发布任务了");
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        }
    }

    @Override
    public List<Map<String, Object>> getPendingInvitations(Long userId) {
        Teacher teacher = getOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (teacher == null) return Collections.emptyList();

        List<TeamTeacher> invites = teamTeacherMapper.selectList(
                new LambdaQueryWrapper<TeamTeacher>()
                        .eq(TeamTeacher::getTeacherId, teacher.getId())
                        .eq(TeamTeacher::getStatus, "PENDING")
                        .orderByDesc(TeamTeacher::getInvitedAt));
        return invites.stream().map(inv -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", inv.getId());
            map.put("teamId", inv.getTeamId());
            map.put("teacherId", inv.getTeacherId());
            map.put("message", inv.getMessage());
            map.put("status", inv.getStatus());
            map.put("invitedAt", inv.getInvitedAt());

            Team team = teamMapper.selectById(inv.getTeamId());
            if (team != null) {
                map.put("teamName", team.getName());
                // 查找队长信息
                User leader = userMapper.selectById(team.getLeaderId());
                map.put("inviterName", leader != null ? leader.getUsername() : "未知");
            }
            return map;
        }).collect(Collectors.toList());
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
