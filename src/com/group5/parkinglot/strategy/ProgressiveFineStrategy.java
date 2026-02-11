package com.group5.parkinglot.strategy;

public class ProgressiveFineStrategy implements FineStrategy {

    @Override
    public double calculateFine(long overstayHours) {
        if (overstayHours <= 0) {
            return 0.0;
        }

        double fine = 0.0;

        // First 24 hours over → RM 50
        if (overstayHours > 0) {
            fine += 50.0;
        }

        // Hours 24–48 → additional RM 100
        if (overstayHours > 24) {
            fine += 100.0;
        }

        // Hours 48–72 → additional RM 150
        if (overstayHours > 48) {
            fine += 150.0;
        }

        // Above 72 hours → additional RM 200
        if (overstayHours > 72) {
            fine += 200.0;
        }

        return fine;
    }

    @Override
    public String getStrategyName() {
        return "Progressive Fine Scheme";
    }
}