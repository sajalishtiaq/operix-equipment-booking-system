package com.equipment.service;

import com.equipment.dao.AuditLogDAO;
import com.equipment.dao.BookingDAO;
import com.equipment.dao.EquipmentDAO;
import com.equipment.model.Booking;
import com.equipment.model.Equipment;
import com.equipment.util.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class EquipmentService {

    private final EquipmentDAO equipmentDAO;
    private final BookingDAO bookingDAO;
    private final AuditLogDAO auditLogDAO;

    public EquipmentService() {
        this.equipmentDAO = new EquipmentDAO();
        this.bookingDAO = new BookingDAO();
        this.auditLogDAO = new AuditLogDAO();
    }

    // ===============================
    // UC01 - SEARCH EQUIPMENT
    // ===============================
    public List<Equipment> searchEquipments(int teacherId, String keyword) throws SQLException {
        ValidationUtil.requireNonBlank(keyword, "Search keyword");

        List<Equipment> results = equipmentDAO.searchEquipments(keyword.trim());

        auditLogDAO.logAction(
                teacherId,
                "SEARCH_EQUIPMENT",
                "EQUIPMENT",
                0,
                "Search: '" + keyword + "', results: " + results.size()
        );

        return results;
    }

    // ===============================
    // UC02 - CHECK AVAILABILITY
    // ===============================
    public Equipment checkAvailability(int equipmentId) throws SQLException {
        ValidationUtil.requirePositive(equipmentId, "Equipment ID");
        return equipmentDAO.checkAvailability(equipmentId);
    }

    // ===============================
    // UC03 - BOOK EQUIPMENT
    // ===============================
    public Booking bookEquipment(int teacherId, int equipmentId,
                                 LocalDate date, LocalTime startTime, LocalTime endTime,
                                 String purpose) throws SQLException {

        // ===== VALIDATION =====
        ValidationUtil.requirePositive(teacherId, "Teacher ID");
        ValidationUtil.requirePositive(equipmentId, "Equipment ID");
        ValidationUtil.requireNonBlank(purpose, "Purpose");

        if (date == null)
            throw new IllegalArgumentException("Booking date is required.");

        if (startTime == null || endTime == null)
            throw new IllegalArgumentException("Start/End time required.");

        if (!endTime.isAfter(startTime))
            throw new IllegalArgumentException("End time must be after start time.");

        if (date.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Cannot book for past date.");

        // ===== CHECK EQUIPMENT =====
        Equipment eq = equipmentDAO.checkAvailability(equipmentId);

        if (eq == null)
            throw new IllegalArgumentException("Equipment not found.");

        if (!eq.isAvailable())
            throw new IllegalStateException("Equipment not available (" + eq.getStatus() + ")");

        // ===== CONFLICT CHECK (CRITICAL) =====
        boolean conflict = bookingDAO.hasConflict(
                equipmentId,
                date,
                startTime,
                endTime,
                -1
        );

        if (conflict)
            throw new IllegalStateException("Time slot already booked.");

        // ===== CREATE BOOKING =====
        Booking booking = new Booking(
                teacherId,
                equipmentId,
                date,
                startTime,
                endTime,
                purpose
        );

        boolean created = bookingDAO.createBooking(booking);

        if (!created)
            throw new SQLException("Booking creation failed.");

        // ===== UPDATE EQUIPMENT STATUS =====
        equipmentDAO.updateStatus(equipmentId, Equipment.Status.RESERVED);

        // ===== AUDIT LOG =====
        auditLogDAO.logAction(
                teacherId,
                "BOOK_EQUIPMENT",
                "BOOKING",
                booking.getBookingId(),
                "Booked equipment '" + eq.getName() +
                        "' on " + date +
                        " from " + startTime + " to " + endTime
        );

        return booking;
    }

    // ===============================
    // UC05 - VIEW BOOKINGS
    // ===============================
    public List<Booking> getBookingStatus(int teacherId) throws SQLException {
        ValidationUtil.requirePositive(teacherId, "Teacher ID");
        return bookingDAO.getBookingsByTeacher(teacherId);
    }

    // ===============================
    // CANCEL BOOKING
    // ===============================
    public boolean cancelBooking(int bookingId, int teacherId) throws SQLException {

        boolean cancelled = bookingDAO.cancelBooking(bookingId, teacherId);

        if (cancelled) {
            auditLogDAO.logAction(
                    teacherId,
                    "CANCEL_BOOKING",
                    "BOOKING",
                    bookingId,
                    "Booking cancelled"
            );
        }

        return cancelled;
    }

    // ===============================
    // GENERAL
    // ===============================
    public List<Equipment> getAllEquipments() throws SQLException {
        return equipmentDAO.getAllEquipments();
    }

    public List<Equipment> getAvailableEquipments() throws SQLException {
        return equipmentDAO.getAvailableEquipments();
    }
}