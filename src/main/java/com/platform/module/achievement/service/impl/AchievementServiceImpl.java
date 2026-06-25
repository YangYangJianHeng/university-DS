package com.platform.module.achievement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.common.util.UserContext;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.achievement.mapper.AchievementMapper;
import com.platform.module.achievement.service.AchievementService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AchievementServiceImpl extends ServiceImpl<AchievementMapper, Achievement> implements AchievementService {

    // 内存存储用户点赞/收藏记录（生产环境应使用数据库表）
    private final Set<String> userLikes = ConcurrentHashMap.newKeySet();
    private final Set<String> userCollects = ConcurrentHashMap.newKeySet();

    @Override
    public Achievement submit(Achievement achievement) {
        // 输入校验
        if (achievement.getTitle() == null || achievement.getTitle().trim().isEmpty()) {
            throw BusinessException.badRequest("成果标题不能为空");
        }
        // 设置提交者
        Long userId = UserContext.getCurrentUserId();
        if (userId != null) {
            achievement.setSubmitterId(userId);
        }
        achievement.setIsFeatured(0);
        achievement.setIsPublic(1);
        achievement.setViewCount(0);
        achievement.setLikeCount(0);
        achievement.setCreatedAt(LocalDateTime.now());
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
        String key = userId + ":" + id;
        if (userLikes.contains(key)) {
            // 取消点赞
            userLikes.remove(key);
            achievement.setLikeCount(Math.max(0, achievement.getLikeCount() - 1));
            updateById(achievement);
            return false;
        } else {
            // 点赞
            userLikes.add(key);
            achievement.setLikeCount(achievement.getLikeCount() + 1);
            updateById(achievement);
            return true;
        }
    }

    @Override
    public boolean toggleCollect(Long id, Long userId) {
        Achievement achievement = getById(id);
        if (achievement == null) {
            throw BusinessException.notFound("Achievement not found");
        }
        String key = userId + ":" + id;
        if (userCollects.contains(key)) {
            // 取消收藏
            userCollects.remove(key);
            return false;
        } else {
            // 收藏
            userCollects.add(key);
            // 同时增加浏览量
            if (achievement.getViewCount() == null) {
                achievement.setViewCount(0);
            }
            achievement.setViewCount(achievement.getViewCount() + 1);
            updateById(achievement);
            return true;
        }
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

        // Group by type using SQL-like aggregation
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
        if (achievement.getTitle() == null || achievement.getTitle().trim().isEmpty()) {
            throw BusinessException.badRequest("分享标题不能为空");
        }
        achievement.setType("SHARE");
        achievement.setIsPublic(1);
        achievement.setViewCount(0);
        achievement.setLikeCount(0);
        achievement.setCreatedAt(LocalDateTime.now());
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
