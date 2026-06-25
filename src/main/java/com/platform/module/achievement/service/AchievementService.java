package com.platform.module.achievement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.achievement.entity.Achievement;

import java.util.List;
import java.util.Map;

public interface AchievementService extends IService<Achievement> {

    Achievement submit(Achievement achievement);

    void setFeatured(Long id);

    boolean toggleLike(Long id, Long userId);

    boolean toggleCollect(Long id, Long userId);

    Map<String, Object> getStatistics();

    Achievement createSharePost(Achievement achievement);

    List<Achievement> getSharePosts();
}
