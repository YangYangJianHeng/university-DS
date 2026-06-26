package com.platform.module.badge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.badge.entity.Badge;

import java.util.List;
import java.util.Map;

public interface BadgeService extends IService<Badge> {

    /**
     * 查询用户已解锁的徽章列表（含解锁状态）
     */
    List<Map<String, Object>> getUserBadges(Long userId);

    /**
     * 管理员创建徽章
     */
    Badge createBadge(Badge badge);

    /**
     * 管理员更新徽章
     */
    Badge updateBadge(Long id, Badge badge);
}
