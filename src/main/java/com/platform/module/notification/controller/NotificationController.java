package com.platform.module.notification.controller;

import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.notification.entity.Notification;
import com.platform.module.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notification Management")
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    @Operation(summary = "Get my notifications")
    public Result<List<Notification>> list() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(notificationService.getMyNotifications(userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public Result<Long> unreadCount() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(notificationService.getUnreadCount(userId));
    }
}
