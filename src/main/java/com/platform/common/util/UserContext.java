package com.platform.common.util;

public class UserContext {
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserRole = new ThreadLocal<>();

    public static void setCurrentUserId(Long userId) {
        currentUserId.set(userId);
    }

    public static Long getCurrentUserId() {
        return currentUserId.get();
    }

    public static void setCurrentUserRole(String role) {
        currentUserRole.set(role);
    }

    public static String getCurrentUserRole() {
        return currentUserRole.get();
    }

    public static void clear() {
        currentUserId.remove();
        currentUserRole.remove();
    }
}
