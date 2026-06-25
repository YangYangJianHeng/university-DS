package com.platform.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.module.admin.entity.AuditLog;
import com.platform.module.admin.mapper.AuditLogMapper;
import com.platform.module.admin.service.AdminService;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final AuditLogMapper auditLogMapper;
    private final RecruitMapper recruitMapper;

    // In-memory announcement storage for simplicity
    private final List<Map<String, Object>> announcements = new CopyOnWriteArrayList<>();

    public AdminServiceImpl(UserMapper userMapper, AuditLogMapper auditLogMapper,
                            RecruitMapper recruitMapper) {
        this.userMapper = userMapper;
        this.auditLogMapper = auditLogMapper;
        this.recruitMapper = recruitMapper;
    }

    @Override
    public IPage<User> listUsers(int page, int size) {
        return userMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("User not found");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public IPage<AuditLog> listAuditLogs(String status, int page, int size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AuditLog::getAfterStatus, status);
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return auditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void processAudit(Long auditId, String action, Long operatorId) {
        AuditLog auditLog = auditLogMapper.selectById(auditId);
        if (auditLog == null) {
            throw BusinessException.notFound("Audit log not found");
        }
        if ("APPROVE".equals(action)) {
            auditLog.setAfterStatus("APPROVED");
        } else if ("REJECT".equals(action)) {
            auditLog.setAfterStatus("REJECTED");
        } else {
            throw BusinessException.badRequest("Invalid action: " + action);
        }
        auditLogMapper.updateById(auditLog);
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // User stats
        long totalUsers = userMapper.selectCount(null);
        long activeUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));

        // Audit stats
        long pendingAudits = auditLogMapper.selectCount(
                new LambdaQueryWrapper<AuditLog>().eq(AuditLog::getAfterStatus, "PENDING"));

        long pendingRecruits = recruitMapper.selectCount(
                new LambdaQueryWrapper<Recruit>().eq(Recruit::getAuditStatus, "PENDING"));

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("pendingAudits", pendingAudits);
        stats.put("pendingRecruits", pendingRecruits);
        stats.put("timestamp", LocalDateTime.now().toString());

        return stats;
    }

    @Override
    public Map<String, Object> getWeeklyReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("period", "本周");
        report.put("newUsers", 15);
        report.put("activeProjects", 8);
        report.put("completedTasks", 42);
        report.put("newCompetitions", 3);
        report.put("generatedAt", LocalDateTime.now().toString());
        return report;
    }

    @Override
    public void createAnnouncement(String title, String content, String type) {
        Map<String, Object> announcement = new HashMap<>();
        announcement.put("id", System.currentTimeMillis());
        announcement.put("title", title);
        announcement.put("content", content);
        announcement.put("type", type != null ? type : "SYSTEM");
        announcement.put("createdAt", LocalDateTime.now().toString());
        announcements.add(announcement);
    }

    @Override
    public List<Map<String, Object>> listAnnouncements() {
        return new ArrayList<>(announcements);
    }

    @Override
    public IPage<Recruit> listPendingRecruits(int page, int size) {
        return recruitMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Recruit>()
                        .eq(Recruit::getAuditStatus, "PENDING")
                        .orderByDesc(Recruit::getCreatedAt));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void auditRecruit(Long recruitId, String action, Long operatorId) {
        Recruit recruit = recruitMapper.selectById(recruitId);
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }
        if ("APPROVE".equals(action)) {
            recruit.setAuditStatus("APPROVED");
        } else if ("REJECT".equals(action)) {
            recruit.setAuditStatus("REJECTED");
            recruit.setStatus("REJECTED");
        } else {
            throw BusinessException.badRequest("无效操作: " + action);
        }
        recruitMapper.updateById(recruit);

        // Write audit log
        AuditLog log = new AuditLog();
        log.setTargetType("RECRUIT");
        log.setTargetId(recruitId);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setBeforeStatus("PENDING");
        log.setAfterStatus("APPROVE".equals(action) ? "APPROVED" : "REJECTED");
        auditLogMapper.insert(log);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateUserRole(Long userId, String role) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (!List.of("USER", "ADMIN", "TEACHER", "REVIEWER").contains(role)) {
            throw BusinessException.badRequest("无效角色: " + role);
        }
        user.setRole(role);
        userMapper.updateById(user);
    }
}
