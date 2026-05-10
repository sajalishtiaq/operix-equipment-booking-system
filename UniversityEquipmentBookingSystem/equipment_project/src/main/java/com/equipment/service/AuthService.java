package com.equipment.service;

import com.equipment.dao.AuditLogDAO;
import com.equipment.dao.UserDAO;
import com.equipment.model.User;
import com.equipment.util.PasswordUtil;
import com.equipment.util.ValidationUtil;

import java.sql.SQLException;
import java.util.List;

public class AuthService {

    private final UserDAO userDAO;
    private final AuditLogDAO auditLogDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.auditLogDAO = new AuditLogDAO();
    }

    // -------------------------------------------------------
    // LOGIN (FIXED VERSION)
    // -------------------------------------------------------
    public User login(String username, String password) throws SQLException {

        ValidationUtil.requireNonBlank(username, "Username");
        ValidationUtil.requireNonBlank(password, "Password");

        // ✔ DO NOT mix hashed + plain logic
        // Decide ONE strategy:

        // OPTION 1: IF DB STORES PLAIN PASSWORDS (for your project now)
        User user = userDAO.login(username, password);

        // OPTION 2 (recommended later): hashed password only
        // String hashed = PasswordUtil.hash(password);
        // User user = userDAO.login(username, hashed);

        if (user != null) {
            auditLogDAO.logAction(
                    user.getUserId(),
                    "LOGIN",
                    "USER",
                    user.getUserId(),
                    "User '" + username + "' logged in successfully"
            );
        }

        return user;
    }

    // -------------------------------------------------------
    // REGISTER USER
    // -------------------------------------------------------
    public boolean registerUser(String username, String password, String fullName,
                                String email, User.Role role) throws SQLException {

        ValidationUtil.requireNonBlank(username, "Username");
        ValidationUtil.requireNonBlank(password, "Password");
        ValidationUtil.requireNonBlank(fullName, "Full name");
        ValidationUtil.requireNonBlank(email, "Email");
        ValidationUtil.requireValidEmail(email);

        // ✔ keep consistent strategy (PLAIN or HASHED — choose one)
        User user = new User(username, password, fullName, email, role);

        boolean created = userDAO.registerUser(user);

        if (created) {
            auditLogDAO.logAction(
                    user.getUserId(),
                    "REGISTER",
                    "USER",
                    user.getUserId(),
                    "New user registered: " + username
            );
        }

        return created;
    }

    // -------------------------------------------------------
    // GET TECHNICIANS
    // -------------------------------------------------------
    public List<User> getAllTechnicians() throws SQLException {
        return userDAO.getUsersByRole(User.Role.TECHNICIAN);
    }

    // -------------------------------------------------------
    // GET ALL USERS
    // -------------------------------------------------------
    public List<User> getAllUsers() throws SQLException {
        return userDAO.getAllUsers();
    }

    // -------------------------------------------------------
    // CHANGE PASSWORD
    // -------------------------------------------------------
    public boolean changePassword(int userId, String oldPassword, String newPassword) throws SQLException {

        ValidationUtil.requireNonBlank(newPassword, "New password");

        User user = userDAO.getUserById(userId);
        if (user == null) return false;

        if (!user.getPassword().equals(oldPassword)) {
            throw new IllegalArgumentException("Old password is incorrect.");
        }

        boolean updated = userDAO.updatePassword(userId, newPassword);

        if (updated) {
            auditLogDAO.logAction(userId, "CHANGE_PASSWORD", "USER", userId, "Password changed.");
        }

        return updated;
    }
}