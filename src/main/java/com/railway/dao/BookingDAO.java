package com.railway.dao;

import com.railway.model.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Access Object for booking operations.
 */
public class BookingDAO {
    private static final Logger logger = LoggerFactory.getLogger(BookingDAO.class);

    /**
     * Adds a new booking to the database.
     * @param booking Booking object
     * @return Generated booking ID, or -1 on failure
     * @throws SQLException on database error
     */
    public int addBooking(Booking booking) throws SQLException {
        if (booking.getUserName() == null || booking.getUserName().isEmpty() || booking.getSeatsBooked() <= 0) {
            logger.error("Invalid booking data: {}", booking);
            throw new IllegalArgumentException("Invalid booking data");
        }
        String sql = "INSERT INTO bookings (user_name, seats_booked, status) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, booking.getUserName());
                stmt.setInt(2, booking.getSeatsBooked());
                stmt.setString(3, booking.getStatus());
                stmt.executeUpdate();
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int bookingId = rs.getInt(1);
                    conn.commit();
                    logger.info("Added booking: {}", booking);
                    return bookingId;
                }
                conn.rollback();
                return -1;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Failed to add booking: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * Updates a seat's booking status.
     * @param seatId Seat ID
     * @param isBooked Booking status
     * @param bookingId Associated booking ID (nullable)
     * @throws SQLException on database error
     */
    public void updateSeat(int seatId, boolean isBooked, Integer bookingId) throws SQLException {
        String sql = "UPDATE seats SET is_booked = ?, booking_id = ? WHERE seat_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, isBooked);
                if (bookingId == null) {
                    stmt.setNull(2, Types.INTEGER);
                } else {
                    stmt.setInt(2, bookingId);
                }
                stmt.setInt(3, seatId);
                stmt.executeUpdate();
                conn.commit();
                logger.debug("Updated seat {}: isBooked={}, bookingId={}", seatId, isBooked, bookingId);
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Failed to update seat {}: {}", seatId, e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * Retrieves available seats.
     * @param totalSeats Total number of seats
     * @return List of available seat IDs
     * @throws SQLException on database error
     */
    public List<Integer> getAvailableSeats(int totalSeats) throws SQLException {
        List<Integer> availableSeats = new ArrayList<>();
        String sql = "SELECT seat_id FROM seats WHERE is_booked = FALSE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                availableSeats.add(rs.getInt("seat_id"));
            }
            logger.debug("Retrieved {} available seats.", availableSeats.size());
            return availableSeats;
        }
    }

    /**
     * Retrieves booking history.
     * @return List of all bookings
     * @throws SQLException on database error
     */
    public List<Booking> getBookingHistory() throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                bookings.add(new Booking(
                        rs.getInt("id"),
                        rs.getString("user_name"),
                        rs.getInt("seats_booked"),
                        rs.getTimestamp("booking_time").toLocalDateTime(),
                        rs.getString("status")
                ));
            }
            logger.debug("Retrieved {} bookings.", bookings.size());
            return bookings;
        }
    }

    /**
     * Retrieves booking statistics.
     * @return Map of status to count
     * @throws SQLException on database error
     */
    public Map<String, Integer> getBookingStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT status, COUNT(*) as count FROM bookings GROUP BY status";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                stats.put(rs.getString("status"), rs.getInt("count"));
            }
            logger.debug("Retrieved booking stats: {}", stats);
            return stats;
        }
    }
}
