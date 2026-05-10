package com.equipment.model;

import java.time.LocalDateTime;

/**
 * AuditLog - Domain Model Entity
 * Records all system actions for accountability and traceability.
 * GRASP: Information Expert - knows its own data
 */
public class AuditLog {

    private int logId;
    private int userId;
    private String action;
    private String entityType;
    private int entityId;
    private String details;
    private LocalDateTime loggedAt;

    // Joined field
    private String username;

    public AuditLog() {}

    public AuditLog(int userId, String action, String entityType, int entityId, String details) {
        this.userId     = userId;
        this.action     = action;
        this.entityType = entityType;
        this.entityId   = entityId;
        this.details    = details;
    }

    public int getLogId()                       { return logId; }
    public void setLogId(int id)                { this.logId = id; }

    public int getUserId()                      { return userId; }
    public void setUserId(int id)               { this.userId = id; }

    public String getAction()                   { return action; }
    public void setAction(String a)             { this.action = a; }

    public String getEntityType()               { return entityType; }
    public void setEntityType(String t)         { this.entityType = t; }

    public int getEntityId()                    { return entityId; }
    public void setEntityId(int id)             { this.entityId = id; }

    public String getDetails()                  { return details; }
    public void setDetails(String d)            { this.details = d; }

    public LocalDateTime getLoggedAt()          { return loggedAt; }
    public void setLoggedAt(LocalDateTime t)    { this.loggedAt = t; }

    public String getUsername()                 { return username; }
    public void setUsername(String u)           { this.username = u; }

    @Override
    public String toString() {
        return String.format("AuditLog{id=%d, user=%s, action='%s', entity=%s[%d]}",
                logId, username != null ? username : userId, action, entityType, entityId);
    }
}
