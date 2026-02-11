package com.group5.parkinglot.model;

public enum SpotType {
    COMPACT(2.0),
    REGULAR(5.0),
    HANDICAPPED(2.0),
    RESERVED(10.0);

    private final double hourlyRate;

    SpotType(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }
}