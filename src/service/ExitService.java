package service;

import data.DataStore;
import fine.FineScheme;
import java.time.*;
import java.util.List;
import model.*;

public class ExitService {
    private DataStore dataStore;
    private FineScheme activeFineScheme;

    public ExitService(DataStore dataStore, FineScheme activeFineScheme) {
        this.dataStore = dataStore;
        this.activeFineScheme = activeFineScheme;
    }

    public PaymentRecord processExit(String plate, String exitTimeISO) {
        ParkingSession session = dataStore.getOpenSessionByPlate(plate);
        if (session == null) return null;

        // 1. Base Fee Calculation
        long hours = calculateHours(session.getEntryTime(), exitTimeISO);
        double hourlyRate = 5.0; 
        double parkingFee = hours * hourlyRate;

        // 2. Requirement 4: Overstay Fine (>24h)
        double newFineAmount = 0;
        if (hours > 24) {
            newFineAmount = activeFineScheme.calculateFine(hours - 24);
            if (newFineAmount > 0) {
                dataStore.addFine(new FineRecord(plate, "OVERSTAY_24H", newFineAmount, exitTimeISO, false));
            }
        }

        // 3. Requirement 4: Reserved Spot Check
        if (session.getSpotId().contains("RES")) {
            double resFine = 100.0; // Flat fine for illegal reserved parking
            newFineAmount += resFine;
            dataStore.addFine(new FineRecord(plate, "RESERVED_VIOLATION", resFine, exitTimeISO, false));
        }

        // 4. Link fines to License Plate (Requirement 4)
        List<FineRecord> unpaidFines = dataStore.getUnpaidFinesByPlate(plate);
        double totalOldFines = unpaidFines.stream().mapToDouble(FineRecord::getAmount).sum();
        double totalFinesToPay = newFineAmount + totalOldFines;

        // Return record to UI (Requirement 5 breakdown)
        return new PaymentRecord(
            session.getTicketNo(), plate, "PENDING", exitTimeISO,
            parkingFee, totalFinesToPay, parkingFee + totalFinesToPay, 0.0, 0.0
        );
    }

    private long calculateHours(String startISO, String endISO) {
        ZonedDateTime start = ZonedDateTime.parse(startISO);
        ZonedDateTime end = ZonedDateTime.parse(endISO);
        return Math.max(1, Duration.between(start, end).toHours());
    }

    public void setActiveFineScheme(FineScheme scheme) { this.activeFineScheme = scheme; }
    public FineScheme getActiveFineScheme() { return activeFineScheme; }
}