package com.equipment;

import com.equipment.model.*;
import com.equipment.service.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Main - Application Entry Point
 * Demonstrates all 13 use cases across three roles.
 *
 * Architecture (3-Layer):
 *   UI Layer       → this class (console)
 *   Business Logic → AuthService, EquipmentService, FaultService, LabManagerService
 *   Persistence    → DAO classes + DatabaseConnection (Singleton)
 *
 * Design Patterns applied:
 *   GRASP: Controller, Creator, Information Expert, Low Coupling, High Cohesion
 *   GOF:   Singleton (DatabaseConnection), Facade (Service classes)
 */
public class Main {

    
    public static void main(String[] args) {
        System.out.println("=== Classroom Equipment Booking & Fault Tracking System ===\n");
        AuthService       authService       = new AuthService();
        EquipmentService  equipmentService  = new EquipmentService();
        FaultService      faultService      = new FaultService();
        LabManagerService labManagerService = new LabManagerService();

        try {
            // -----------------------------------------------
            // TEACHER FLOW
            // -----------------------------------------------
            System.out.println("--- TEACHER LOGIN ---");
            User teacher = authService.login("teacher1", "hashed_pass1");
            if (teacher == null) {
                System.out.println("[WARN] teacher1 login failed (check seeded password).");
                return;
            }
            System.out.println("Logged in: " + teacher);

            // UC01 (Teacher) - Search Equipments
            System.out.println("\n[UC01-Teacher] Search Equipments by 'Projector'");
            List<Equipment> found = equipmentService.searchEquipments(teacher.getUserId(), "Projector");
            found.forEach(e -> System.out.println("  Found: " + e));

            // UC02 (Teacher) - Check Availability
            if (!found.isEmpty()) {
                System.out.println("\n[UC02-Teacher] Check Availability of first result");
                Equipment eq = equipmentService.checkAvailability(found.get(0).getEquipmentId());
                System.out.println("  Status: " + (eq != null ? eq.getStatus() : "NOT FOUND"));
            }

            // UC03 (Teacher) - Book Equipment
            System.out.println("\n[UC03-Teacher] Book Equipment");
            try {
                Booking booking = equipmentService.bookEquipment(
                        teacher.getUserId(),
                        found.isEmpty() ? 1 : found.get(0).getEquipmentId(),
                        LocalDate.now().plusDays(1),
                        LocalTime.of(9, 0),
                        LocalTime.of(11, 0),
                        "Physics lecture demo"
                );
                System.out.println("  Booking created: " + (booking != null ? booking : "CONFLICT"));
            } catch (IllegalStateException e) {
                System.out.println("  Booking skipped: " + e.getMessage());
            }

            // UC05 (Teacher) - Check Booking Status
            System.out.println("\n[UC05-Teacher] Check Booking Status");
            List<Booking> bookings = equipmentService.getBookingStatus(teacher.getUserId());
            bookings.stream().limit(3).forEach(b -> System.out.println("  " + b));

            // UC04 (Teacher) - Report Faulty Equipment
            System.out.println("\n[UC04-Teacher] Report Faulty Equipment (Microscope id=5)");
            try {
                FaultReport fault = faultService.reportFault(
                        teacher.getUserId(), 5,
                        "Lens cracked and motor not responding",
                        FaultReport.Severity.HIGH
                );
                System.out.println("  Fault reported: " + fault);

                // -----------------------------------------------
                // LAB MANAGER FLOW
                // -----------------------------------------------
                System.out.println("\n--- LAB MANAGER LOGIN ---");
                User manager = authService.login("manager1", "hashed_pass3");
                System.out.println("Logged in: " + manager);

                // UC01 (Lab Manager) - Approve/Reject Booking
                System.out.println("\n[UC01-LabManager] Approve/Reject Pending Bookings");
                List<Booking> pending = labManagerService.getPendingBookings();
                if (!pending.isEmpty()) {
                    boolean ok = labManagerService.processBooking(
                            manager.getUserId(), pending.get(0).getBookingId(), Booking.Status.APPROVED);
                    System.out.println("  Booking #" + pending.get(0).getBookingId() + " approved: " + ok);
                } else {
                    System.out.println("  No pending bookings.");
                }

                // UC02 (Lab Manager) - Monitor Equipment Availability
                System.out.println("\n[UC02-LabManager] Monitor Equipment Availability");
                List<Equipment> all = labManagerService.monitorEquipmentAvailability();
                all.forEach(e -> System.out.println("  " + e.getName() + " → " + e.getStatus()));

                // UC03 (Lab Manager) - Update Equipment Status
                System.out.println("\n[UC03-LabManager] Update Equipment Status (Whiteboard → RETIRED)");
                boolean upd = labManagerService.updateEquipmentStatus(
                        manager.getUserId(), 7, Equipment.Status.RETIRED);
                System.out.println("  Updated: " + upd);

                // UC04 (Lab Manager) - View Reports and Analytics
                System.out.println("\n[UC04-LabManager] View Reports");
                List<FaultReport> unresolved = labManagerService.getUnresolvedFaults();
                System.out.println("  Unresolved faults: " + unresolved.size());
                List<Booking> allBookings = labManagerService.getAllBookings();
                System.out.println("  Total bookings: " + allBookings.size());

                // UC05 (Lab Manager) - Assign Maintenance Task
                System.out.println("\n[UC05-LabManager] Assign Maintenance Task");
                List<User> techs = labManagerService.getAllTechnicians();
                if (!techs.isEmpty() && fault != null) {
                    MaintenanceTask task = labManagerService.assignMaintenanceTask(
                            manager.getUserId(),
                            fault.getFaultId(),
                            techs.get(0).getUserId(),
                            MaintenanceTask.Priority.HIGH,
                            "Urgent: replace microscope lens assembly"
                    );
                    System.out.println("  Task assigned: " + task);

                    // -----------------------------------------------
                    // TECHNICIAN FLOW
                    // -----------------------------------------------
                    System.out.println("\n--- TECHNICIAN LOGIN ---");
                    User tech = authService.login("tech1", "hashed_pass4");
                    System.out.println("Logged in: " + tech);

                    // UC01 (Technician) - View Assigned Tasks
                    System.out.println("\n[UC01-Technician] View Assigned Maintenance Tasks");
                    List<MaintenanceTask> tasks = faultService.getAssignedTasks(tech.getUserId());
                    tasks.forEach(t -> System.out.println("  " + t));

                    // UC04 (Technician) - View Fault Details
                    System.out.println("\n[UC04-Technician] View Fault Details");
                    FaultReport details = faultService.getFaultDetails(fault.getFaultId());
                    System.out.println("  " + details);

                    // UC02 (Technician) - Update Repair Status → IN_PROGRESS
                    System.out.println("\n[UC02-Technician] Update Repair Status to IN_PROGRESS");
                    boolean statusUp = faultService.updateRepairStatus(
                            tech.getUserId(), task.getTaskId(),
                            MaintenanceTask.Status.IN_PROGRESS,
                            "Started disassembly of microscope lens"
                    );
                    System.out.println("  Updated to IN_PROGRESS: " + statusUp);

                    // UC03 (Technician) - Mark Fault as Resolved
                    System.out.println("\n[UC03-Technician] Mark Fault as Resolved");
                    boolean resolved = faultService.markFaultResolved(tech.getUserId(), task.getTaskId());
                    System.out.println("  Fault resolved: " + resolved);

                    // UC05 (Technician) - View Maintenance History
                    System.out.println("\n[UC05-Technician] View Maintenance History for equipment #5");
                    List<MaintenanceTask> history = faultService.getMaintenanceHistory(5);
                    System.out.println("  Completed tasks: " + history.size());
                    history.forEach(t -> System.out.println("  " + t));
                }

            } catch (IllegalStateException e) {
                System.out.println("  State error: " + e.getMessage());
            }

            System.out.println("\n=== All Use Cases Demonstrated Successfully ===");

        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }
}
