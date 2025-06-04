package com.railway.threads;

import com.railway.service.RailwayTicketBookingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Thread for simulating random bookings.
 */
public class RandomBookingThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(RandomBookingThread.class);
    private final RailwayTicketBookingSystem bookingSystem;

    /**
     * Constructs a random booking thread.
     * @param bookingSystem Booking system instance
     */
    public RandomBookingThread(RailwayTicketBookingSystem bookingSystem) {
        this.bookingSystem = bookingSystem;
    }

    @Override
    public void run() {
        Random rand = new Random();
        String[] users = {"Alice", "Bob", "Charlie", "David", "Eve"};
        try {
            for (int i = 0; i < 5; i++) {
                String userName = users[rand.nextInt(users.length)];
                int seats = rand.nextInt(3) + 1;
                bookingSystem.bookTicket(userName, seats, null);
                logger.info("Random booking: {} booked {} seats.", userName, seats);
                Thread.sleep(rand.nextInt(2000));
            }
        } catch (InterruptedException e) {
            logger.error("Random booking interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}

