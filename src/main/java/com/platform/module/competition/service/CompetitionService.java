package com.platform.module.competition.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.competition.entity.Competition;

import java.util.List;
import java.util.Map;

public interface CompetitionService extends IService<Competition> {

    IPage<Competition> listByPage(String level, String category, String status, int page, int size);

    List<Competition> getRecommendByMajor(String major);

    boolean toggleCollect(Long competitionId, Long userId);

    List<Map<String, Object>> getCalendarEvents();
}
