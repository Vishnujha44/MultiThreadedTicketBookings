package com.railway.service;

import com.railway.dao.BookingDAO;
import com.railway.dao.DatabaseConnection;
import com.railway.dao.DatabaseInitializer;
import com.railway.model.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages railway ticket bookings, cancellations, and waitlist processing.
 */
public class RailwayTicketBookingSystem {
    private static final Logger logger = LoggerFactory.getLogger(RailwayTicketBookingSystem.class);
    private final int totalSeats;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Runnable> updateListeners = new ArrayList<>();

    /**
     * Constructs the booking system and initializes the database.
     * @param totalSeats Total number of seats
     */
    public RailwayTicketBookingSystem(int totalSeats) {
        this.totalSeats = totalSeats;
        initialize();
    }

    /**
     * Adds a listener for UI updates.
     * @param listener Runnable to execute on update
     */
    public void addUpdateListener(Runnable listener) {
        updateListeners.add(listener);
    }

    private void notifyUpdate() {
        updateListeners.forEach(Runnable::run);
    }

    private void initialize() {
        try {
            DatabaseInitializer.initializeDatabase(totalSeats);
            logger.info("Booking system initialized with {} seats.", totalSeats);
        } catch (Exception e) {
            logger.error("Initialization failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Books tickets for a user.
     * @param userName User's name
     * @param requestedSeats Number of seats
     * @param selectedSeats Specific seat IDs (optional)
     * @return true if successful, false otherwise
     */
    public boolean bookTicket(String userName, int requestedSeats, int[] selectedSeats) {
        lock.lock();
        try {
            if (!isValidUserName(userName) || requestedSeats <= 0 || requestedSeats > 10) {
                logger.error("Invalid booking request: userName={}, seats={}", userName, requestedSeats);
                return false;
            }
            BookingDAO dao = new BookingDAO();
            List<Integer> availableSeats = dao.getAvailableSeats(totalSeats);
            if (selectedSeats != null && selectedSeats.length > 0) {
                if (!validateSelectedSeats(selectedSeats, availableSeats, requestedSeats)) {
                    logger.error("Invalid seat selection by {}: {}", userName, Arrays.toString(selectedSeats));
                    return false;
                }
            }
            boolean isWaitlisted = availableSeats.size() < requestedSeats;
            String status = isWaitlisted ? "WAITLISTED" : "CONFIRMED";
            Booking booking = new Booking(0, userName, requestedSeats, null, status);
            int bookingId = dao.addBooking(booking);
            if (bookingId == -1) {
                logger.error("Failed to create booking for {}", userName);
                return false;
            }
            if (!isWaitlisted) {
                List<Integer> seatsToBook = selectedSeats != null && selectedSeats.length > 0
                        ? Arrays.stream(selectedSeats).boxed().toList()
                        : availableSeats.subList(0, requestedSeats);
                for (int seatId : seatsToBook) {
                    dao.updateSeat(seatId, true, bookingId);
                }
            }
            logger.info("Booking {} for {}: {} seats, status={}. Seats: {}", bookingId, userName, requestedSeats, status,
                    selectedSeats != null ? Arrays.toString(selectedSeats) : "Auto-allocated");
            logger.info("Simulated email to {}: Booking {} for {} seats", userName, status, requestedSeats);
            notifyUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Booking failed for {}: {}", userName, e.getMessage(), e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cancels a booking.
     * @param userName User’s name
     * @param seatsToCancel Number of seats to cancel
     * @return true if successful, false otherwise
     */
    public boolean cancelBooking(String userName, int seatsToCancel) {
        lock.lock();
        try {
            if (!isValidUserName(userName) || seatsToCancel <= 0) {
                logger.error("Invalid cancellation request: userName={}, seats={}", userName, seatsToCancel);
                return false;
            }
            BookingDAO dao = new BookingDAO();
            List<Booking> bookings = dao.getBookingHistory();
            for (Booking booking : bookings) {
                if (booking.getUserName().equals(userName) && booking.getStatus().equals("CONFIRMED")) {
                    String sql = "UPDATE bookings SET status = 'CANCELLED' WHERE id = ?";
                    String seatSql = "UPDATE seats SET is_booked = FALSE, booking_id = NULL WHERE booking_id = ?";
                    try (Connection conn = DatabaseConnection.getConnection()) {
                        conn.setAutoCommit(false);
                        try (PreparedStatement stmt = conn.prepareStatement(sql);
                             PreparedStatement stmt2 = conn.prepareStatement(seatSql)) {
                            stmt.setInt(1, booking.getId());
                            stmt.executeUpdate();
                            stmt2.setInt(1, booking.getId());
                            stmt2.executeUpdate();
                            conn.commit();
                            logger.info("Cancelled booking {} for {}: {} seats", booking.getId(), userName, seatsToCancel);
                            processWait();
                            notifyUpdate();
                            return true;
                        } catch (SQLException e) {
                            conn.rollback();
                            throw e;
                        }
                    }
                }
            }
            logger.warn("No booking found for {} to cancel.", userName);
            return false;
        } catch (SQLException e) {
            logger.error("Cancellation failed for {}: {}", userName, e.getMessage(), e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    private boolean isValidUserName(String name) {
        return name != null && !name.isEmpty() && name.length() <= 50 && name.matches("[a-zA-Z0-9 ]+");
    }

    private boolean validateSelectedSeats(int[] selectedSeats, List<Integer> availableSeats, int requestedSeats) {
        if (selectedSeats.length != requestedSeats) return false;
        for (int seatId : selectedSeats) {
            if (!availableSeats.contains(seatId)) return false;
        }
        return true;
    }

    /**
     * Displays the seat map.
     * @return String representation of seat availability
     */
    public String[] displaySeatMap() {
        try {
            BookingDAO dao = new BookingDAO();
            List<Integer> availableSeats = dao.getAvailableSeats(totalSeats);
            String[] seatMap = new String[totalSeats];
            for (int i = 1; i <= totalSeats; i++) {
                seatMap[i - 1] = availableSeats.contains(i) ? "Available" : "Booked";
            }
            logger.debug("Generated seat map display.");
            return seatMap;
        } catch (SQLException e) {
            logger.error("Failed to display seat map: {}", e.getMessage(), e);
            return new String[totalSeats];
        }
    }

    /**
     * Retrieves booking history.
     * @return List of bookings
     */
    public List<Booking> getBookingHistory() {
        try {
            return new BookingDAO().getBookingHistory();
        } catch (SQLException e) {
            logger.error("Failed to retrieve booking history: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves booking statistics.
     * @return Map of status to count
     */
    public Map<String, Integer> getBookingStats() {
        try {
            return new BookingDAO().getBookingStats();
        } catch (SQLException e) {
            logger.error("Failed to retrieve booking stats: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Processes waitlisted bookings when seats become available.
     */
    public void processWait() {
        lock.lock();
        try {
            BookingDAO dao = new BookingDAO();
            List<Booking> bookings = dao.getBookingHistory();
            List<Integer> availableSeats = dao.getAvailableSeats(totalSeats);
            for (Booking booking : bookings) {
                if (booking.getStatus().equals("WAITLISTED") && availableSeats.size() >= booking.getSeatsBooked()) {
                    String sql = "UPDATE bookings SET status = 'CONFIRMED' WHERE id = ?";
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, booking.getId());
                        stmt.executeUpdate();
                    }
                    List<Integer> seatsToBook = availableSeats.subList(0, booking.getSeatsBooked());
                    for (int seatId : seatsToBook) {
                        dao.updateSeat(seatId, true, booking.getId());
                    }
                    availableSeats.subList(0, booking.getSeatsBooked()).clear();
                    logger.info("Processed waitlist booking {} for {}: {} seats", booking.getId(), booking.getUserName(), booking.getSeatsBooked());
                    logger.info("Simulated email to {}: Waitlist booking confirmed for {} seats", booking.getUserName(), booking.getSeatsBooked());
                }
            }
            notifyUpdate();
        } catch (SQLException e) {
            logger.error("Waitlist processing failed: {}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
}
