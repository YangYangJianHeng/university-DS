package com.platform.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.admin.entity.AuditLog;
import com.platform.module.admin.service.AdminService;
import com.platform.module.badge.entity.Badge;
import com.platform.module.badge.entity.Certificate;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.competition.entity.Competition;
import com.platform.module.project.entity.Project;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.teacher.entity.Teacher;
import com.platform.module.team.entity.Team;
import com.platform.module.user.entity.User;
import com.platform.module.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Admin Management")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @Operation(summary = "List users")
    public Result<IPage<UserVO>> listUsers(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listUsers(page, size));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Update user status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateUserStatus(id, status);
        return Result.success();
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Update user role")
    public Result<Void> updateUserRole(@PathVariable Long id, @RequestParam String role) {
        adminService.updateUserRole(id, role);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user (admin)")
    public Result<Void> deleteUser(@PathVariable Long id) {
        Long operatorId = UserContext.getCurrentUserId();
        adminService.deleteUser(id, operatorId);
        return Result.<Void>success("用户已删除", null);
    }

    @GetMapping("/audit/list")
    @Operation(summary = "List audit logs")
    public Result<IPage<AuditLog>> listAudit(@RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listAuditLogs(status, page, size));
    }

    @PutMapping("/audit/{id}")
    @Operation(summary = "Process audit")
    public Result<Void> processAudit(@PathVariable Long id, @RequestParam String action) {
        Long operatorId = UserContext.getCurrentUserId();
        adminService.processAudit(id, action, operatorId);
        return Result.success();
    }

    // ---- Recruit Audit ----

    @GetMapping("/recruits/pending")
    @Operation(summary = "List pending recruits")
    public Result<IPage<Recruit>> listPendingRecruits(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listPendingRecruits(page, size));
    }

    @PutMapping("/recruits/{id}/audit")
    @Operation(summary = "Approve or reject a recruit")
    public Result<Void> auditRecruit(@PathVariable Long id, @RequestParam String action) {
        Long operatorId = UserContext.getCurrentUserId();
        adminService.auditRecruit(id, action, operatorId);
        return Result.success();
    }

    // ---- Dashboard & Reports ----
    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get platform stats")
    public Result<Map<String, Object>> dashboardStats() {
        return Result.success(adminService.getDashboardStats());
    }

    @GetMapping("/report/weekly")
    @Operation(summary = "Get weekly report")
    public Result<Map<String, Object>> weeklyReport() {
        return Result.success(adminService.getWeeklyReport());
    }

    @PostMapping("/announcement")
    @Operation(summary = "Create announcement")
    public Result<Void> createAnnouncement(@RequestBody Map<String, String> request) {
        adminService.createAnnouncement(request.get("title"), request.get("content"), request.get("type"));
        return Result.success();
    }

    @GetMapping("/announcements")
    @Operation(summary = "List announcements")
    public Result<List<Map<String, Object>>> listAnnouncements() {
        return Result.success(adminService.listAnnouncements());
    }

    // ---- Admin Full Management APIs ----

    // Projects
    @GetMapping("/projects")
    @Operation(summary = "List all projects")
    public Result<IPage<Project>> listAllProjects(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listAllProjects(page, size));
    }

    @PutMapping("/projects/{id}")
    @Operation(summary = "Update project")
    public Result<Project> updateProject(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String status = (String) request.get("status");
        return Result.success(adminService.updateProject(id, name, description, status));
    }

    @DeleteMapping("/projects/{id}")
    @Operation(summary = "Delete project")
    public Result<Void> deleteProject(@PathVariable Long id) {
        adminService.deleteProject(id);
        return Result.success();
    }

    // Tasks
    @GetMapping("/tasks")
    @Operation(summary = "List all tasks")
    public Result<IPage<Task>> listAllTasks(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listAllTasks(page, size));
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Update task")
    public Result<Task> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String title = (String) request.get("title");
        String description = (String) request.get("description");
        String priority = (String) request.get("priority");
        String status = (String) request.get("status");
        return Result.success(adminService.updateTask(id, title, description, priority, status));
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Delete task")
    public Result<Void> deleteTask(@PathVariable Long id) {
        adminService.deleteTask(id);
        return Result.success();
    }

    // Teams
    @GetMapping("/teams")
    @Operation(summary = "List all teams")
    public Result<IPage<Team>> listAllTeams(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listAllTeams(page, size));
    }

    @PutMapping("/teams/{id}")
    @Operation(summary = "Update team")
    public Result<Team> updateTeam(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String status = (String) request.get("status");
        return Result.success(adminService.updateTeam(id, name, status));
    }

    @DeleteMapping("/teams/{id}")
    @Operation(summary = "Delete team")
    public Result<Void> deleteTeam(@PathVariable Long id) {
        adminService.deleteTeam(id);
        return Result.success();
    }

    // Teachers
    @GetMapping("/teachers")
    @Operation(summary = "List all teachers")
    public Result<IPage<Teacher>> listAllTeachers(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listAllTeachers(page, size));
    }

    @PutMapping("/teachers/{id}")
    @Operation(summary = "Update teacher")
    public Result<Teacher> updateTeacher(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String department = (String) request.get("department");
        String title = (String) request.get("title");
        String researchFields = (String) request.get("researchFields");
        Integer isVerified = request.get("isVerified") != null ? Integer.valueOf(request.get("isVerified").toString()) : null;
        return Result.success(adminService.updateTeacher(id, department, title, researchFields, isVerified));
    }

    @DeleteMapping("/teachers/{id}")
    @Operation(summary = "Delete teacher")
    public Result<Void> deleteTeacher(@PathVariable Long id) {
        adminService.deleteTeacher(id);
        return Result.success();
    }

    // ---- Competition Management ----

    @GetMapping("/competitions")
    @Operation(summary = "List all competitions")
    public Result<IPage<Competition>> listAllCompetitions(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listAllCompetitions(page, size));
    }

    @PostMapping("/competitions")
    @Operation(summary = "Create competition")
    public Result<Competition> createCompetition(@RequestBody Competition competition) {
        return Result.success(adminService.createCompetition(competition));
    }

    @PutMapping("/competitions/{id}")
    @Operation(summary = "Update competition")
    public Result<Competition> updateCompetition(@PathVariable Long id, @RequestBody Competition competition) {
        return Result.success(adminService.updateCompetition(id, competition));
    }

    @DeleteMapping("/competitions/{id}")
    @Operation(summary = "Delete competition")
    public Result<Void> deleteCompetition(@PathVariable Long id) {
        adminService.deleteCompetition(id);
        return Result.success();
    }

    // ---- Achievement Management ----

    @GetMapping("/achievements")
    @Operation(summary = "List all achievements")
    public Result<IPage<Achievement>> listAllAchievements(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        try {
            return Result.success(adminService.listAllAchievements(page, size));
        } catch (Exception e) {
            log.warn("Failed to list achievements in admin, returning empty result: {}", e.getMessage());
            IPage<Achievement> emptyPage = new Page<>(page, size);
            emptyPage.setRecords(java.util.Collections.emptyList());
            emptyPage.setTotal(0);
            return Result.success(emptyPage);
        }
    }

    @PutMapping("/achievements/{id}")
    @Operation(summary = "Update achievement")
    public Result<Achievement> updateAchievement(@PathVariable Long id, @RequestBody Achievement achievement) {
        return Result.success(adminService.updateAchievement(id, achievement));
    }

    @DeleteMapping("/achievements/{id}")
    @Operation(summary = "Delete achievement")
    public Result<Void> deleteAchievement(@PathVariable Long id) {
        adminService.deleteAchievement(id);
        return Result.success();
    }

    // ---- Badge Management ----

    @GetMapping("/badges")
    @Operation(summary = "List all badges")
    public Result<IPage<Badge>> listAllBadges(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        return Result.success(adminService.listAllBadges(page, size));
    }

    @PostMapping("/badges")
    @Operation(summary = "Create badge")
    public Result<Badge> createBadge(@RequestBody Badge badge) {
        return Result.success(adminService.createBadge(badge));
    }

    @PutMapping("/badges/{id}")
    @Operation(summary = "Update badge")
    public Result<Badge> updateBadge(@PathVariable Long id, @RequestBody Badge badge) {
        return Result.success(adminService.updateBadge(id, badge));
    }

    @DeleteMapping("/badges/{id}")
    @Operation(summary = "Delete badge")
    public Result<Void> deleteBadge(@PathVariable Long id) {
        adminService.deleteBadge(id);
        return Result.success();
    }

    // ---- Certificate Management ----

    @PostMapping("/certificates/sync")
    @Operation(summary = "Sync certificates for badge earners")
    public Result<Map<String, Object>> syncCertificates(@RequestParam(required = false) Long badgeId) {
        List<Certificate> generated = adminService.syncCertificates(badgeId);
        Map<String, Object> result = new HashMap<>();
        result.put("generatedCount", generated.size());
        result.put("certificates", generated);
        return Result.success(result);
    }

    @GetMapping("/certificates")
    @Operation(summary = "List all certificates")
    public Result<Map<String, Object>> listAllCertificates(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        List<Certificate> list = adminService.listAllCertificates(page, size);
        long total = adminService.countAllCertificates();
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        return Result.success(result);
    }

    @DeleteMapping("/certificates/{id}")
    @Operation(summary = "Delete certificate")
    public Result<Void> deleteCertificate(@PathVariable Long id) {
        adminService.deleteCertificate(id);
        return Result.success();
    }

    @PostMapping("/certificates/regenerate")
    @Operation(summary = "Regenerate all certificates with new template")
    public Result<Map<String, Object>> regenerateAllCertificates() {
        List<Certificate> generated = adminService.regenerateAllCertificates();
        Map<String, Object> result = new HashMap<>();
        result.put("generatedCount", generated.size());
        result.put("certificates", generated);
        return Result.success(result);
    }
}
