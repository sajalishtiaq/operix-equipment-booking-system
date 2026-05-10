package com.equipment.model;

import java.time.LocalDateTime;

/**
 * MaintenanceTask - Domain Model Entity
 * Assigned to Technician by Lab Manager for fault resolution
 */
public class MaintenanceTask {

    public enum Priority { LOW, MEDIUM, HIGH }
    public enum Status   { PENDING, IN_PROGRESS, COMPLETED }

    private int taskId;
    private int faultId;
    private int technicianId;
    private int assignedBy;
    private Priority priority;
    private Status status;
    private String notes;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;

    // Joined / display fields
    private String technicianName;
    private String assignedByName;
    private String equipmentName;
    private String faultDescription;

    public MaintenanceTask() {}

    public MaintenanceTask(int faultId, int technicianId, int assignedBy, Priority priority) {
        this.faultId      = faultId;
        this.technicianId = technicianId;
        this.assignedBy   = assignedBy;
        this.priority     = priority;
        this.status       = Status.PENDING;
    }

    // Getters & Setters
    public int getTaskId()                      { return taskId; }
    public void setTaskId(int id)               { this.taskId = id; }

    public int getFaultId()                     { return faultId; }
    public void setFaultId(int id)              { this.faultId = id; }

    public int getTechnicianId()                { return technicianId; }
    public void setTechnicianId(int id)         { this.technicianId = id; }

    public int getAssignedBy()                  { return assignedBy; }
    public void setAssignedBy(int id)           { this.assignedBy = id; }

    public Priority getPriority()               { return priority; }
    public void setPriority(Priority p)         { this.priority = p; }

    public Status getStatus()                   { return status; }
    public void setStatus(Status s)             { this.status = s; }

    public String getNotes()                    { return notes; }
    public void setNotes(String n)              { this.notes = n; }

    public LocalDateTime getAssignedAt()        { return assignedAt; }
    public void setAssignedAt(LocalDateTime t)  { this.assignedAt = t; }

    public LocalDateTime getCompletedAt()       { return completedAt; }
    public void setCompletedAt(LocalDateTime t) { this.completedAt = t; }

    public String getTechnicianName()           { return technicianName; }
    public void setTechnicianName(String n)     { this.technicianName = n; }

    public String getAssignedByName()           { return assignedByName; }
    public void setAssignedByName(String n)     { this.assignedByName = n; }

    public String getEquipmentName()            { return equipmentName; }
    public void setEquipmentName(String n)      { this.equipmentName = n; }

    public String getFaultDescription()         { return faultDescription; }
    public void setFaultDescription(String d)   { this.faultDescription = d; }

    @Override
    public String toString() {
        return String.format("MaintenanceTask{id=%d, technician='%s', priority=%s, status=%s}",
                taskId, technicianName != null ? technicianName : technicianId, priority, status);
    }
}
