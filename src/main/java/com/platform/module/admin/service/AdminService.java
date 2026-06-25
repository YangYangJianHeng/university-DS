package com.platform.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.module.admin.entity.AuditLog;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.user.entity.User;

import java.util.List;
import java.util.Map;

public interface AdminService {

    IPage<User> listUsers(int page, int size);

    void updateUserStatus(Long userId, Integer status);

    IPage<AuditLog> listAuditLogs(String status, int page, int size);

    void processAudit(Long auditId, String action, Long operatorId);

    Map<String, Object> getDashboardStats();

    Map<String, Object> getWeeklyReport();

    void createAnnouncement(String title, String content, String type);

    List<Map<String, Object>> listAnnouncements();

    // Recruit audit
    IPage<Recruit> listPendingRecruits(int page, int size);

    void auditRecruit(Long recruitId, String action, Long operatorId);

    // User role management
    void updateUserRole(Long userId, String role);
}
