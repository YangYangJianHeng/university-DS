package com.platform.module.achievement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.achievement.mapper.AchievementMapper;
import com.platform.module.achievement.service.AchievementService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AchievementServiceImpl extends ServiceImpl<AchievementMapper, Achievement> implements AchievementService {

    @Override
    public Achievement submit(Achievement achievement) {
        achievement.setIsFeatured(0);
        achievement.setViewCount(0);
        achievement.setLikeCount(0);
        save(achievement);
        return achievement;
    }

    @Override
    public void setFeatured(Long id) {
        Achievement achievement = getById(id);
        if (achievement == null) {
            throw BusinessException.notFound("Achievement not found");
        }
        achievement.setIsFeatured(achievement.getIsFeatured() != null && achievement.getIsFeatured() == 1 ? 0 : 1);
        updateById(achievement);
    }

    @Override
    public boolean toggleLike(Long id, Long userId) {
        Achievement achievement = getById(id);
        if (achievement == null) {
            throw BusinessException.notFound("Achievement not found");
        }
        if (achievement.getLikeCount() == null) {
            achievement.setLikeCount(0);
        }
        // In a real app, check user_like table to determine toggle direction.
        // For simplicity, just increment.
        achievement.setLikeCount(achievement.getLikeCount() + 1);
        updateById(achievement);
        return true;
    }

    @Override
    public boolean toggleCollect(Long id, Long userId) {
        // For simplicity, just increment viewCount as a proxy for collect action.
        Achievement achievement = getById(id);
        if (achievement == null) {
            throw BusinessException.notFound("Achievement not found");
        }
        // In a real app, this would use a user_collection table.
        return true;
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        long total = count();
        long featured = count(new LambdaQueryWrapper<Achievement>().eq(Achievement::getIsFeatured, 1));
        long publicCount = count(new LambdaQueryWrapper<Achievement>().eq(Achievement::getIsPublic, 1));

        stats.put("total", total);
        stats.put("featured", featured);
        stats.put("public", publicCount);

        // Group by type
        List<Achievement> all = list();
        Map<String, Long> typeCount = new HashMap<>();
        for (Achievement a : all) {
            String type = a.getType() != null ? a.getType() : "OTHER";
            typeCount.merge(type, 1L, Long::sum);
        }
        stats.put("byType", typeCount);

        return stats;
    }

    @Override
    public Achievement createSharePost(Achievement achievement) {
        achievement.setType("SHARE");
        achievement.setIsPublic(1);
        achievement.setViewCount(0);
        achievement.setLikeCount(0);
        save(achievement);
        return achievement;
    }

    @Override
    public List<Achievement> getSharePosts() {
        return list(new LambdaQueryWrapper<Achievement>()
                .eq(Achievement::getType, "SHARE")
                .orderByDesc(Achievement::getCreatedAt));
    }
}
