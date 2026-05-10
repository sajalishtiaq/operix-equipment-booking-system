package com.equipment.model;

import java.time.LocalDateTime;

/**
 * Equipment - Domain Model Entity
 * Represents physical classroom equipment
 */
public class Equipment {

    public enum Status {
        AVAILABLE, RESERVED, FAULTY, UNDER_REPAIR, RETIRED
    }

    private int equipmentId;
    private String name;
    private String category;
    private String description;
    private Status status;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Equipment() {}

    public Equipment(String name, String category, String description, Status status, String location) {
        this.name        = name;
        this.category    = category;
        this.description = description;
        this.status      = status;
        this.location    = location;
    }

    // Getters & Setters
    public int getEquipmentId()             { return equipmentId; }
    public void setEquipmentId(int id)      { this.equipmentId = id; }

    public String getName()                 { return name; }
    public void setName(String n)           { this.name = n; }

    public String getCategory()             { return category; }
    public void setCategory(String c)       { this.category = c; }

    public String getDescription()          { return description; }
    public void setDescription(String d)    { this.description = d; }

    public Status getStatus()               { return status; }
    public void setStatus(Status s)         { this.status = s; }

    public String getLocation()             { return location; }
    public void setLocation(String l)       { this.location = l; }

    public LocalDateTime getCreatedAt()     { return createdAt; }
    public void setCreatedAt(LocalDateTime t){ this.createdAt = t; }

    public LocalDateTime getUpdatedAt()     { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t){ this.updatedAt = t; }

    public boolean isAvailable() {
        return this.status == Status.AVAILABLE;
    }

    @Override
    public String toString() {
        return String.format("Equipment{id=%d, name='%s', category='%s', status=%s, location='%s'}",
                equipmentId, name, category, status, location);
    }
}
