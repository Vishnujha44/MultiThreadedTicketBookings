package com.railway.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages database connections using JDBC.
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String PROPERTIES_FILE = "/db.properties";
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    static {
        Properties props = new Properties();
        try (InputStream input = DatabaseConnection.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new IOException("Unable to find " + PROPERTIES_FILE);
            }
            props.load(input);
            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");
            logger.info("Database properties loaded successfully.");
        } catch (IOException e) {
            logger.error("Failed to load database properties: {}", e.getMessage(), e);
            throw new RuntimeException("Database configuration failed", e);
        }
    }

    /**
     * Gets a database connection with retry logic.
     * @return Connection object
     * @throws SQLException if connection fails after retries
     */
    public static Connection getConnection() throws SQLException {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                logger.debug("Database connection established.");
                return conn;
            } catch (SQLException e) {
                attempts++;
                logger.warn("Connection attempt {}/{} failed: {}", attempts, MAX_RETRIES, e.getMessage());
                if (attempts == MAX_RETRIES) {
                    logger.error("Failed to connect after {} attempts.", MAX_RETRIES, e);
                    throw e;
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted during retry.", ie);
                    throw new SQLException("Interrupted during retry", ie);
                }
            }
        }
        throw new SQLException("Failed to connect after " + MAX_RETRIES + " attempts");
    }
}