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

    public void processExit(String plate, String exitTimeISO) {
        // 1. Get the current session
        ParkingSession session = dataStore.getOpenSessionByPlate(plate);
        if (session == null) {
            System.out.println("No active session found for plate: " + plate);
            return;
        }

        // 2. Calculate Duration and Base Parking Fee
        long hours = calculateHours(session.getEntryTime(), exitTimeISO);
        // Assume a default rate or fetch from spot if your model allows
        double hourlyRate = 5.0; 
        double parkingFee = hours * hourlyRate;

        // 3. Check for NEW Fines (Overstay > 24h)
        double newFineAmount = 0;
        if (hours > 24) {
            long overstayHours = hours - 24;
            newFineAmount = activeFineScheme.calculateFine(overstayHours);
            
            if (newFineAmount > 0) {
                FineRecord overstayFine = new FineRecord(plate, "OVERSTAY_24H", newFineAmount, exitTimeISO, false);
                dataStore.addFine(overstayFine);
            }
        }

        // 4. Check for OLD Unpaid Fines
        List<FineRecord> unpaidFines = dataStore.getUnpaidFines(plate);
        double totalOldFines = unpaidFines.stream().mapToDouble(FineRecord::getAmount).sum();

        // 5. Grand Total
        double totalDue = parkingFee + newFineAmount + totalOldFines;

        // 6. Display Summary (This would be passed to your UI/MainFrame)
        displaySummary(plate, hours, parkingFee, newFineAmount, totalOldFines, totalDue);
        
        // Next: Call PaymentProcessor to handle the RM...
    }

    private long calculateHours(String startISO, String endISO) {
        ZonedDateTime start = ZonedDateTime.parse(startISO);
        ZonedDateTime end = ZonedDateTime.parse(endISO);
        Duration duration = Duration.between(start, end);
        return Math.max(1, duration.toHours()); // Minimum 1 hour charge
    }

    private void displaySummary(String plate, long hrs, double fee, double newFine, double oldFine, double total) {
        System.out.println("\n--- EXIT BILLING: " + plate + " ---");
        System.out.println("Duration: " + hrs + " hours");
        System.out.println("Parking Fee: RM " + fee);
        System.out.println("New Fines: RM " + newFine);
        System.out.println("Unpaid Previous Fines: RM " + oldFine);
        System.out.println("TOTAL DUE: RM " + total);
        System.out.println("--------------------------------\n");
    }

    // Inside ExitService.java
    public void setActiveFineScheme(FineScheme activeFineScheme) {
        this.activeFineScheme = activeFineScheme;
    }

    public FineScheme getActiveFineScheme() {
        return activeFineScheme;
    }
}