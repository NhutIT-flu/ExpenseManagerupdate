package com.expensemanager.util;

public class UserSession {
    private static int currentUserId;
    private static String currentUserRole;
    private static String currentUsername;

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUserId(int userId) {
        currentUserId = userId;
    }

    public static String getCurrentUserRole() {
        return currentUserRole;
    }

    public static void setCurrentUserRole(String role) {
        currentUserRole = role;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static void setCurrentUsername(String username) {
        currentUsername = username;
    }

    public static void clearSession() {
        currentUserId = 0;
        currentUserRole = null;
        currentUsername = null;
    }
} 