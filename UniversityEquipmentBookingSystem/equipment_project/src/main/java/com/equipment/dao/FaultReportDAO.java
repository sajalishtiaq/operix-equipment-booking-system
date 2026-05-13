package com.equipment.dao;

import com.equipment.model.FaultReport;
import com.equipment.model.FaultReport.Severity;
import com.equipment.model.FaultReport.Status;
import com.equipment.persistence.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FaultReportDAO - Data Access Object
 * Handles DB operations for FaultReport entity
 */
public class FaultReportDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------
    // UC4 (Teacher): Report Faulty Equipment
    // -------------------------------------------------------
    public boolean reportFault(FaultReport fault) throws SQLException {
        String sql = "INSERT INTO dbo.fault_reports (equipment_id, reported_by, fault_description, severity, status) " +
                     "VALUES (?, ?, ?, ?, 'REPORTED')";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fault.getEquipmentId());
            ps.setInt(2, fault.getReportedBy());
            ps.setString(3, fault.getFaultDescription());
            ps.setString(4, fault.getSeverity().name());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) fault.setFaultId(keys.getInt(1));
            }
            return rows > 0;
        }
    }

    // -------------------------------------------------------
    // UC4 (Technician): View Fault Details
    // -------------------------------------------------------
    public FaultReport getFaultById(int faultId) throws SQLException {
        String sql = "SELECT fr.*, e.name AS equipment_name, u.full_name AS reporter_name " +
                     "FROM dbo.fault_reports fr " +
                     "JOIN dbo.equipment e ON fr.equipment_id = e.equipment_id " +
                     "JOIN dbo.users u ON fr.reported_by = u.user_id " +
                     "WHERE fr.fault_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, faultId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // -------------------------------------------------------
    // Lab Manager: Get all faults sorted by severity
    // -------------------------------------------------------
    public List<FaultReport> getAllFaultsSortedBySeverity() throws SQLException {
        String sql = "SELECT fr.*, e.name AS equipment_name, u.full_name AS reporter_name " +
                     "FROM dbo.fault_reports fr " +
                     "JOIN dbo.equipment e ON fr.equipment_id = e.equipment_id " +
                     "JOIN dbo.users u ON fr.reported_by = u.user_id " +
                     "ORDER BY CASE fr.severity " +
                     "  WHEN 'HIGH'   THEN 1 " +
                     "  WHEN 'MEDIUM' THEN 2 " +
                     "  WHEN 'LOW'    THEN 3 " +
                     "  ELSE 4 END, fr.reported_date ASC";
        return fetchAll(sql);
    }

    // -------------------------------------------------------
    // Technician: Get faults assigned to a specific technician
    // -------------------------------------------------------
    public List<FaultReport> getFaultsByTechnician(int technicianId) throws SQLException {
        String sql = "SELECT fr.*, e.name AS equipment_name, u.full_name AS reporter_name " +
                     "FROM dbo.fault_reports fr " +
                     "JOIN dbo.equipment e ON fr.equipment_id = e.equipment_id " +
                     "JOIN dbo.users u ON fr.reported_by = u.user_id " +
                     "JOIN dbo.maintenance_tasks mt ON mt.fault_id = fr.fault_id " +
                     "WHERE mt.technician_id = ? ORDER BY fr.reported_date DESC";
        List<FaultReport> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, technicianId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // -------------------------------------------------------
    // Update fault status
    // -------------------------------------------------------
    public boolean updateFaultStatus(int faultId, Status newStatus) throws SQLException {
        String sql = newStatus == Status.RESOLVED
                ? "UPDATE dbo.fault_reports SET status = ?, resolved_date = GETDATE() WHERE fault_id = ?"
                : "UPDATE dbo.fault_reports SET status = ? WHERE fault_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, faultId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------------------------------------
    // Reports: All faults for a given equipment (Maintenance History)
    // -------------------------------------------------------
    public List<FaultReport> getMaintenanceHistoryByEquipment(int equipmentId) throws SQLException {
        String sql = "SELECT fr.*, e.name AS equipment_name, u.full_name AS reporter_name " +
                     "FROM dbo.fault_reports fr " +
                     "JOIN dbo.equipment e ON fr.equipment_id = e.equipment_id " +
                     "JOIN dbo.users u ON fr.reported_by = u.user_id " +
                     "WHERE fr.equipment_id = ? ORDER BY fr.reported_date DESC";
        List<FaultReport> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // -------------------------------------------------------
    // Reports: All UNRESOLVED faults (for Lab Manager report)
    // -------------------------------------------------------
    public List<FaultReport> getUnresolvedFaults() throws SQLException {
        String sql = "SELECT fr.*, e.name AS equipment_name, u.full_name AS reporter_name " +
                     "FROM dbo.fault_reports fr " +
                     "JOIN dbo.equipment e ON fr.equipment_id = e.equipment_id " +
                     "JOIN dbo.users u ON fr.reported_by = u.user_id " +
                     "WHERE fr.status != 'RESOLVED' ORDER BY fr.reported_date DESC";
        return fetchAll(sql);
    }

    // -------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------
    private List<FaultReport> fetchAll(String sql) throws SQLException {
        List<FaultReport> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private FaultReport mapRow(ResultSet rs) throws SQLException {
        FaultReport fr = new FaultReport();
        fr.setFaultId(rs.getInt("fault_id"));
        fr.setEquipmentId(rs.getInt("equipment_id"));
        fr.setReportedBy(rs.getInt("reported_by"));
        fr.setFaultDescription(rs.getString("fault_description"));
        fr.setSeverity(Severity.valueOf(rs.getString("severity")));
        fr.setStatus(Status.valueOf(rs.getString("status")));
        Timestamp rd  = rs.getTimestamp("reported_date");
        Timestamp rsd = rs.getTimestamp("resolved_date");
        if (rd  != null) fr.setReportedDate(rd.toLocalDateTime());
        if (rsd != null) fr.setResolvedDate(rsd.toLocalDateTime());
        fr.setEquipmentName(rs.getString("equipment_name"));
        fr.setReporterName(rs.getString("reporter_name"));
        return fr;
    }
}
