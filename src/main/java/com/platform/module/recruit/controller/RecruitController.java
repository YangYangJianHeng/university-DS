package com.platform.module.recruit.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.recruit.dto.ConvertToProjectDTO;
import com.platform.module.recruit.dto.CreateRecruitDTO;
import com.platform.module.recruit.dto.UpdateRecruitDTO;
import com.platform.module.recruit.service.RecruitService;
import com.platform.module.recruit.vo.RecruitApplicationVO;
import com.platform.module.recruit.vo.RecruitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    public Result<List<RecruitApplicationVO>> getApplications(@PathVariable Long id) {
        List<RecruitApplicationVO> applications = recruitService.getApplications(id);
        return Result.success(applications);
    }

    /**
     * Approve or reject an application (publisher only).
     */
    @PutMapping("/applications/{applicationId}")
    public Result<String> approveApplication(@PathVariable Long applicationId,
                                             @RequestParam boolean approved) {
        Long publisherId = UserContext.getCurrentUserId();
        recruitService.approveApplication(applicationId, publisherId, approved);
        return Result.success(approved ? "已通过申请" : "已拒绝申请");
    }

    /**
     * 将招募转为项目：创建项目、团队，并可继续招募和批量创建任务
     * (publisher or admin only)
     */
    @PostMapping("/{id}/convert-to-project")
    public Result<Map<String, Object>> convertToProject(@PathVariable Long id,
                                                         @Valid @RequestBody ConvertToProjectDTO dto) {
        Long projectId = recruitService.convertToProject(id, dto);
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("message", "招募已成功转为项目");
        return Result.success(result);
    }

    /**
     * 切换继续招募状态（转为项目后，发布者/管理员可切换）
     * @param continueRecruit 0=停止招募，1=继续招募
     */
    @PutMapping("/{id}/continue-recruit")
    public Result<Map<String, Object>> toggleContinueRecruit(@PathVariable Long id,
                                                              @RequestParam Integer continueRecruit) {
        recruitService.toggleContinueRecruit(id, continueRecruit);
        Map<String, Object> result = new HashMap<>();
        result.put("continueRecruit", continueRecruit);
        result.put("message", continueRecruit == 1 ? "已开启继续招募" : "已停止招募");
        return Result.success(result);
    }
}
