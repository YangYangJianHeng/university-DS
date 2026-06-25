package com.platform.module.recruit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.module.recruit.dto.CreateRecruitDTO;
import com.platform.module.recruit.dto.UpdateRecruitDTO;
import com.platform.module.recruit.entity.Application;
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

    List<RecruitVO> getMyRecruits(Long userId);

    List<Application> getApplications(Long recruitId);
}
