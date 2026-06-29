package com.platform.module.project.controller;

import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.project.entity.Project;
import com.platform.module.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Project Management")
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create project")
    public Result<Project> create(@RequestBody Map<String, Object> request) {
        Object teamIdObj = request.get("teamId");
        Long teamId = teamIdObj != null ? Long.valueOf(teamIdObj.toString()) : null;
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        return Result.success(projectService.create(teamId, name, description));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project detail")
    public Result<Project> detail(@PathVariable Long id) {
        Project project = projectService.getById(id);
        if (project == null) {
            return Result.notFound("Project not found");
        }
        // 补充团队名称和队长名称
        projectService.enrichProject(project);
        return Result.success(project);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project")
    public Result<Project> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String status = (String) request.get("status");
        return Result.success(projectService.update(id, name, description, status));
    }

    @PostMapping("/{id}/milestone")
    @Operation(summary = "Add milestone")
    public Result<Void> addMilestone(@PathVariable Long id, @RequestBody Map<String, Object> milestone) {
        projectService.addMilestone(id, milestone);
        return Result.success();
    }

    @PutMapping("/{id}/milestone/{milestoneId}")
    @Operation(summary = "Update milestone")
    public Result<Void> updateMilestone(@PathVariable Long id, @PathVariable Long milestoneId,
                                        @RequestBody Map<String, Object> milestone) {
        projectService.updateMilestone(id, milestoneId, milestone);
        return Result.success();
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive project")
    public Result<Void> archive(@PathVariable Long id) {
        projectService.archive(id);
        return Result.success();
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close project (also close associated team)")
    public Result<String> close(@PathVariable Long id) {
        projectService.closeProject(id);
        return Result.success("项目已关闭，关联团队同步关闭");
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reopen a closed project (also reopen associated team)")
    public Result<String> reopen(@PathVariable Long id) {
        projectService.reopenProject(id);
        return Result.success("项目已重新开启，关联团队同步开启");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project (cascade delete team, tasks and files)")
    public Result<String> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return Result.success("项目及关联团队、任务已全部删除");
    }

    @GetMapping("/{id}/gantt")
    @Operation(summary = "Get gantt chart data")
    public Result<List<Map<String, Object>>> gantt(@PathVariable Long id) {
        return Result.success(projectService.getGanttData(id));
    }

    @GetMapping("/{id}/risk")
    @Operation(summary = "Get risk assessment")
    public Result<Map<String, Object>> risk(@PathVariable Long id) {
        return Result.success(projectService.getRiskAssessment(id));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my projects")
    public Result<List<Project>> myProjects() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(projectService.getMyProjects(userId));
    }

    @PostMapping("/{id}/member")
    @Operation(summary = "Add member to project team")
    public Result<Void> addMember(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        Long userId = userIdObj != null ? Long.valueOf(userIdObj.toString()) : null;
        if (userId == null) {
            return Result.badRequest("userId is required");
        }
        projectService.addProjectMember(id, userId);
        return Result.success();
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Get project team members")
    public Result<List<Map<String, Object>>> getMembers(@PathVariable Long id) {
        return Result.success(projectService.getProjectMembers(id));
    }
}
