package com.platform.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.module.admin.entity.AuditLog;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.project.entity.Project;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.competition.entity.Competition;
import com.platform.module.teacher.entity.Teacher;
import com.platform.module.team.entity.Team;
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

    // ---- Admin full management ----

    // Project management
    IPage<Project> listAllProjects(int page, int size);
    Project updateProject(Long id, String name, String description, String status);
    void deleteProject(Long id);

    // Task management
    IPage<Task> listAllTasks(int page, int size);
    Task updateTask(Long id, String title, String description, String priority, String status);
    void deleteTask(Long id);

    // Team management
    IPage<Team> listAllTeams(int page, int size);
    Team updateTeam(Long id, String name, String status);
    void deleteTeam(Long id);

    // Teacher management
    IPage<Teacher> listAllTeachers(int page, int size);
    Teacher updateTeacher(Long id, String department, String title, String researchFields, Integer isVerified);
    void deleteTeacher(Long id);

    // Competition management
    IPage<Competition> listAllCompetitions(int page, int size);
    Competition updateCompetition(Long id, Competition competition);
    void deleteCompetition(Long id);

    // Achievement management
    IPage<Achievement> listAllAchievements(int page, int size);
    Achievement updateAchievement(Long id, Achievement achievement);
    void deleteAchievement(Long id);
}
