package com.equipment.service;

import com.equipment.dao.*;
import com.equipment.model.*;
import com.equipment.util.ValidationUtil;

import java.sql.SQLException;
import java.util.List;

/**
 * LabManagerService - Business Logic Layer
 * Handles all Lab Manager use cases.
 *
 * GRASP Patterns:
 *   - Controller: Coordinates all lab manager operations across DAOs.
 *   - Information Expert: Knows business rules for bookings, equipment, and task assignment.
 *   - Low Coupling / High Cohesion: Only lab manager related operations.
 *
 * GOF Pattern:
 *   - Facade: Hides multi-DAO coordination complexity from callers.
 *
 * Use Cases Implemented:
 *   UC01 (Lab Manager) - Approve / Reject Booking Request
 *   UC02 (Lab Manager) - Monitor Equipment Availability
 *   UC03 (Lab Manager) - Update Equipment Status
 *   UC04 (Lab Manager) - View Reports and Analytics
 *   UC05 (Lab Manager) - Assign Maintenance Task to Technician
 */
public class LabManagerService {

    private final BookingDAO         bookingDAO;
    private final EquipmentDAO       equipmentDAO;
    private final FaultReportDAO     faultReportDAO;
    private final MaintenanceTaskDAO maintenanceTaskDAO;
    private final UserDAO            userDAO;
    private final AuditLogDAO        auditLogDAO;

    // GRASP Creator: LabManagerService creates its own DAO dependencies
    public LabManagerService() {
        this.bookingDAO         = new BookingDAO();
        this.equipmentDAO       = new EquipmentDAO();
        this.faultReportDAO     = new FaultReportDAO();
        this.maintenanceTaskDAO = new MaintenanceTaskDAO();
        this.userDAO            = new UserDAO();
        this.auditLogDAO        = new AuditLogDAO();
    }

    // -------------------------------------------------------
    // UC01 (Lab Manager): Approve or Reject Booking Request
    // System Operation: processBooking(managerId, bookingId, newStatus)
    // -------------------------------------------------------
    /**
     * Lab Manager approves or rejects a pending booking.
     * Business rules:
     *   1. Only PENDING bookings can be processed.
     *   2. On APPROVED: equipment remains RESERVED.
     *   3. On REJECTED: equipment status reverts to AVAILABLE.
     */
    public boolean processBooking(int managerId, int bookingId, Booking.Status newStatus)
            throws SQLException {

        ValidationUtil.requirePositive(managerId,  "Manager ID");
        ValidationUtil.requirePositive(bookingId,  "Booking ID");
        if (newStatus == null) throw new IllegalArgumentException("New status is required.");

        if (newStatus != Booking.Status.APPROVED && newStatus != Booking.Status.REJECTED)
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED.");

        boolean updated = bookingDAO.updateBookingStatus(bookingId, newStatus, managerId);

        if (updated) {
            // If rejected, check if equipment should revert to AVAILABLE
            if (newStatus == Booking.Status.REJECTED) {
                // Find the booking to get equipmentId - re-fetch from all bookings
                List<Booking> all = bookingDAO.getAllBookings();
                all.stream()
                   .filter(b -> b.getBookingId() == bookingId)
                   .findFirst()
                   .ifPresent(b -> {
                       try {
                           // Only revert if no other active bookings for same equipment
                           boolean hasOther = bookingDAO.hasConflict(
                               b.getEquipmentId(), b.getBookingDate(),
                               b.getStartTime(), b.getEndTime(), bookingId);
                           if (!hasOther) {
                               equipmentDAO.updateStatus(b.getEquipmentId(), Equipment.Status.AVAILABLE);
                           }
                       } catch (SQLException ignored) {}
                   });
            }
            auditLogDAO.logAction(managerId, newStatus == Booking.Status.APPROVED
                    ? "APPROVE_BOOKING" : "REJECT_BOOKING",
                    "BOOKING", bookingId,
                    "Booking #" + bookingId + " " + newStatus.name().toLowerCase() + " by manager #" + managerId);
        }
        return updated;
    }

    // -------------------------------------------------------
    // UC01 Helper: Get all pending bookings for review
    // -------------------------------------------------------
    public List<Booking> getPendingBookings() throws SQLException {
        return bookingDAO.getPendingBookings();
    }

    // -------------------------------------------------------
    // UC02 (Lab Manager): Monitor Equipment Availability
    // System Operation: monitorEquipmentAvailability()
    // -------------------------------------------------------
    /**
     * Returns all equipment with their current status for monitoring.
     * Lab Manager can see which equipment is AVAILABLE, RESERVED, FAULTY, etc.
     *
     * @return list of all Equipment objects with current statuses
     */
    public List<Equipment> monitorEquipmentAvailability() throws SQLException {
        return equipmentDAO.getAllEquipments();
    }
    // -------------------------------------------------------
    // Check if equipment has any unresolved active fault
    // -------------------------------------------------------
    public boolean hasActiveFault(int equipmentId) throws SQLException {
        List<FaultReport> faults = faultReportDAO.getMaintenanceHistoryByEquipment(equipmentId);
        return faults.stream().anyMatch(f ->
            f.getStatus() == FaultReport.Status.REPORTED  ||
            f.getStatus() == FaultReport.Status.ASSIGNED  ||
            f.getStatus() == FaultReport.Status.IN_PROGRESS
        );
    }
    // -------------------------------------------------------
    // UC03 (Lab Manager): Update Equipment Status
    // System Operation: updateEquipmentStatus(managerId, equipmentId, newStatus)
    // -------------------------------------------------------
    /**
     * Lab Manager manually updates the status of an equipment item.
     * Business rules:
     *   1. Equipment must exist.
     *   2. RETIRED equipment cannot be changed back to AVAILABLE directly.
     *   3. Action is logged for audit trail.
     */
    public boolean updateEquipmentStatus(int managerId, int equipmentId, Equipment.Status newStatus)
            throws SQLException {

        ValidationUtil.requirePositive(managerId,   "Manager ID");
        ValidationUtil.requirePositive(equipmentId, "Equipment ID");
        if (newStatus == null) throw new IllegalArgumentException("New status is required.");

        Equipment eq = equipmentDAO.checkAvailability(equipmentId);
        if (eq == null) throw new IllegalArgumentException("Equipment not found.");

        // Business rule: Cannot reactivate RETIRED equipment to AVAILABLE directly
        if (eq.getStatus() == Equipment.Status.RETIRED && newStatus == Equipment.Status.AVAILABLE)
            throw new IllegalStateException("Retired equipment must be formally recommissioned before marking AVAILABLE.");

        boolean updated = equipmentDAO.updateStatus(equipmentId, newStatus);

        if (updated) {
            auditLogDAO.logAction(managerId, "UPDATE_EQUIPMENT_STATUS", "EQUIPMENT", equipmentId,
                    "Equipment '" + eq.getName() + "' status changed from " + eq.getStatus() + " to " + newStatus);
        }
        return updated;
    }

    // -------------------------------------------------------
    // UC04 (Lab Manager): View Reports and Analytics
    // System Operations: getUnresolvedFaults(), getAllBookings(), getAuditLogs()
    // -------------------------------------------------------
    /**
     * Returns all unresolved (REPORTED / ASSIGNED / IN_PROGRESS) fault reports.
     * Sorted by severity (HIGH first) then by reported date.
     */
    public List<FaultReport> getUnresolvedFaults() throws SQLException {
        return faultReportDAO.getUnresolvedFaults();
    }

    /**
     * Returns all bookings in the system for reporting purposes.
     */
    public List<Booking> getAllBookings() throws SQLException {
        return bookingDAO.getAllBookings();
    }

    /**
     * Returns full audit log for admin/analytics view.
     */
    public List<AuditLog> getAuditLogs() throws SQLException {
        return auditLogDAO.getAllLogs();
    }

    /**
     * Returns all fault reports sorted by severity (for analytics dashboard).
     */
    public List<FaultReport> getAllFaultsSortedBySeverity() throws SQLException {
        return faultReportDAO.getAllFaultsSortedBySeverity();
    }

    /**
     * Returns all maintenance tasks (active + completed) for overview.
     */
    public List<MaintenanceTask> getAllMaintenanceTasks() throws SQLException {
        return maintenanceTaskDAO.getAllTasks();
    }

    // -------------------------------------------------------
    // UC05 (Lab Manager): Assign Maintenance Task to Technician
    // System Operation: assignMaintenanceTask(managerId, faultId, technicianId, priority, notes)
    // -------------------------------------------------------
    /**
     * Lab Manager assigns a maintenance task to a technician for a reported fault.
     * Business rules:
     *   1. Fault must exist and be in REPORTED status (or ASSIGNED for reassignment).
     *   2. Technician must exist and have TECHNICIAN role.
     *   3. No duplicate active tasks for the same fault.
     *   4. Fault status updated to ASSIGNED upon task creation.
     */
    public MaintenanceTask assignMaintenanceTask(int managerId, int faultId, int technicianId,
                                                  MaintenanceTask.Priority priority, String notes)
            throws SQLException {

        ValidationUtil.requirePositive(managerId,    "Manager ID");
        ValidationUtil.requirePositive(faultId,      "Fault ID");
        ValidationUtil.requirePositive(technicianId, "Technician ID");
        if (priority == null) throw new IllegalArgumentException("Priority is required.");

        // Validate fault exists
        FaultReport fault = faultReportDAO.getFaultById(faultId);
        if (fault == null) throw new IllegalArgumentException("Fault report not found.");
        if (fault.getStatus() == FaultReport.Status.RESOLVED)
            throw new IllegalStateException("Fault is already resolved.");

        // Validate no active task already exists for this fault
        if (maintenanceTaskDAO.hasPendingTaskForFault(faultId))
            throw new IllegalStateException("An active maintenance task already exists for this fault.");

        // Validate technician exists and has correct role
        User tech = userDAO.getUserById(technicianId);
        if (tech == null) throw new IllegalArgumentException("Technician not found.");
        if (tech.getRole() != User.Role.TECHNICIAN)
            throw new IllegalArgumentException("Assigned user is not a Technician.");

        // Create the maintenance task
        MaintenanceTask task = new MaintenanceTask(faultId, technicianId, managerId, priority);
        task.setNotes(notes);
        boolean created = maintenanceTaskDAO.assignTask(task);

        if (created) {
            // Update fault status to ASSIGNED
            faultReportDAO.updateFaultStatus(faultId, FaultReport.Status.ASSIGNED);
            auditLogDAO.logAction(managerId, "ASSIGN_TASK", "MAINTENANCE_TASK", task.getTaskId(),
                    "Task assigned to technician '" + tech.getFullName() + "' for fault #" + faultId
                    + " [" + priority + "]");
        }
        return created ? task : null;
    }

    // -------------------------------------------------------
    // Helper: Get all technicians (for task assignment UI)
    // -------------------------------------------------------
    public List<User> getAllTechnicians() throws SQLException {
        return userDAO.getUsersByRole(User.Role.TECHNICIAN);
    }

    // -------------------------------------------------------
    // Helper: Add new equipment to inventory
    // -------------------------------------------------------
    public boolean addEquipment(int managerId, Equipment equipment) throws SQLException {
        ValidationUtil.requirePositive(managerId, "Manager ID");
        if (equipment == null) throw new IllegalArgumentException("Equipment cannot be null.");
        ValidationUtil.requireNonBlank(equipment.getName(), "Equipment name");

        boolean created = equipmentDAO.addEquipment(equipment);
        if (created) {
            auditLogDAO.logAction(managerId, "ADD_EQUIPMENT", "EQUIPMENT", equipment.getEquipmentId(),
                    "New equipment added: '" + equipment.getName() + "'");
        }
        return created;
    }
}
