package com.group5.parkinglot.model;

import java.time.LocalDateTime;

public abstract class Vehicle {

    private String licensePlate;
    private VehicleType type;
    private LocalDateTime entryTime;

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    // Core polymorphic method - each subclass decides which spot types it can use
    public abstract boolean canParkIn(SpotType spotType);

    // Common behavior
    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleType getType() {
        return type;
    }

    // Optional: override in HandicappedVehicle if needed for special rate logic
    public double getEffectiveRate(SpotType spotType) {
        return spotType.getHourlyRate();
    }

    @Override
    public String toString() {
        return type + " [" + licensePlate + "]";
    }
}