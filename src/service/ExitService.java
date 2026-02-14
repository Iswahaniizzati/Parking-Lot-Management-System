package service;

import data.DataStore;
import fine.FineScheme;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import model.*;

public class ExitService {
    private DataStore dataStore;
    private FineScheme activeFineScheme;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ExitService(DataStore dataStore, FineScheme activeFineScheme) {
        this.dataStore = dataStore;
        this.activeFineScheme = activeFineScheme;
    }

    //requirement 3, process vehicle exit
    //calculates duration, applies HC discounts, and checks for fines.
    public PaymentRecord processExit(String plate, boolean hasHcCard) {
        //retrieve active session from database
        ParkingSession session = dataStore.getOpenSessionByPlate(plate);
        if (session == null) return null;

        String exitTime = LocalDateTime.now().format(FORMATTER);

        // 1. Base Fee Calculation
        // Duration Calculation (Ceiling Rounding)
        long hours = calculateHoursCeiling(session.getEntryTime(), exitTime);
        // Requirement 2: Pricing Logic (HC Card Holder gets RM 2/hour)
        double hourlyRate;

        if (hasHcCard) {
            if (session.getSpotId().contains("HC") || session.getSpotId().contains("H")) {
                hourlyRate = 0.0;
            } else {
                hourlyRate = 2.0;
            }
        } else {
            // Default rate for non-HC card holders (or follow spot type rate)
            hourlyRate = 5.0;
        }
        double parkingFee = hours * hourlyRate;

        // 2. Requirement 4: Overstay Fine (>24h)
        double newFineAmount = 0;
        if (hours > 24) {
            newFineAmount = activeFineScheme.calculateFine(hours - 24);
            if (newFineAmount > 0) {
                dataStore.addFine(new FineRecord(plate, "OVERSTAY_24H", newFineAmount, exitTime, false));
            }
        }

        // 3. Requirement 4: Reserved Spot Check
        if (session.getSpotId().contains("RES")) {
            double resFine = 100.0; // Flat fine for illegal reserved parking
            newFineAmount += resFine;
            dataStore.addFine(new FineRecord(plate, "RESERVED_VIOLATION", resFine, exitTime, false));
        }

        // 4. Link fines to License Plate (Requirement 4)
        List<FineRecord> unpaidFines = dataStore.getUnpaidFinesByPlate(plate);
        double totalOldFines = unpaidFines.stream().mapToDouble(FineRecord::getAmount).sum();
        double totalFinesToPay = newFineAmount + totalOldFines;

        // Return record to UI (Requirement 5 breakdown)
        return new PaymentRecord(
            session.getTicketNo(), plate, "PENDING", exitTime,
            parkingFee, totalFinesToPay, parkingFee + totalFinesToPay, 0.0, 0.0
        );
    }

    //Requirement 2: Rounds up to the nearest hour (Ceiling rounding)
    private long calculateHoursCeiling(String startStr, String endStr) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = LocalDateTime.parse(startStr, formatter);
        LocalDateTime end = LocalDateTime.parse(endStr, formatter);

        Duration duration = Duration.between(start, end);
        long totalMinutes = duration.toMinutes();

        return (long) Math.ceil(totalMinutes / 60.0);
    }

    public void finalizeExit(String ticketNo, String spotId, int hours, double fee, String plate) {
        String exitTime = LocalDateTime.now().format(FORMATTER);
        dataStore.setSpotAvailable(spotId);
        dataStore.closeSession(ticketNo, exitTime, hours, fee);
        dataStore.markAllFinesPaid(plate, exitTime);

        System.out.println("Exit finalized for " + plate + ". Fines cleared.");
    }

    public void setActiveFineScheme(FineScheme scheme) { this.activeFineScheme = scheme; }
    public FineScheme getActiveFineScheme() { return activeFineScheme; }
}