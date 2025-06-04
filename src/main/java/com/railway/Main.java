package com.railway;

import com.railway.model.Booking;
import com.railway.service.RailwayTicketBookingSystem;
import com.railway.ui.TicketBookingGUI;
import com.railway.threads.RandomBookingThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

/**
 * Entry point for the Railway Ticket Booking System.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Railway Ticket Booking System ===");
        System.out.print("Run in GUI mode? (y/n): ");
        String mode = scanner.nextLine().trim().toLowerCase();
        int totalSeats;
        try {
            Properties props = new Properties();
            try (InputStream input = Main.class.getResourceAsStream("/db.properties")) {
                if (input == null) {
                    throw new IOException("Unable to find db.properties");
                }
                props.load(input);
                totalSeats = Integer.parseInt(props.getProperty("total.seats", "20"));
                if (totalSeats <= 0 || totalSeats > 1000) {
                    throw new IllegalArgumentException("Total seats must be between 1 and 1000");
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Error loading total seats, defaulting to 20: {}", e.getMessage(), e);
            totalSeats = 20;
        }
        RailwayTicketBookingSystem bookingSystem = new RailwayTicketBookingSystem(totalSeats);

        if (mode.equals("y")) {
            new TicketBookingGUI(bookingSystem, totalSeats);
            new RandomBookingThread(bookingSystem).start();
        } else {
            runConsoleMode(bookingSystem, scanner);
        }
    }

    private static void runConsoleMode(RailwayTicketBookingSystem bookingSystem, Scanner scanner) {
        while (true) {
            System.out.println("\n1. Book Tickets");
            System.out.println("2. Cancel Booking");
            System.out.println("3. Display Seat Map");
            System.out.println("4. Display Booking History");
            System.out.println("5. Display Booking Stats");
            System.out.println("6. Export Bookings");
            System.out.println("7. Start Random Booking");
            System.out.println("8. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> {
                        System.out.print("Enter user name: ");
                        String userName = scanner.nextLine().trim();
                        if (!isValidUserName(userName)) {
                            System.out.println("Error: User name must be 1-50 characters, letters, numbers, or spaces.");
                            continue;
                        }
                        System.out.print("Enter number of seats: ");
                        int seats = Integer.parseInt(scanner.nextLine().trim());
                        if (seats <= 0 || seats > 10) {
                            System.out.println("Error: Seats must be between 1 and 10.");
                            continue;
                        }
                        boolean success = bookingSystem.bookTicket(userName, seats, null);
                        System.out.println(success ? "Booking successful." : "Booking failed.");
                    }
                    case "2" -> {
                        System.out.print("Enter user name: ");
                        String userName = scanner.nextLine().trim();
                        if (!isValidUserName(userName)) {
                            System.out.println("Error: User name must be 1-50 characters, letters, numbers, or spaces.");
                            continue;
                        }
                        System.out.print("Enter number of seats to cancel: ");
                        int seats = Integer.parseInt(scanner.nextLine().trim());
                        if (seats <= 0 || seats > 10) {
                            System.out.println("Error: Seats must be between 1 and 10.");
                            continue;
                        }
                        boolean success = bookingSystem.cancelBooking(userName, seats);
                        System.out.println(success ? "Cancellation successful." : "No booking found to cancel.");
                    }
                    case "3" -> {
                        String[] seatMap = bookingSystem.displaySeatMap();
                        for (int i = 0; i < seatMap.length; i++) {
                            System.out.println("Seat " + (i + 1) + ": " + seatMap[i]);
                        }
                    }
                    case "4" -> {
                        for (Booking booking : bookingSystem.getBookingHistory()) {
                            System.out.println(booking);
                        }
                    }
                    case "5" -> {
                        Map<String, Integer> stats = bookingSystem.getBookingStats();
                        stats.forEach((status, count) -> System.out.println(status + ": " + count));
                    }
                    case "6" -> {
                        try (PrintWriter writer = new PrintWriter("bookings.csv")) {
                            writer.println("ID,User,Seats,Time,Status");
                            for (Booking booking : bookingSystem.getBookingHistory()) {
                                writer.println(String.format("%d,%s,%d,%s,%s",
                                        booking.getId(), booking.getUserName(), booking.getSeatsBooked(),
                                        booking.getBookingTime(), booking.getStatus()));
                            }
                            System.out.println("Bookings exported to bookings.csv");
                        }
                    }
                    case "7" -> new RandomBookingThread(bookingSystem).start();
                    case "8" -> {
                        System.out.println("Exiting...");
                        scanner.close();
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static boolean isValidUserName(String name) {
        return name != null && !name.isEmpty() && name.length() <= 50 && name.matches("[a-zA-Z0-9 ]+");
    }
}
