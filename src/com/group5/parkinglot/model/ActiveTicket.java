package com.group5.parkinglot.model;

import java.time.LocalDateTime;

public class ActiveTicket {

    private final String ticketId;
    private final String licensePlate;
    private final String vehicleType;
    private final String spotId;
    private final LocalDateTime entryTime;

    public ActiveTicket(String ticketId, String licensePlate, String vehicleType, String spotId, LocalDateTime entryTime) {
        this.ticketId = ticketId;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.spotId = spotId;
        this.entryTime = entryTime;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getSpotId() {
        return spotId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public long calculateHours(LocalDateTime exitTime) {
        long minutes = java.time.Duration.between(entryTime, exitTime).toMinutes();
        return (minutes + 59) / 60;  // ceiling to next hour
    }

    @Override
    public String toString() {
        return String.format("Ticket[%s]: %s (%s) in %s since %s",
                ticketId, licensePlate, vehicleType, spotId, entryTime);
    }
}
