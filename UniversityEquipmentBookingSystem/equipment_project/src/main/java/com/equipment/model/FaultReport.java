package com.equipment.model;

import java.time.LocalDateTime;

/**
 * FaultReport - Domain Model Entity
 * Represents a fault reported on equipment by a Teacher
 */
public class FaultReport {

    public enum Severity { LOW, MEDIUM, HIGH }
    public enum Status   { REPORTED, ASSIGNED, IN_PROGRESS, RESOLVED }

    private int faultId;
    private int equipmentId;
    private int reportedBy;
    private String faultDescription;
    private Severity severity;
    private Status status;
    private LocalDateTime reportedDate;
    private LocalDateTime resolvedDate;

    // Joined fields
    private String equipmentName;
    private String reporterName;

    public FaultReport() {}

    public FaultReport(int equipmentId, int reportedBy, String faultDescription, Severity severity) {
        this.equipmentId      = equipmentId;
        this.reportedBy       = reportedBy;
        this.faultDescription = faultDescription;
        this.severity         = severity;
        this.status           = Status.REPORTED;
    }

    // Getters & Setters
    public int getFaultId()                     { return faultId; }
    public void setFaultId(int id)              { this.faultId = id; }

    public int getEquipmentId()                 { return equipmentId; }
    public void setEquipmentId(int id)          { this.equipmentId = id; }

    public int getReportedBy()                  { return reportedBy; }
    public void setReportedBy(int id)           { this.reportedBy = id; }

    public String getFaultDescription()         { return faultDescription; }
    public void setFaultDescription(String d)   { this.faultDescription = d; }

    public Severity getSeverity()               { return severity; }
    public void setSeverity(Severity s)         { this.severity = s; }

    public Status getStatus()                   { return status; }
    public void setStatus(Status s)             { this.status = s; }

    public LocalDateTime getReportedDate()      { return reportedDate; }
    public void setReportedDate(LocalDateTime d){ this.reportedDate = d; }

    public LocalDateTime getResolvedDate()      { return resolvedDate; }
    public void setResolvedDate(LocalDateTime d){ this.resolvedDate = d; }

    public String getEquipmentName()            { return equipmentName; }
    public void setEquipmentName(String n)      { this.equipmentName = n; }

    public String getReporterName()             { return reporterName; }
    public void setReporterName(String n)       { this.reporterName = n; }

    public boolean isResolved() {
        return this.status == Status.RESOLVED;
    }

    @Override
    public String toString() {
        return String.format("FaultReport{id=%d, equipment='%s', severity=%s, status=%s}",
                faultId, equipmentName != null ? equipmentName : equipmentId, severity, status);
    }
}
