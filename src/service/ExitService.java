package service;

import data.DataStore;
import enums.FineReason;
import fine.FineScheme;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import model.*;

public class ExitService {
    private DataStore dataStore;
    private FineScheme activeFineScheme;

    public ExitService(DataStore dataStore, FineScheme activeFineScheme) {
        this.dataStore = dataStore;
        this.activeFineScheme = activeFineScheme;
    }

    /**
     * Calculates parking fee + potential new fines + existing unpaid fines.
     * DOES NOT write anything to the store (no new FineRecords created).
     * Used for showing amount due during search/preview.
     */
    public PaymentRecord calculateExitPreview(String plate, String exitTimeISO) {
        ParkingSession session = dataStore.getOpenSessionByPlate(plate);
        if (session == null) {
            return null;
        }

        long hours = calculateHours(session.getEntryTime(), exitTimeISO);

        ParkingSpot spot = dataStore.getAllSpots().stream()
                .filter(s -> s.getSpotId().equals(session.getSpotId()))
                .findFirst().orElse(null);

        double hourlyRate = (spot != null) ? spot.getHourlyRate() : 5.0;
        double parkingFee = hours * hourlyRate;

        double newFines = 0.0;

        // Preview overstay fine
        if (hours > 24) {
            newFines += activeFineScheme.calculateFine(hours - 24);
        }

        // Preview reserved spot violation
        if (session.getSpotId().contains("RES")) {
            newFines += 100.0;
        }

        // Existing unpaid fines (already in DB)
        List<FineRecord> unpaid = dataStore.getUnpaidFinesByPlate(plate);
        double existingFines = unpaid.stream().mapToDouble(FineRecord::getAmount).sum();

        double totalFines = newFines + existingFines;
        double totalDue = parkingFee + totalFines;

        return new PaymentRecord(
                session.getTicketNo(),
                plate,
                "PREVIEW",
                exitTimeISO,
                parkingFee,
                totalFines,
                totalDue,
                0.0,
                0.0
        );
    }

    /**
     * Final exit processing: calculates + **commits** new fines + returns final record.
     * Call this ONLY when full payment is confirmed and vehicle is about to exit.
     */
    public PaymentRecord processExit(String plate, String exitTimeISO) {
        ParkingSession session = dataStore.getOpenSessionByPlate(plate);
        if (session == null) {
            return null;
        }

        long hours = calculateHours(session.getEntryTime(), exitTimeISO);

        ParkingSpot spot = dataStore.getAllSpots().stream()
                .filter(s -> s.getSpotId().equals(session.getSpotId()))
                .findFirst().orElse(null);

        double hourlyRate = (spot != null) ? spot.getHourlyRate() : 5.0;
        double parkingFee = hours * hourlyRate;

        double newFines = 0.0;

        // Commit overstay fine
        if (hours > 24) {
            double overstay = activeFineScheme.calculateFine(hours - 24);
            if (overstay > 0) {
                newFines += overstay;
                dataStore.addFine(new FineRecord(
                        plate, FineReason.OVERSTAY_24H, overstay, exitTimeISO, false));
            }
        }

        // Commit reserved violation
        if (session.getSpotId().contains("RES")) {
            double reservedFine = 100.0;
            newFines += reservedFine;
            dataStore.addFine(new FineRecord(
                    plate, FineReason.RESERVED_VIOLATION, reservedFine, exitTimeISO, false));
        }

        List<FineRecord> unpaid = dataStore.getUnpaidFinesByPlate(plate);
        double existingFines = unpaid.stream().mapToDouble(FineRecord::getAmount).sum();

        double totalFines = newFines + existingFines;
        double totalDue = parkingFee + totalFines;

        return new PaymentRecord(
                session.getTicketNo(),
                plate,
                "PENDING",
                exitTimeISO,
                parkingFee,
                totalFines,
                totalDue,
                0.0,
                0.0
        );
    }

    // In ExitService.java
    public long calculateHours(String entryTimeStr, String exitTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            LocalDateTime entry = LocalDateTime.parse(entryTimeStr, formatter);
            LocalDateTime exit  = LocalDateTime.parse(exitTimeStr, formatter);

            return Math.max(1, Duration.between(entry, exit).toHours());
        } catch (DateTimeParseException e) {
            // Fallback or log — but shouldn't happen with new sessions
            System.err.println("Time parse failed: " + e.getMessage());
            throw e;  // or return -1 / handle gracefully
        }
    }

    public void setActiveFineScheme(FineScheme scheme) {
        this.activeFineScheme = scheme;
    }
    public FineScheme getActiveFineScheme() { return activeFineScheme; }
}
