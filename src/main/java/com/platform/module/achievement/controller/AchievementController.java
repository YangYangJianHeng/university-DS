package com.platform.module.achievement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.achievement.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Achievement Management")
@RestController
@RequestMapping("/api/achievement")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @PostMapping
    @Operation(summary = "Submit achievement")
    public Result<Achievement> submit(@RequestBody Achievement achievement) {
        return Result.success(achievementService.submit(achievement));
    }

    @GetMapping("/list")
    @Operation(summary = "List achievements with filters and pagination")
    public Result<IPage<Achievement>> list(@RequestParam(required = false) String type,
                                           @RequestParam(required = false) String awardLevel,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        LambdaQueryWrapper<Achievement> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Achievement::getType, type);
        }
        if (awardLevel != null && !awardLevel.isEmpty()) {
            wrapper.eq(Achievement::getAwardLevel, awardLevel);
        }
        wrapper.orderByDesc(Achievement::getCreatedAt);
        return Result.success(achievementService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get achievement detail")
    public Result<Achievement> detail(@PathVariable Long id) {
        Achievement achievement = achievementService.getById(id);
        if (achievement == null) {
            return Result.notFound("Achievement not found");
        }
        // Increment view count
        achievement.setViewCount(achievement.getViewCount() != null ? achievement.getViewCount() + 1 : 1);
        achievementService.updateById(achievement);
        return Result.success(achievement);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update achievement")
    public Result<Achievement> update(@PathVariable Long id, @RequestBody Achievement achievement) {
        Achievement existing = achievementService.getById(id);
        if (existing == null) {
            return Result.notFound("Achievement not found");
        }
        achievement.setId(id);
        achievementService.updateById(achievement);
        return Result.success(achievementService.getById(id));
    }

    @PutMapping("/{id}/featured")
    @Operation(summary = "Toggle featured (admin)")
    public Result<Void> setFeatured(@PathVariable Long id) {
        achievementService.setFeatured(id);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "Toggle like")
    public Result<Boolean> like(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(achievementService.toggleLike(id, userId));
    }

    @PostMapping("/{id}/collect")
    @Operation(summary = "Toggle collect")
    public Result<Boolean> collect(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(achievementService.toggleCollect(id, userId));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(achievementService.getStatistics());
    }

    @PostMapping("/share")
    @Operation(summary = "Create share post")
    public Result<Achievement> createShare(@RequestBody Achievement achievement) {
        return Result.success(achievementService.createSharePost(achievement));
    }

    @GetMapping("/share/list")
    @Operation(summary = "List share posts")
    public Result<List<Achievement>> shareList() {
        return Result.success(achievementService.getSharePosts());
    }
}
