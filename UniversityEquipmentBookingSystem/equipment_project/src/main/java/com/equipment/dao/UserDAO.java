package com.equipment.dao;

import com.equipment.model.User;
import com.equipment.model.User.Role;
import com.equipment.persistence.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO - Data Access Object
 * Handles DB operations for User entity (authentication, registration, lookup).
 * GRASP: Information Expert - knows how to persist/retrieve User objects.
 * Pattern: DAO - separates persistence logic from business logic.
 */
public class UserDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------
    // Authentication: Login
    // -------------------------------------------------------
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM dbo.users WHERE username = ? AND password = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // -------------------------------------------------------
    // CRUD: Register / Add User
    // -------------------------------------------------------
    public boolean registerUser(User user) throws SQLException {
        // Check username uniqueness
        if (usernameExists(user.getUsername())) return false;
        String sql = "INSERT INTO dbo.users (username, password, full_name, email, role) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getRole().name());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) user.setUserId(keys.getInt(1));
            }
            return rows > 0;
        }
    }

    // -------------------------------------------------------
    // Lookup: Find user by ID
    // -------------------------------------------------------
    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT * FROM dbo.users WHERE user_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // -------------------------------------------------------
    // Lookup: Find user by username
    // -------------------------------------------------------
    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM dbo.users WHERE username = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // -------------------------------------------------------
    // Get all users by role (e.g., get all TECHNICIAN users for assignment)
    // -------------------------------------------------------
    public List<User> getUsersByRole(Role role) throws SQLException {
        String sql = "SELECT * FROM dbo.users WHERE role = ? ORDER BY full_name";
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // -------------------------------------------------------
    // Get all users
    // -------------------------------------------------------
    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM dbo.users ORDER BY role, full_name";
        List<User> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // -------------------------------------------------------
    // CRUD: Update user
    // -------------------------------------------------------
    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE dbo.users SET full_name=?, email=?, role=? WHERE user_id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole().name());
            ps.setInt(4, user.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------------------------------------
    // CRUD: Update password
    // -------------------------------------------------------
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE dbo.users SET password=? WHERE user_id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------
    private boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.users WHERE username=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setRole(Role.valueOf(rs.getString("role")));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) u.setCreatedAt(ca.toLocalDateTime());
        return u;
    }
}
