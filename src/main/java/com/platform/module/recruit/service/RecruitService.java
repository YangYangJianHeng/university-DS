package com.platform.module.recruit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.module.recruit.dto.ConvertToProjectDTO;
import com.platform.module.recruit.dto.CreateRecruitDTO;
import com.platform.module.recruit.dto.UpdateRecruitDTO;
import com.platform.module.recruit.vo.RecruitApplicationVO;
import com.platform.module.recruit.vo.RecruitVO;

import java.util.List;
import java.util.Map;

public interface RecruitService {

    RecruitVO createRecruit(CreateRecruitDTO dto);

    RecruitVO getRecruitById(Long id);

    IPage<RecruitVO> listRecruits(String projectType, String status, int page, int size);

    RecruitVO updateRecruit(Long id, UpdateRecruitDTO dto);

    void closeRecruit(Long id);

    List<Map<String, Object>> findMatchingUsers(Long recruitId);

    void applyToRecruit(Long recruitId, Long userId);

    void approveApplication(Long applicationId, Long publisherId, boolean approved);

    List<RecruitVO> getMyRecruits(Long userId);

    List<RecruitApplicationVO> getApplications(Long recruitId);

    /**
     * 将招募转为项目（创建项目、团队，并可继续招募）
     * @return 返回新创建的 projectId
     */
    Long convertToProject(Long recruitId, ConvertToProjectDTO dto);

    /**
     * 切换继续招募状态（转为项目后，发布者/管理员可切换）
     * @param recruitId 招募ID
     * @param continueRecruit 0=停止招募，1=继续招募
     */
    void toggleContinueRecruit(Long recruitId, Integer continueRecruit);
}
