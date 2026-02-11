package com.group5.parkinglot.strategy;

/**
 * Fixed fine strategy: RM10 per overstay hour
 */
public class FixedFineStrategy implements FineStrategy {

    @Override
    public double calculateFine(long overstayHours) {
        return overstayHours * 10; // RM10 per hour
    }

    @Override
    public String getStrategyName() {
        return "Fixed Fine Strategy";
    }
}
