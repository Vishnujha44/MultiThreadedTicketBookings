package com.railway.model;

import java.time.LocalDateTime;


public class Booking {
    private final int id;
    private final String userName;
    private final int seatsBooked;
    private final LocalDateTime bookingTime;
    private final String status;


    public Booking(int id, String userName, int seatsBooked, LocalDateTime bookingTime, String status) {
        this.id = id;
        this.userName = userName;
        this.seatsBooked = seatsBooked;
        this.bookingTime = bookingTime;
        this.status = status;
    }

    // Getters and setters
    public int getId() { return id; }
    public String getUserName() { return userName; }
    public int getSeatsBooked() { return seatsBooked; }
    public LocalDateTime getBookingTime() { return bookingTime; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return String.format("Booking{id=%d, user='%s', seats=%d, time=%s, status='%s'}",
                id, userName, seatsBooked, bookingTime, status);
    }
}
