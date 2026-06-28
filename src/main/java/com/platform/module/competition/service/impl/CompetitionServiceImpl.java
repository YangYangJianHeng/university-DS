package com.platform.module.competition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.module.competition.entity.Competition;
import com.platform.module.competition.mapper.CompetitionMapper;
import com.platform.module.competition.service.CompetitionService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CompetitionServiceImpl extends ServiceImpl<CompetitionMapper, Competition> implements CompetitionService {

    // 内存存储用户收藏记录（生产环境应使用数据库表）
    private final Set<String> userCollects = ConcurrentHashMap.newKeySet();

    @Override
    public IPage<Competition> listByPage(String level, String category, String status, int page, int size) {
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();
        if (level != null && !level.isEmpty()) {
            wrapper.eq(Competition::getLevel, level);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Competition::getCategory, category);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Competition::getStatus, status);
        }
        wrapper.orderByDesc(Competition::getRegisterStart);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Cacheable(value = "competition", key = "'recommend:' + #major")
    public List<Competition> getRecommendByMajor(String major) {
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Competition::getStatus, "REGISTERING")
                .and(w -> w.like(Competition::getCategory, major)
                        .or()
                        .like(Competition::getName, major))
                .orderByDesc(Competition::getViewCount);
        return page(new Page<>(1, 10), wrapper).getRecords();
    }

    @Override
    @CacheEvict(value = "competition", allEntries = true)
    public boolean toggleCollect(Long competitionId, Long userId) {
        Competition competition = getById(competitionId);
        if (competition == null) {
            throw BusinessException.notFound("Competition not found");
        }
        if (competition.getCollectCount() == null) {
            competition.setCollectCount(0);
        }
        String key = userId + ":" + competitionId;
        if (userCollects.contains(key)) {
            // 取消收藏
            userCollects.remove(key);
            competition.setCollectCount(Math.max(0, competition.getCollectCount() - 1));
            updateById(competition);
            return false;
        } else {
            // 收藏
            userCollects.add(key);
            competition.setCollectCount(competition.getCollectCount() + 1);
            updateById(competition);
            return true;
        }
    }

    @Override
    @Cacheable(value = "competition", key = "'calendar'")
    public List<Map<String, Object>> getCalendarEvents() {
        List<Competition> list = list(new LambdaQueryWrapper<Competition>()
                .eq(Competition::getStatus, "REGISTERING")
                .or()
                .eq(Competition::getStatus, "ONGOING"));
        return list.stream().map(c -> {
            Map<String, Object> event = new HashMap<>();
            event.put("id", c.getId());
            event.put("title", c.getName());
            event.put("start", c.getRegisterStart() != null ? c.getRegisterStart().toString() : null);
            event.put("end", c.getRegisterEnd() != null ? c.getRegisterEnd().toString() : null);
            event.put("type", "competition");
            return event;
        }).collect(Collectors.toList());
    }
}
