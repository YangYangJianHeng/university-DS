package com.platform.module.competition.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.competition.entity.Competition;
import com.platform.module.competition.mapper.CompetitionMapper;
import com.platform.module.competition.service.CompetitionService;
import com.platform.module.user.entity.User;
import com.platform.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Competition Management")
@RestController
@RequestMapping("/api/competition")
public class CompetitionController {

    private final CompetitionService competitionService;
    private final CompetitionMapper competitionMapper;
    private final UserMapper userMapper;

    public CompetitionController(CompetitionService competitionService,
                                 CompetitionMapper competitionMapper,
                                 UserMapper userMapper) {
        this.competitionService = competitionService;
        this.competitionMapper = competitionMapper;
        this.userMapper = userMapper;
    }

    @PostMapping
    @Operation(summary = "Create competition (admin)")
    public Result<Competition> create(@RequestBody Competition competition) {
        requireAdmin();
        competitionService.save(competition);
        return Result.success(competition);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update competition (admin)")
    public Result<Competition> update(@PathVariable Long id, @RequestBody Competition competition) {
        requireAdmin();
        Competition existing = competitionService.getById(id);
        if (existing == null) {
            return Result.notFound("Competition not found");
        }
        if (competition.getName() != null) existing.setName(competition.getName());
        if (competition.getLevel() != null) existing.setLevel(competition.getLevel());
        if (competition.getCategory() != null) existing.setCategory(competition.getCategory());
        if (competition.getOrganizer() != null) existing.setOrganizer(competition.getOrganizer());
        if (competition.getDescription() != null) existing.setDescription(competition.getDescription());
        if (competition.getStatus() != null) existing.setStatus(competition.getStatus());
        if (competition.getRegisterStart() != null) existing.setRegisterStart(competition.getRegisterStart());
        if (competition.getRegisterEnd() != null) existing.setRegisterEnd(competition.getRegisterEnd());
        if (competition.getEventStart() != null) existing.setEventStart(competition.getEventStart());
        if (competition.getEventEnd() != null) existing.setEventEnd(competition.getEventEnd());
        if (competition.getOfficialUrl() != null) existing.setOfficialUrl(competition.getOfficialUrl());
        competitionService.updateById(existing);
        return Result.success(existing);
    }

    @GetMapping("/list")
    @Operation(summary = "List competitions with filters")
    public Result<IPage<Competition>> list(@RequestParam(required = false) String level,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.success(competitionService.listByPage(level, category, status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get competition detail")
    public Result<Competition> detail(@PathVariable Long id) {
        Competition competition = competitionService.getById(id);
        if (competition == null) {
            return Result.notFound("Competition not found");
        }
        // 原子操作更新浏览次数（避免并发覆盖问题）
        competitionMapper.incrementViewCount(id);
        competition.setViewCount(competition.getViewCount() != null ? competition.getViewCount() + 1 : 1);
        return Result.success(competition);
    }

    @GetMapping("/recommend")
    @Operation(summary = "Recommend competitions by user major")
    public Result<List<Competition>> recommend() {
        Long userId = UserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        String major = user != null ? user.getMajor() : "";
        return Result.success(competitionService.getRecommendByMajor(major));
    }

    @PostMapping("/{id}/collect")
    @Operation(summary = "Toggle collect competition")
    public Result<Boolean> toggleCollect(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(competitionService.toggleCollect(id, userId));
    }

    @GetMapping("/calendar")
    @Operation(summary = "Get competition calendar events")
    public Result<List<Map<String, Object>>> calendar() {
        return Result.success(competitionService.getCalendarEvents());
    }

    /** 检查当前用户是否为管理员 */
    private void requireAdmin() {
        String role = UserContext.getCurrentUserRole();
        if (role == null || !"ADMIN".equals(role)) {
            throw BusinessException.forbidden("需要管理员权限");
        }
    }
}
