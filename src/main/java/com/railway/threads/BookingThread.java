package com.railway.threads;

import com.railway.service.RailwayTicketBookingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread for processing a booking request.
 */
public class BookingThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(BookingThread.class);
    private final RailwayTicketBookingSystem bookingSystem;
    private final String userName;
    private final int requestedSeats;
    private final int[] selectedSeats;

    /**
     * Constructs a booking thread.
     * @param bookingSystem Booking system instance
     * @param userName User name
     * @param requestedSeats Number of seats
     * @param selectedSeats Specific seats (optional)
     */
    public BookingThread(RailwayTicketBookingSystem bookingSystem, String userName, int requestedSeats, int[] selectedSeats) {
        this.bookingSystem = bookingSystem;
        this.userName = userName;
        this.requestedSeats = requestedSeats;
        this.selectedSeats = selectedSeats != null ? selectedSeats.clone() : null;
    }

    @Override
    public void run() {
        try {
            boolean success = bookingSystem.bookTicket(userName, requestedSeats, selectedSeats);
            if (success) {
                logger.info("Booking thread completed for {}: {} seats.", userName, requestedSeats);
            } else {
                logger.warn("Booking thread failed for {}: {} seats.", userName, requestedSeats);
            }
        } catch (Exception e) {
            logger.error("Booking thread error for {}: {}", userName, e.getMessage(), e);
        }
    }
}