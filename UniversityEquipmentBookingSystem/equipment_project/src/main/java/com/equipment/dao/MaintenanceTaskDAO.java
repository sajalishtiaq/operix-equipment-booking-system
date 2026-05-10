package com.equipment.dao;

import com.equipment.model.MaintenanceTask;
import com.equipment.model.MaintenanceTask.Priority;
import com.equipment.model.MaintenanceTask.Status;
import com.equipment.persistence.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MaintenanceTaskDAO - Data Access Object
 * Handles DB operations for MaintenanceTask entity.
 *
 * FIX (SQL Server compatibility):
 *   NOW() -> GETDATE()  — NOW() is MySQL only, not recognized by SQL Server
 */
public class MaintenanceTaskDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------
    // UC05 (Lab Manager): Assign Maintenance Task to Technician
    // -------------------------------------------------------
    public boolean assignTask(MaintenanceTask task) throws SQLException {
        String sql = "INSERT INTO dbo.maintenance_tasks (fault_id, technician_id, assigned_by, priority, status, notes) " +
                     "VALUES (?, ?, ?, ?, 'PENDING', ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, task.getFaultId());
            ps.setInt(2, task.getTechnicianId());
            ps.setInt(3, task.getAssignedBy());
            ps.setString(4, task.getPriority().name());
            ps.setString(5, task.getNotes());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) task.setTaskId(keys.getInt(1));
            }
            return rows > 0;
        }
    }

    // -------------------------------------------------------
    // UC01 (Technician): View All Assigned Tasks for a Technician
    // -------------------------------------------------------
    public List<MaintenanceTask> getTasksByTechnician(int technicianId) throws SQLException {
        String sql = "SELECT mt.*, u1.full_name AS technician_name, u2.full_name AS assigned_by_name, " +
                     "e.name AS equipment_name, fr.fault_description " +
                     "FROM dbo.maintenance_tasks mt " +
                     "JOIN dbo.users u1 ON mt.technician_id = u1.user_id " +
                     "JOIN dbo.users u2 ON mt.assigned_by = u2.user_id " +
                     "JOIN dbo.fault_reports fr ON mt.fault_id = fr.fault_id " +
                     "JOIN dbo.equipment e ON fr.equipment_id = e.equipment_id " +
                     "WHERE mt.technician_id = ? ORDER BY mt.assigned_at DESC";
        List<MaintenanceTask> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, technicianId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // -------------------------------------------------------
    // Get task by ID (for viewing details / ownership check)
    // -------------------------------------------------------
    public MaintenanceTask getTaskById(int taskId) throws SQLException {
        String sql = "SELECT mt.*, u1.full_name AS technician_name, u2.full_name AS assigned_by_name, " +
                     "e.name AS equipment_name, fr.fault_description " +
                     "FROM dbo.maintenance_tasks mt " +
                     "JOIN dbo.users u1 ON mt.technician_id = u1.user_id " +
                     "JOIN dbo.users u2 ON mt.assigned_by = u2.user_id " +
                     "JOIN dbo.fault_reports fr ON mt.fault_id = fr.fault_id " +
                     "JOIN dbo.equipment e ON fr.equipment_id = e.equipment_id " +
                     "WHERE mt.task_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // -------------------------------------------------------
    // UC02 (Technician): Update Repair Status
    // FIX: NOW() -> GETDATE()
    //      NOW() is MySQL syntax. SQL Server uses GETDATE() for current timestamp.
    // -------------------------------------------------------
    public boolean updateTaskStatus(int taskId, Status newStatus) throws SQLException {
        String sql = newStatus == Status.COMPLETED
                ? "UPDATE dbo.maintenance_tasks SET status = ?, completed_at = GETDATE() WHERE task_id = ?"
                : "UPDATE dbo.maintenance_tasks SET status = ? WHERE task_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, taskId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------------------------------------
    // UC02 (Technician): Update notes on a task
    // -------------------------------------------------------
    public boolean updateTaskNotes(int taskId, String notes) throws SQLException {
        String sql = "UPDATE dbo.maintenance_tasks SET notes = ? WHERE task_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, notes);
            ps.setInt(2, taskId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------------------------------------
    // Get all tasks (Lab Manager view)
    // -------------------------------------------------------
    public List<MaintenanceTask> getAllTasks() throws SQLException {
        String sql = "SELECT mt.*, u1.full_name AS technician_name, u2.full_name AS assigned_by_name, " +
                     "e.name AS equipment_name, fr.fault_description " +
                     "FROM dbo.maintenance_tasks mt " +
                     "JOIN dbo.users u1 ON mt.technician_id = u1.user_id " +
                     "JOIN dbo.users u2 ON mt.assigned_by = u2.user_id " +
                     "JOIN dbo.fault_reports fr ON mt.fault_id = fr.fault_id " +
                     "JOIN dbo.equipment e ON fr.equipment_id = e.equipment_id " +
                     "ORDER BY mt.assigned_at DESC";
        return fetchAll(sql);
    }

    // -------------------------------------------------------
    // Get pending/in-progress tasks (active tasks)
    // -------------------------------------------------------
    public List<MaintenanceTask> getActiveTasks() throws SQLException {
        String sql = "SELECT mt.*, u1.full_name AS technician_name, u2.full_name AS assigned_by_name, " +
                     "e.name AS equipment_name, fr.fault_description " +
                     "FROM dbo.maintenance_tasks mt " +
                     "JOIN dbo.users u1 ON mt.technician_id = u1.user_id " +
                     "JOIN dbo.users u2 ON mt.assigned_by = u2.user_id " +
                     "JOIN dbo.fault_reports fr ON mt.fault_id = fr.fault_id " +
                     "JOIN dbo.equipment e ON fr.equipment_id = e.equipment_id " +
                     "WHERE mt.status != 'COMPLETED' ORDER BY mt.assigned_at ASC";
        return fetchAll(sql);
    }

    // -------------------------------------------------------
    // UC05 (View Maintenance History): Get completed tasks for equipment
    // -------------------------------------------------------
    public List<MaintenanceTask> getCompletedTasksByEquipment(int equipmentId) throws SQLException {
        String sql = "SELECT mt.*, u1.full_name AS technician_name, u2.full_name AS assigned_by_name, " +
                     "e.name AS equipment_name, fr.fault_description " +
                     "FROM dbo.maintenance_tasks mt " +
                     "JOIN dbo.users u1 ON mt.technician_id = u1.user_id " +
                     "JOIN dbo.users u2 ON mt.assigned_by = u2.user_id " +
                     "JOIN dbo.fault_reports fr ON mt.fault_id = fr.fault_id " +
                     "JOIN dbo.equipment e ON fr.equipment_id = e.equipment_id " +
                     "WHERE e.equipment_id = ? AND mt.status = 'COMPLETED' " +
                     "ORDER BY mt.completed_at DESC";
        List<MaintenanceTask> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // -------------------------------------------------------
    // Check if a fault already has an active task assigned
    // -------------------------------------------------------
    public boolean hasPendingTaskForFault(int faultId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.maintenance_tasks WHERE fault_id = ? AND status != 'COMPLETED'";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, faultId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    // -------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------
    private List<MaintenanceTask> fetchAll(String sql) throws SQLException {
        List<MaintenanceTask> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private MaintenanceTask mapRow(ResultSet rs) throws SQLException {
        MaintenanceTask mt = new MaintenanceTask();
        mt.setTaskId(rs.getInt("task_id"));
        mt.setFaultId(rs.getInt("fault_id"));
        mt.setTechnicianId(rs.getInt("technician_id"));
        mt.setAssignedBy(rs.getInt("assigned_by"));
        mt.setPriority(Priority.valueOf(rs.getString("priority")));
        mt.setStatus(Status.valueOf(rs.getString("status")));
        mt.setNotes(rs.getString("notes"));
        Timestamp aa = rs.getTimestamp("assigned_at");
        Timestamp ca = rs.getTimestamp("completed_at");
        if (aa != null) mt.setAssignedAt(aa.toLocalDateTime());
        if (ca != null) mt.setCompletedAt(ca.toLocalDateTime());
        mt.setTechnicianName(rs.getString("technician_name"));
        mt.setAssignedByName(rs.getString("assigned_by_name"));
        mt.setEquipmentName(rs.getString("equipment_name"));
        mt.setFaultDescription(rs.getString("fault_description"));
        return mt;
    }
}