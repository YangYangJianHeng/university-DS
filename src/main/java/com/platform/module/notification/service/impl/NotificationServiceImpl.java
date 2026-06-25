package com.platform.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.module.notification.entity.Notification;
import com.platform.module.notification.mapper.NotificationMapper;
import com.platform.module.notification.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Override
    public List<Notification> getMyNotifications(Long userId) {
        return list(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt));
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        Notification notification = getById(id);
        if (notification == null) {
            throw BusinessException.notFound("Notification not found");
        }
        if (!notification.getUserId().equals(userId)) {
            throw BusinessException.forbidden("Cannot mark other user's notification as read");
        }
        notification.setIsRead(1);
        updateById(notification);
    }

    @Override
    public void markAllRead(Long userId) {
        List<Notification> unreadList = list(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        for (Notification notification : unreadList) {
            notification.setIsRead(1);
        }
        if (!unreadList.isEmpty()) {
            updateBatchById(unreadList);
        }
    }

    @Override
    public long getUnreadCount(Long userId) {
        return count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    @Override
    public Notification createNotification(Long userId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type != null ? type : "SYSTEM");
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        save(notification);
        return notification;
    }
}
