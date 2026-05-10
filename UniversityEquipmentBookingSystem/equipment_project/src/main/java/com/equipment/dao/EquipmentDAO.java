package com.equipment.dao;

import com.equipment.model.Equipment;
import com.equipment.model.Equipment.Status;
import com.equipment.persistence.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * EquipmentDAO - Data Access Object
 * Handles all DB operations for Equipment entity.
 * Pattern: DAO (Data Access Object) - separates persistence logic from business logic.
 */
public class EquipmentDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------
    // UC1 (Teacher): Search Equipments by name or category
    // -------------------------------------------------------
    public List<Equipment> searchEquipments(String keyword) throws SQLException {
        String sql = "SELECT * FROM equipment WHERE name LIKE ? OR category LIKE ? ORDER BY name";
        List<Equipment> results = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) results.add(mapRow(rs));
        }
        return results;
    }

    // -------------------------------------------------------
    // UC2 (Teacher): Check Availability of a specific equipment
    // -------------------------------------------------------
    public Equipment checkAvailability(int equipmentId) throws SQLException {
        String sql = "SELECT * FROM dbo.equipment WHERE equipment_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // -------------------------------------------------------
    // Lab Manager: Update Equipment Status (UC03 - Manager)
    // -------------------------------------------------------
    public boolean updateStatus(int equipmentId, Status newStatus) throws SQLException {
        String sql = "UPDATE dbo.equipment SET status = ? WHERE equipment_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, equipmentId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------------------------------------
    // Lab Manager: Monitor Equipment Availability (UC02)
    // Returns all equipment with their current status
    // -------------------------------------------------------
    public List<Equipment> getAllEquipments() throws SQLException {
        String sql = "SELECT * FROM dbo.equipment ORDER BY name";
        List<Equipment> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // -------------------------------------------------------
    // Lab Manager: Monitor only AVAILABLE equipment
    // -------------------------------------------------------
    public List<Equipment> getAvailableEquipments() throws SQLException {
        String sql = "SELECT * FROM dbo.equipment WHERE status = 'AVAILABLE' ORDER BY name";
        List<Equipment> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // -------------------------------------------------------
    // CRUD: Add Equipment
    // -------------------------------------------------------
    public boolean addEquipment(Equipment eq) throws SQLException {
        String sql = "INSERT INTO dbo.equipment (name, category, description, status, location) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, eq.getName());
            ps.setString(2, eq.getCategory());
            ps.setString(3, eq.getDescription());
            ps.setString(4, eq.getStatus().name());
            ps.setString(5, eq.getLocation());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) eq.setEquipmentId(keys.getInt(1));
            }
            return rows > 0;
        }
    }

    // -------------------------------------------------------
    // CRUD: Update Equipment
    // -------------------------------------------------------
    public boolean updateEquipment(Equipment eq) throws SQLException {
        String sql = "UPDATE dbo.equipment SET name=?, category=?, description=?, status=?, location=? WHERE equipment_id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, eq.getName());
            ps.setString(2, eq.getCategory());
            ps.setString(3, eq.getDescription());
            ps.setString(4, eq.getStatus().name());
            ps.setString(5, eq.getLocation());
            ps.setInt(6, eq.getEquipmentId());
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------------------------------------
    // CRUD: Delete Equipment
    // -------------------------------------------------------
    public boolean deleteEquipment(int equipmentId) throws SQLException {
        String sql = "DELETE FROM dbo.equipment WHERE equipment_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------------------------------------
    // Internal mapper
    // -------------------------------------------------------
    private Equipment mapRow(ResultSet rs) throws SQLException {
        Equipment eq = new Equipment();
        eq.setEquipmentId(rs.getInt("equipment_id"));
        eq.setName(rs.getString("name"));
        eq.setCategory(rs.getString("category"));
        eq.setDescription(rs.getString("description"));
        eq.setStatus(Status.valueOf(rs.getString("status")));
        eq.setLocation(rs.getString("location"));
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ca != null) eq.setCreatedAt(ca.toLocalDateTime());
        if (ua != null) eq.setUpdatedAt(ua.toLocalDateTime());
        return eq;
    }
}
