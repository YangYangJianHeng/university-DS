package com.platform.module.recruit.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.recruit.dto.CreateRecruitDTO;
import com.platform.module.recruit.dto.UpdateRecruitDTO;
import com.platform.module.recruit.entity.Application;
import com.platform.module.recruit.service.RecruitService;
import com.platform.module.recruit.vo.RecruitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recruit")
@RequiredArgsConstructor
public class RecruitController {

    private final RecruitService recruitService;

    /**
     * Create a recruit posting.
     */
    @PostMapping
    public Result<RecruitVO> createRecruit(@Valid @RequestBody CreateRecruitDTO dto) {
        RecruitVO vo = recruitService.createRecruit(dto);
        return Result.success(vo);
    }

    /**
     * Get recruit detail by id (increments view count).
     */
    @GetMapping("/{id}")
    public Result<RecruitVO> getRecruitById(@PathVariable Long id) {
        RecruitVO vo = recruitService.getRecruitById(id);
        return Result.success(vo);
    }

    /**
     * Paginated list with optional projectType and status filters.
     */
    @GetMapping("/list")
    public Result<IPage<RecruitVO>> listRecruits(
            @RequestParam(required = false) String projectType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<RecruitVO> pageResult = recruitService.listRecruits(projectType, status, page, size);
        return Result.success(pageResult);
    }

    /**
     * Update a recruit posting (publisher only).
     */
    @PutMapping("/{id}")
    public Result<RecruitVO> updateRecruit(@PathVariable Long id,
                                           @Valid @RequestBody UpdateRecruitDTO dto) {
        RecruitVO vo = recruitService.updateRecruit(id, dto);
        return Result.success(vo);
    }

    /**
     * Close (soft delete) a recruit posting (publisher only).
     */
    @DeleteMapping("/{id}")
    public Result<Void> closeRecruit(@PathVariable Long id) {
        recruitService.closeRecruit(id);
        return Result.success();
    }

    /**
     * Find users whose skills match the recruit's required skills
     * using cosine similarity.
     */
    @GetMapping("/match")
    public Result<List<Map<String, Object>>> findMatchingUsers(
            @RequestParam Long recruitId) {
        List<Map<String, Object>> matches = recruitService.findMatchingUsers(recruitId);
        return Result.success(matches);
    }

    /**
     * Apply to join a recruit.
     */
    @PostMapping("/{id}/apply")
    public Result<String> applyToRecruit(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        recruitService.applyToRecruit(id, userId);
        return Result.success("申请已提交");
    }

    /**
     * Get recruits published by the current user.
     */
    @GetMapping("/my")
    public Result<List<RecruitVO>> getMyRecruits() {
        Long userId = UserContext.getCurrentUserId();
        List<RecruitVO> recruits = recruitService.getMyRecruits(userId);
        return Result.success(recruits);
    }

    /**
     * Get applications for a specific recruit (publisher only).
     */
    @GetMapping("/{id}/applications")
    public Result<List<Application>> getApplications(@PathVariable Long id) {
        List<Application> applications = recruitService.getApplications(id);
        return Result.success(applications);
    }
}
