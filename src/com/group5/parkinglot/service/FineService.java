package com.group5.parkinglot.service;

import com.group5.parkinglot.strategy.FineStrategy;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles fines: calculation, tracking unpaid fines, marking as paid
 */
public class FineService {

    private FineStrategy strategy;
    private final Map<String, Double> unpaidFines = new HashMap<>(); // licensePlate -> amount

    public FineService(FineStrategy strategy) {
        this.strategy = strategy;
    }

    /** Change the fine calculation strategy */
    public void setStrategy(FineStrategy strategy) {
        this.strategy = strategy;
    }

    /** Calculate fine for overstay hours and record it */
    public double calculateFine(String licensePlate, long overstayHours) {
        double fine = strategy.calculateFine(overstayHours);
        unpaidFines.put(licensePlate, unpaidFines.getOrDefault(licensePlate, 0.0) + fine);
        return fine;
    }

    /** Return unpaid fines for a vehicle */
    public double getUnpaidFines(String licensePlate) {
        return unpaidFines.getOrDefault(licensePlate, 0.0);
    }

    /** Mark fines as paid */
    public void markFinesPaid(String licensePlate) {
        unpaidFines.remove(licensePlate);
    }
}
