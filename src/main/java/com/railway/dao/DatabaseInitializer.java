package com.railway.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes the railway database and tables.
 */
public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    /**
     * Initializes the database with schema and seats.
     * @param totalSeats Number of seats to initialize
     */
    public static void initializeDatabase(int totalSeats) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS railway_db");
            stmt.execute("USE railway_db");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS bookings (" +
                            "id INT PRIMARY KEY AUTO_INCREMENT, " +
                            "user_name VARCHAR(50) NOT NULL, " +
                            "seats_booked INT NOT NULL, " +
                            "booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "status ENUM('CONFIRMED', 'WAITLISTED', 'CANCELLED') DEFAULT 'CONFIRMED')"
            );
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS seats (" +
                            "seat_id INT PRIMARY KEY, " +
                            "is_booked BOOLEAN DEFAULT FALSE, " +
                            "booking_id INT, " +
                            "FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL)"
            );
            for (int i = 1; i <= totalSeats; i++) {
                stmt.execute("INSERT IGNORE INTO seats (seat_id, is_booked) VALUES (" + i + ", FALSE)");
            }
            logger.info("Database initialized with {} seats.", totalSeats);
        } catch (SQLException e) {
            logger.error("Database initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
