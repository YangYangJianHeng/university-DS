package com.platform.module.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.notification.entity.Notification;

import java.util.List;

public interface NotificationService extends IService<Notification> {

    List<Notification> getMyNotifications(Long userId);

    void markAsRead(Long id, Long userId);

    void markAllRead(Long userId);

    long getUnreadCount(Long userId);

    /**
     * 创建通知
     */
    Notification createNotification(Long userId, String title, String content, String type);
}
