package com.equipment.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Booking - FINAL FIXED ENTITY (matches current DB schema)
 */
public class Booking {

    public enum Status {
        PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED
    }

    private int bookingId;
    private int teacherId;
    private int equipmentId;

    // ✅ MATCHES YOUR DATABASE (NOT DATETIME)
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private String purpose;
    private Status status;
    private int managerId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Joined fields
    private String teacherName;
    private String equipmentName;
    private String managerName;

    public Booking() {}

    public Booking(int teacherId, int equipmentId,
                   LocalDate bookingDate,
                   LocalTime startTime,
                   LocalTime endTime,
                   String purpose) {

        this.teacherId = teacherId;
        this.equipmentId = equipmentId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = Status.PENDING;
    }

    // ======================================================
    // GETTERS & SETTERS
    // ======================================================

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getManagerId() {
        return managerId;
    }

    public void setManagerId(int managerId) {
        this.managerId = managerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    @Override
    public String toString() {
        return String.format(
                "Booking{id=%d, teacher='%s', equipment='%s', date=%s, start=%s, status=%s}",
                bookingId,
                teacherName != null ? teacherName : String.valueOf(teacherId),
                equipmentName != null ? equipmentName : String.valueOf(equipmentId),
                bookingDate,
                startTime,
                status
        );
    }
}