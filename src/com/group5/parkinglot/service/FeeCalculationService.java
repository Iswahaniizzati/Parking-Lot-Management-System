package com.group5.parkinglot.service;

import com.group5.parkinglot.model.ActiveTicket;
import com.group5.parkinglot.model.ParkingSpot;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Calculates parking fees for active tickets.
 * Fee is based on spot rate × hours parked (rounded up to next hour).
 */
public class FeeCalculationService {

    /**
     * Calculate total parking fee for a ticket.
     *
     * @param ticket the active ticket
     * @param spot the parking spot
     * @return fee in RM
     */
    public double calculateFee(ActiveTicket ticket, ParkingSpot spot) {
        if (ticket == null || spot == null) return 0.0;

        long hours = calculateHours(ticket.getEntryTime(), LocalDateTime.now());
        double rate = spot.getCurrentRate(); // RM per hour

        return hours * rate;
    }

    /**
     * Utility: calculate hours between two times (ceiling to next hour)
     */
    public long calculateHours(LocalDateTime start, LocalDateTime end) {
        long minutes = ChronoUnit.MINUTES.between(start, end);
        return (minutes + 59) / 60; // round up to next hour
    }
}
