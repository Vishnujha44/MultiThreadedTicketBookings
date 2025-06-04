package com.railway.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.railway.dao.BookingDAO;
import com.railway.model.Booking;
import com.railway.service.RailwayTicketBookingSystem;
import com.railway.threads.BookingThread;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GUI for the Railway Ticket Booking System.
 */
public class TicketBookingGUI extends JFrame {
    private final RailwayTicketBookingSystem bookingSystem;
    private final JTextField nameField;
    private final JTextField seatsField;
    private final JTextArea outputArea;
    private final JCheckBox[] seatCheckBoxes;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private boolean isProcessing = false;

    /**
     * Constructs the GUI.
     *
     * @param bookingSystem Booking system instance
     * @param totalSeats    Total number of seats
     */
    public TicketBookingGUI(RailwayTicketBookingSystem bookingSystem, int totalSeats) {
        this.bookingSystem = bookingSystem;
        setTitle("Railway Ticket Booking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.add(new JLabel("User Name:"));
        nameField = new JTextField(20);
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Number of Seats:"));
        seatsField = new JTextField(5);
        inputPanel.add(seatsField);

        JPanel seatPanel = new JPanel(new GridLayout(0, 10, 5, 5));
        seatCheckBoxes = new JCheckBox[totalSeats];
        for (int i = 0; i < totalSeats; i++) {
            seatCheckBoxes[i] = new JCheckBox("Seat " + (i + 1));
            seatPanel.add(seatCheckBoxes[i]);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton bookButton = new JButton("Book Tickets");
        JButton cancelButton = new JButton("Cancel Booking");
        JButton seatMapButton = new JButton("View Seat Map");
        JButton historyButton = new JButton("View Booking History");
        JButton statsButton = new JButton("View Booking Stats");
        JButton exportButton = new JButton("Export Bookings");
        buttonPanel.add(bookButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(seatMapButton);
        buttonPanel.add(historyButton);
        buttonPanel.add(statsButton);
        buttonPanel.add(exportButton);

        outputArea = new JTextArea(10, 50);
        outputArea.setEditable(false);
        System.setOut(new TextAreaPrintStream(outputArea));
        JScrollPane outputScroll = new JScrollPane(outputArea);

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(seatPanel), BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(outputScroll, BorderLayout.EAST);
        add(mainPanel);

        bookingSystem.addUpdateListener(this::updateSeatMap);

        bookButton.addActionListener(this::actionPerformed2);

        cancelButton.addActionListener(this::actionPerformed);

        seatMapButton.addActionListener(e -> displaySeatMap());
        historyButton.addActionListener(e -> displayBookingHistory());
        statsButton.addActionListener(e -> displayBookingStats());
        exportButton.addActionListener(e -> exportBookings());

        updateSeatMap();
        setVisible(true);
    }

    private void handleBooking() {
        try {
            String userName = nameField.getText().trim();
            if (!isValidUserName(userName)) {
                JOptionPane.showMessageDialog(this, "User name must be 1-50 characters, letters, numbers, or spaces.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int requestedSeats = Integer.parseInt(seatsField.getText().trim());
            if (requestedSeats <= 0 || requestedSeats > 10) {
                JOptionPane.showMessageDialog(this, "Seats must be between 1 and 10.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int[] selectedSeats = getSelectedSeats();
            if (selectedSeats.length > 0 && selectedSeats.length != requestedSeats) {
                JOptionPane.showMessageDialog(this, "Selected seats must match requested seats.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            executor.submit(() -> {
                try {
                    BookingThread thread = new BookingThread(bookingSystem, userName, requestedSeats, selectedSeats);
                    thread.run();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Booking processed for " + userName + ".", "Success", JOptionPane.INFORMATION_MESSAGE);
                        updateSeatMap();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Booking failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                }
            });
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for seats.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancellation() {
        try {
            String userName = nameField.getText().trim();
            if (!isValidUserName(userName)) {
                JOptionPane.showMessageDialog(this, "User name must be 1-50 characters, letters, numbers, or spaces.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int seatsToCancel = Integer.parseInt(seatsField.getText().trim());
            if (seatsToCancel <= 0 || seatsToCancel > 10) {
                JOptionPane.showMessageDialog(this, "Seats to cancel must be between 1 and 10.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            executor.submit(() -> {
                try {
                    boolean success = bookingSystem.cancelBooking(userName, seatsToCancel);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, success ? "Cancellation successful." : "No booking found to cancel.", "Result", JOptionPane.INFORMATION_MESSAGE);
                        updateSeatMap();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Cancellation failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                }
            });
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for seats.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isValidUserName(String name) {
        return name != null && !name.isEmpty() && name.length() <= 50 && name.matches("[a-zA-Z0-9 ]+");
    }

    private int[] getSelectedSeats() {
        return Arrays.stream(seatCheckBoxes)
                .filter(JCheckBox::isSelected)
                .mapToInt(cb -> Integer.parseInt(cb.getText().replace("Seat ", "")))
                .toArray();
    }

    private void updateSeatMap() {
        String[] seatMap = bookingSystem.displaySeatMap();
        for (int i = 0; i < seatCheckBoxes.length; i++) {
            seatCheckBoxes[i].setEnabled(seatMap[i].equals("Available"));
            seatCheckBoxes[i].setSelected(false);
        }
        outputArea.append("Seat map updated.\n");
    }

    private void displaySeatMap() {
        String[] seatMap = bookingSystem.displaySeatMap();
        outputArea.append("Seat Map:\n");
        for (int i = 0; i < seatMap.length; i++) {
            outputArea.append("Seat " + (i + 1) + ": " + seatMap[i] + "\n");
        }
    }

    private void displayBookingHistory() {
        List<Booking> history = bookingSystem.getBookingHistory();
        outputArea.append("Booking History:\n");
        for (Booking booking : history) {
            outputArea.append(booking.toString() + "\n");
        }
    }

    private void displayBookingStats() {
        Map<String, Integer> stats = bookingSystem.getBookingStats();
        outputArea.append("Booking Statistics:\n");
        stats.forEach((status, count) -> outputArea.append(status + ": " + count + "\n"));
    }

    private void exportBookings() {
        try (PrintWriter writer = new PrintWriter("bookings.csv")) {
            writer.println("ID,User,Seats,Time,Status");
            for (Booking booking : new BookingDAO().getBookingHistory()) {
                writer.println(String.format("%d,%s,%d,%s,%s",
                        booking.getId(), booking.getUserName(), booking.getSeatsBooked(),
                        booking.getBookingTime(), booking.getStatus()));
            }
            JOptionPane.showMessageDialog(this, "Bookings exported to bookings.csv", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void dispose() {
        executor.shutdown();
        super.dispose();
    }

    private void actionPerformed(ActionEvent e) {
        if (isProcessing) return;
        isProcessing = true;
        handleCancellation();
        new Timer(1000, evt -> isProcessing = false).setRepeats(false);
    }

    private void actionPerformed2(ActionEvent e) {
        if (isProcessing) return;
        isProcessing = true;
        handleBooking();
        new Timer(1000, evt -> isProcessing = false).setRepeats(false);
    }
}