package com.equipment.ui;

import com.equipment.model.User;

/**
 * SessionManager - Holds the currently logged-in user.
 *
 * Controllers read SessionManager.getCurrentUser() to know
 * which user is logged in and what role they have.
 *
 * No changes needed in backend code.
 */
public class SessionManager {

    private static User currentUser;

    private SessionManager() {}

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clearSession() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isTeacher() {
        return currentUser != null && currentUser.getRole() == User.Role.TEACHER;
    }

    public static boolean isLabManager() {
        return currentUser != null && currentUser.getRole() == User.Role.LAB_MANAGER;
    }

    public static boolean isTechnician() {
        return currentUser != null && currentUser.getRole() == User.Role.TECHNICIAN;
    }
}
