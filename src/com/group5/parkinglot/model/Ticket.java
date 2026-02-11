package com.group5.parkinglot.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class Ticket {

    private String ticketId;
    private String licensePlate;
    private String vehicleType;
    private String spotId;
    private LocalDateTime entryTime;

    public Ticket(String ticketId, String licensePlate, String vehicleType, String spotId, LocalDateTime entryTime) {
        this.ticketId = ticketId;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.spotId = spotId;
        this.entryTime = entryTime;
    }

    public long calculateHours(LocalDateTime exitTime) {
        long minutes = Duration.between(entryTime, exitTime).toMinutes();
        return (minutes + 59) / 60;  // ceiling to next hour
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getSpotId() {
        return spotId;
    }

    // ──── Add this missing getter ────
    public String getTicketId() {
        return ticketId;
    }

    // Optional: useful for display / receipt
    public String getVehicleType() {
        return vehicleType;
    }

    // Optional: full info string for printing receipt
    @Override
    public String toString() {
        return "Ticket{" +
               "ticketId='" + ticketId + '\'' +
               ", licensePlate='" + licensePlate + '\'' +
               ", vehicleType='" + vehicleType + '\'' +
               ", spotId='" + spotId + '\'' +
               ", entryTime=" + entryTime +
               '}';
    }
}