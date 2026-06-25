package com.platform.module.teacher.controller;

import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.teacher.entity.Teacher;
import com.platform.module.teacher.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Teacher Management")
@RestController
@RequestMapping("/api")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/teacher/register")
    @Operation(summary = "Register as teacher")
    public Result<Teacher> register(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getCurrentUserId();
        String department = (String) request.get("department");
        String title = (String) request.get("title");
        String researchFields = (String) request.get("researchFields");
        Integer maxTeams = request.get("maxTeams") != null ? Integer.valueOf(request.get("maxTeams").toString()) : null;
        return Result.success(teacherService.register(userId, department, title, researchFields, maxTeams));
    }

    @GetMapping("/teacher/list")
    @Operation(summary = "List all teachers")
    public Result<List<Teacher>> list() {
        return Result.success(teacherService.list());
    }

    @GetMapping("/teacher/{id}")
    @Operation(summary = "Get teacher detail")
    public Result<Teacher> detail(@PathVariable Long id) {
        Teacher teacher = teacherService.getById(id);
        if (teacher == null) {
            return Result.notFound("Teacher not found");
        }
        return Result.success(teacher);
    }

    @PutMapping("/teacher/{id}")
    @Operation(summary = "Update teacher")
    public Result<Teacher> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String department = (String) request.get("department");
        String title = (String) request.get("title");
        String researchFields = (String) request.get("researchFields");
        Integer maxTeams = request.get("maxTeams") != null ? Integer.valueOf(request.get("maxTeams").toString()) : null;
        return Result.success(teacherService.updateTeacher(id, department, title, researchFields, maxTeams));
    }

    @PostMapping("/team/{teamId}/teacher/invite")
    @Operation(summary = "Invite teacher to team")
    public Result<Void> invite(@PathVariable Long teamId, @RequestBody Map<String, Object> request) {
        Long teacherId = Long.valueOf(request.get("teacherId").toString());
        teacherService.inviteTeacher(teamId, teacherId);
        return Result.success();
    }

    @PutMapping("/teacher/invite/{id}/approve")
    @Operation(summary = "Approve teacher invitation")
    public Result<Void> approve(@PathVariable Long id) {
        teacherService.approveInvitation(id);
        return Result.success();
    }

    @GetMapping("/teacher/my-teams")
    @Operation(summary = "Get my guided teams")
    public Result<List<Map<String, Object>>> myTeams() {
        Long userId = UserContext.getCurrentUserId();
        // Find teacher by userId
        Teacher teacher = teacherService.lambdaQuery().eq(Teacher::getUserId, userId).one();
        if (teacher == null) {
            return Result.badRequest("Teacher profile not found");
        }
        return Result.success(teacherService.getMyTeams(teacher.getId()));
    }

    @PostMapping("/teacher/guidance-log")
    @Operation(summary = "Log guidance activity")
    public Result<Void> addGuidanceLog(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getCurrentUserId();
        Teacher teacher = teacherService.lambdaQuery().eq(Teacher::getUserId, userId).one();
        if (teacher == null) {
            return Result.badRequest("Teacher profile not found");
        }
        Long teamId = Long.valueOf(request.get("teamId").toString());
        String content = (String) request.get("content");
        teacherService.addGuidanceLog(teacher.getId(), teamId, content);
        return Result.success();
    }

    @GetMapping("/teacher/guidance-logs")
    @Operation(summary = "Get guidance logs")
    public Result<List<Map<String, Object>>> getGuidanceLogs() {
        Long userId = UserContext.getCurrentUserId();
        Teacher teacher = teacherService.lambdaQuery().eq(Teacher::getUserId, userId).one();
        if (teacher == null) {
            return Result.badRequest("Teacher profile not found");
        }
        return Result.success(teacherService.getGuidanceLogs(teacher.getId()));
    }
}
