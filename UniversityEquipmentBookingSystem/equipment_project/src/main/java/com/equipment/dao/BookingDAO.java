package com.equipment.dao;

import com.equipment.model.Booking;
import com.equipment.model.Booking.Status;
import com.equipment.persistence.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BookingDAO - FIXED VERSION
 * Works with existing schema (start_datetime + end_datetime
 */
public class BookingDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // =======================================================
    // CREATE BOOKING
    // =======================================================
    public boolean createBooking(Booking booking) throws SQLException {
        String sql = "INSERT INTO dbo.bookings " +
                    "(teacher_id, equipment_id, start_datetime, end_datetime, purpose, status) " +
                    "VALUES (?, ?, ?, ?, ?, 'PENDING')";

        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime startDT = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
            LocalDateTime endDT   = LocalDateTime.of(booking.getBookingDate(), booking.getEndTime());

            ps.setInt(1, booking.getTeacherId());
            ps.setInt(2, booking.getEquipmentId());
            ps.setTimestamp(3, Timestamp.valueOf(startDT));
            ps.setTimestamp(4, Timestamp.valueOf(endDT));
            ps.setString(5, booking.getPurpose());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    booking.setBookingId(keys.getInt(1));
                }
            }

            return rows > 0;
        }
    }

    // =======================================================
    // CONFLICT CHECK
    // =======================================================
    public boolean hasConflict(int equipmentId,
                               LocalDate date,
                               LocalTime start,
                               LocalTime end,
                               int excludeBookingId) throws SQLException {

        String sql =
            "SELECT COUNT(*) FROM dbo.bookings " +
            "WHERE equipment_id = ? " +
            "AND CAST(start_datetime AS DATE) = ? " +
            "AND status IN ('PENDING','APPROVED') " +
            "AND booking_id != ? " +
            "AND (start_datetime < ? AND end_datetime > ?)";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            LocalDateTime startDT = LocalDateTime.of(date, start);
            LocalDateTime endDT   = LocalDateTime.of(date, end);

            ps.setInt(1, equipmentId);
            ps.setDate(2, Date.valueOf(date));
            ps.setInt(3, excludeBookingId);
            ps.setTimestamp(4, Timestamp.valueOf(endDT));
            ps.setTimestamp(5, Timestamp.valueOf(startDT));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }

        return false;
    }

    // =======================================================
    // GET BOOKINGS BY TEACHER
    // =======================================================
    public List<Booking> getBookingsByTeacher(int teacherId) throws SQLException {
        String sql =
                "SELECT b.*, u.full_name AS teacher_name, e.name AS equipment_name, " +
                "m.full_name AS manager_name " +
                "FROM dbo.bookings b " +
                "JOIN dbo.users u ON b.teacher_id = u.user_id " +
                "JOIN dbo.equipment e ON b.equipment_id = e.equipment_id " +
                "LEFT JOIN dbo.users m ON b.manager_id = m.user_id " +  // ← FIXED
                "WHERE b.teacher_id = ? " +
                "ORDER BY b.created_at DESC";

        return fetchList(sql, teacherId);
    }

    // =======================================================
    // APPROVE / REJECT
    // =======================================================
    public boolean updateBookingStatus(int bookingId, Status status, int managerId) throws SQLException {
        String sql = "UPDATE dbo.bookings SET status = ?, manager_id = ? WHERE booking_id = ?";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, managerId);
            ps.setInt(3, bookingId);

            return ps.executeUpdate() > 0;
        }
    }

    // =======================================================
    // PENDING BOOKINGS
    // =======================================================
    public List<Booking> getPendingBookings() throws SQLException {
        String sql =
                "SELECT b.*, u.full_name AS teacher_name, e.name AS equipment_name, " +
                "NULL AS manager_name " +
                "FROM dbo.bookings b " +
                "JOIN dbo.users u ON b.teacher_id = u.user_id " +
                "JOIN dbo.equipment e ON b.equipment_id = e.equipment_id " +
                "WHERE b.status = 'PENDING' " +
                "ORDER BY b.start_datetime";

        return fetchList(sql, null);
    }

    // =======================================================
    // CANCEL BOOKING
    // =======================================================
    public boolean cancelBooking(int bookingId, int teacherId) throws SQLException {
        String sql =
                "UPDATE dbo.bookings SET status = 'CANCELLED' " +
                "WHERE booking_id = ? AND teacher_id = ? AND status = 'PENDING'";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, teacherId);

            return ps.executeUpdate() > 0;
        }
    }

    // =======================================================
    // ALL BOOKINGS
    // =======================================================
    public List<Booking> getAllBookings() throws SQLException {
        String sql =
                "SELECT b.*, u.full_name AS teacher_name, e.name AS equipment_name, " +
                "m.full_name AS manager_name " +
                "FROM dbo.bookings b " +
                "JOIN dbo.users u ON b.teacher_id = u.user_id " +
                "JOIN dbo.equipment e ON b.equipment_id = e.equipment_id " +
                "LEFT JOIN dbo.users m ON b.manager_id = m.user_id " +
                "ORDER BY b.created_at DESC";

        return fetchList(sql, null);
    }

    // =======================================================
    // GENERIC FETCH
    // =======================================================
    private List<Booking> fetchList(String sql, Integer param) throws SQLException {
        List<Booking> list = new ArrayList<>();

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    // =======================================================
    // MAP RESULTSET → OBJECT
    // =======================================================
    private Booking mapRow(ResultSet rs) throws SQLException {

        Booking b = new Booking();

        b.setBookingId(rs.getInt("booking_id"));
        b.setTeacherId(rs.getInt("teacher_id"));
        b.setEquipmentId(rs.getInt("equipment_id"));

        Timestamp start = rs.getTimestamp("start_datetime");
        Timestamp end   = rs.getTimestamp("end_datetime");

        if (start != null && end != null) {
            LocalDateTime startDT = start.toLocalDateTime();
            LocalDateTime endDT   = end.toLocalDateTime();

            b.setBookingDate(startDT.toLocalDate());
            b.setStartTime(startDT.toLocalTime());
            b.setEndTime(endDT.toLocalTime());
        }

        b.setPurpose(rs.getString("purpose"));
        b.setStatus(Status.valueOf(rs.getString("status")));
        b.setTeacherName(rs.getString("teacher_name"));
        b.setEquipmentName(rs.getString("equipment_name"));
        b.setManagerName(rs.getString("manager_name"));   // NULL is fine for pending

        return b;
    }
}
