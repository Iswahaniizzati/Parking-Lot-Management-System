package com.group5.parkinglot.strategy;

public class HourlyFineStrategy implements FineStrategy {

    private static final double RATE_PER_HOUR = 20.0;

    @Override
    public double calculateFine(long overstayHours) {
        if (overstayHours <= 0) {
            return 0.0;
        }
        // RM 20 per hour for every hour over 24 hours
        return overstayHours * RATE_PER_HOUR;
    }

    @Override
    public String getStrategyName() {
        return "Hourly Fine (RM 20/hour)";
    }
}