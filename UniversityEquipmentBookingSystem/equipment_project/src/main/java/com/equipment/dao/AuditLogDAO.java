package com.equipment.dao;

import com.equipment.model.AuditLog;
import com.equipment.persistence.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditLogDAO - Data Access Object
 * Persists and retrieves audit log entries.
 * GRASP: Information Expert - knows how to persist AuditLog objects.
 */
public class AuditLogDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------
    // Log an action
    // -------------------------------------------------------
    public boolean log(AuditLog entry) throws SQLException {
        
        String sql = "INSERT INTO dbo.audit_log (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entry.getUserId());
            ps.setString(2, entry.getAction());
            ps.setString(3, entry.getEntityType());
            ps.setInt(4, entry.getEntityId());
            ps.setString(5, entry.getDetails());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) entry.setLogId(keys.getInt(1));
            }
            return rows > 0;
        }
    }

    // -------------------------------------------------------
    // Convenience factory: log without constructing manually
    // -------------------------------------------------------
    public void logAction(int userId, String action, String entityType, int entityId, String details)
            throws SQLException {
        log(new AuditLog(userId, action, entityType, entityId, details));
    }

    // -------------------------------------------------------
    // UC04 (Lab Manager): View Reports and Analytics - get all logs
    // -------------------------------------------------------
    public List<AuditLog> getAllLogs() throws SQLException {
        String sql = "SELECT al.*, u.username FROM dbo.audit_log al " +
                     "LEFT JOIN dbo.users u ON al.user_id = u.user_id " +
                     "ORDER BY al.logged_at DESC";
        List<AuditLog> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // -------------------------------------------------------
    // Get logs for a specific entity (e.g., equipment history)
    // -------------------------------------------------------
    public List<AuditLog> getLogsByEntity(String entityType, int entityId) throws SQLException {
        String sql = "SELECT al.*, u.username FROM dno.audit_log al " +
                     "LEFT JOIN dbo.users u ON al.user_id = u.user_id " +
                     "WHERE al.entity_type = ? AND al.entity_id = ? ORDER BY al.logged_at DESC";
        List<AuditLog> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, entityId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // -------------------------------------------------------
    // Get logs for a specific user
    // -------------------------------------------------------
    public List<AuditLog> getLogsByUser(int userId) throws SQLException {
        String sql = "SELECT al.*, u.username FROM dbo.audit_log al " +
                     "LEFT JOIN dbo.users u ON al.user_id = u.user_id " +
                     "WHERE al.user_id = ? ORDER BY al.logged_at DESC";
        List<AuditLog> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setLogId(rs.getInt("log_id"));
        log.setUserId(rs.getInt("user_id"));
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));
        log.setEntityId(rs.getInt("entity_id"));
        log.setDetails(rs.getString("details"));
        Timestamp la = rs.getTimestamp("logged_at");
        if (la != null) log.setLoggedAt(la.toLocalDateTime());
        log.setUsername(rs.getString("username"));
        return log;
    }
}
