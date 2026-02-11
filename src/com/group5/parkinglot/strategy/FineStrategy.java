package com.group5.parkinglot.strategy;

/**
 * Strategy interface for calculating fines.
 */
public interface FineStrategy {
    double calculateFine(long overstayHours);
    String getStrategyName();
}
