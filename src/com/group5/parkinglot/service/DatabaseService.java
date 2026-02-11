package com.group5.parkinglot.service;

import com.group5.parkinglot.model.ActiveTicket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {

    private final List<ActiveTicket> activeTicketsDb = new ArrayList<>();

    // Initialize DB (mock)
    public void initializeDatabase() {
        System.out.println("Database initialized.");
    }

    public void saveActiveTicket(ActiveTicket ticket) {
        activeTicketsDb.add(ticket);
        System.out.println("Saved ticket to DB: " + ticket);
    }

    public List<ActiveTicket> loadActiveTickets() {
        // Return copy to prevent external modification
        return new ArrayList<>(activeTicketsDb);
    }

    public void removeActiveTicket(String licensePlate) {
        activeTicketsDb.removeIf(t -> t.getLicensePlate().equals(licensePlate));
    }

    // Optional: fines DB can be separate
    public void saveFine(String licensePlate, double amount, String reason, LocalDateTime time) {
        System.out.printf("Saved fine: %s → RM%.2f (%s)\n", licensePlate, amount, reason);
    }

    public double getUnpaidFines(String licensePlate) {
        return 0.0; // mock for now
    }

    public void markFinesPaid(String licensePlate) {
        System.out.println("Fines marked paid for " + licensePlate);
    }
}
