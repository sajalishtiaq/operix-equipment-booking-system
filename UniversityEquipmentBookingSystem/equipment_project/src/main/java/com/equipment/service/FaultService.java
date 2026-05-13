package com.equipment.service;

import com.equipment.dao.AuditLogDAO;
import com.equipment.dao.EquipmentDAO;
import com.equipment.dao.FaultReportDAO;
import com.equipment.dao.MaintenanceTaskDAO;
import com.equipment.model.Equipment;
import com.equipment.model.FaultReport;
import com.equipment.model.MaintenanceTask;
import com.equipment.util.ValidationUtil;

import java.sql.SQLException;
import java.util.List;

/**
 * FaultService - Business Logic Layer
 */
public class FaultService {

    private final FaultReportDAO      faultReportDAO;
    private final MaintenanceTaskDAO  maintenanceTaskDAO;
    private final EquipmentDAO        equipmentDAO;
    private final AuditLogDAO         auditLogDAO;

    public FaultService() {
        this.faultReportDAO     = new FaultReportDAO();
        this.maintenanceTaskDAO = new MaintenanceTaskDAO();
        this.equipmentDAO       = new EquipmentDAO();
        this.auditLogDAO        = new AuditLogDAO();
    }

    // -------------------------------------------------------
    // UC04 (Teacher): Report Faulty Equipment
    // -------------------------------------------------------
    public FaultReport reportFault(int teacherId, int equipmentId,
                                   String description, FaultReport.Severity severity)
            throws SQLException {

        ValidationUtil.requirePositive(teacherId,   "Teacher ID");
        ValidationUtil.requirePositive(equipmentId, "Equipment ID");
        ValidationUtil.requireNonBlank(description,  "Fault description");
        if (severity == null) throw new IllegalArgumentException("Severity is required.");

        Equipment eq = equipmentDAO.checkAvailability(equipmentId);
        if (eq == null) throw new IllegalArgumentException("Equipment not found.");

        FaultReport fault = new FaultReport(equipmentId, teacherId, description, severity);
        boolean created = faultReportDAO.reportFault(fault);

        if (created) {
            equipmentDAO.updateStatus(equipmentId, Equipment.Status.FAULTY);
            auditLogDAO.logAction(teacherId, "REPORT_FAULT", "FAULT_REPORT", fault.getFaultId(),
                    "Fault reported on '" + eq.getName() + "': " + description);
        }
        return created ? fault : null;
    }

    // -------------------------------------------------------
    // UC01 (Technician): View Assigned Maintenance Tasks
    // -------------------------------------------------------
    public List<MaintenanceTask> getAssignedTasks(int technicianId) throws SQLException {
        ValidationUtil.requirePositive(technicianId, "Technician ID");
        return maintenanceTaskDAO.getTasksByTechnician(technicianId);
    }

    // -------------------------------------------------------
    // UC02 (Technician): Update Repair Status
    //
    // State machine:
    //   PENDING     → IN_PROGRESS : marks fault IN_PROGRESS, equipment UNDER_REPAIR
    //   IN_PROGRESS → COMPLETED   : marks fault RESOLVED,    equipment AVAILABLE  
    //   PENDING     → COMPLETED   : same as above (direct resolve)                
    // -------------------------------------------------------
    public boolean updateRepairStatus(int technicianId, int taskId,
                                      MaintenanceTask.Status newStatus, String notes)
            throws SQLException {

        ValidationUtil.requirePositive(technicianId, "Technician ID");
        ValidationUtil.requirePositive(taskId,       "Task ID");
        if (newStatus == null) throw new IllegalArgumentException("New status is required.");

        MaintenanceTask task = maintenanceTaskDAO.getTaskById(taskId);
        if (task == null) throw new IllegalArgumentException("Maintenance task not found.");

        if (task.getTechnicianId() != technicianId)
            throw new SecurityException("Technician is not assigned to this task.");

        // Validate forward-only state transition
        boolean validTransition =
                (task.getStatus() == MaintenanceTask.Status.PENDING
                        && newStatus == MaintenanceTask.Status.IN_PROGRESS)
             || (task.getStatus() == MaintenanceTask.Status.IN_PROGRESS
                        && newStatus == MaintenanceTask.Status.COMPLETED)
             || (task.getStatus() == MaintenanceTask.Status.PENDING
                        && newStatus == MaintenanceTask.Status.COMPLETED);

        if (!validTransition)
            throw new IllegalStateException("Invalid status transition: "
                    + task.getStatus() + " → " + newStatus);

        if (notes != null && !notes.isBlank()) {
            maintenanceTaskDAO.updateTaskNotes(taskId, notes);
        }

        boolean updated = maintenanceTaskDAO.updateTaskStatus(taskId, newStatus);

        if (updated) {

            FaultReport fault = faultReportDAO.getFaultById(task.getFaultId());

            if (newStatus == MaintenanceTask.Status.IN_PROGRESS) {
                // Mark fault as in-progress
                faultReportDAO.updateFaultStatus(task.getFaultId(), FaultReport.Status.IN_PROGRESS);
                // Mark equipment as under repair using the CORRECT equipment ID
                if (fault != null) {
                    equipmentDAO.updateStatus(fault.getEquipmentId(), Equipment.Status.UNDER_REPAIR);
                }

            } else if (newStatus == MaintenanceTask.Status.COMPLETED) {
               
                faultReportDAO.updateFaultStatus(task.getFaultId(), FaultReport.Status.RESOLVED);
                // Restore equipment to AVAILABLE using the CORRECT equipment ID
                if (fault != null) {
                    equipmentDAO.updateStatus(fault.getEquipmentId(), Equipment.Status.AVAILABLE);
                }
            }

            auditLogDAO.logAction(technicianId, "UPDATE_REPAIR_STATUS", "MAINTENANCE_TASK", taskId,
                    "Task #" + taskId + " status changed to " + newStatus);
        }
        return updated;
    }

    // -------------------------------------------------------
    // UC03 (Technician): Mark Fault as Resolved
    // (shortcut — sets task COMPLETED + fault RESOLVED in one click)
    // -------------------------------------------------------
    public boolean markFaultResolved(int technicianId, int taskId) throws SQLException {
        ValidationUtil.requirePositive(technicianId, "Technician ID");
        ValidationUtil.requirePositive(taskId,       "Task ID");

        MaintenanceTask task = maintenanceTaskDAO.getTaskById(taskId);
        if (task == null) throw new IllegalArgumentException("Maintenance task not found.");

        if (task.getTechnicianId() != technicianId)
            throw new SecurityException("Technician is not assigned to this task.");

        if (task.getStatus() == MaintenanceTask.Status.COMPLETED)
            throw new IllegalStateException("Task is already completed.");

        boolean taskUpdated = maintenanceTaskDAO.updateTaskStatus(taskId, MaintenanceTask.Status.COMPLETED);

        if (taskUpdated) {
            faultReportDAO.updateFaultStatus(task.getFaultId(), FaultReport.Status.RESOLVED);
            FaultReport fault = faultReportDAO.getFaultById(task.getFaultId());
            if (fault != null) {
                equipmentDAO.updateStatus(fault.getEquipmentId(), Equipment.Status.AVAILABLE);
            }
            auditLogDAO.logAction(technicianId, "MARK_RESOLVED", "FAULT_REPORT", task.getFaultId(),
                    "Fault #" + task.getFaultId() + " resolved by technician #" + technicianId);
        }
        return taskUpdated;
    }

    // -------------------------------------------------------
    // UC04 (Technician): View Fault Details
    // -------------------------------------------------------
    public FaultReport getFaultDetails(int faultId) throws SQLException {
        ValidationUtil.requirePositive(faultId, "Fault ID");
        return faultReportDAO.getFaultById(faultId);
    }

    // -------------------------------------------------------
    // UC05 (Technician): View Maintenance History
    // -------------------------------------------------------
    public List<MaintenanceTask> getMaintenanceHistory(int equipmentId) throws SQLException {
        ValidationUtil.requirePositive(equipmentId, "Equipment ID");
        return maintenanceTaskDAO.getCompletedTasksByEquipment(equipmentId);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    public List<FaultReport> getFaultHistoryByEquipment(int equipmentId) throws SQLException {
        return faultReportDAO.getMaintenanceHistoryByEquipment(equipmentId);
    }

    public List<FaultReport> getFaultsByTechnician(int technicianId) throws SQLException {
        return faultReportDAO.getFaultsByTechnician(technicianId);
    }
}
