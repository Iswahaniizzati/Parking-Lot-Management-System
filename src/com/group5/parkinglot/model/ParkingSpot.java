package com.group5.parkinglot.model;

import java.time.LocalDateTime;

public class ParkingSpot {

    private final String spotId;          // e.g. "F1-R2-S15"
    private final int floorNumber;
    private final int rowNumber;
    private final int spotNumber;
    private final SpotType type;
    private final double hourlyRate;

    private boolean available = true;
    private Vehicle currentVehicle = null;

    /**
     * Constructor - creates a new parking spot
     */
    public ParkingSpot(int floor, int row, int number, SpotType type) {
        this.floorNumber = floor;
        this.rowNumber = row;
        this.spotNumber = number;
        this.type = type;
        this.hourlyRate = type.getHourlyRate();

        // Generate formatted ID: F1-R2-S15
        this.spotId = String.format("F%d-R%d-S%02d", floor, row, number);
    }

    // Getters
    public String getSpotId() {
        return spotId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public int getSpotNumber() {
        return spotNumber;
    }

    public SpotType getType() {
        return type;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public boolean isAvailable() {
        return available;
    }

    public Vehicle getCurrentVehicle() {
        return currentVehicle;
    }

    /**
     * Check if this spot can accommodate the given vehicle
     * (uses vehicle's polymorphic canParkIn method)
     */
    public boolean canFit(Vehicle vehicle) {
        return vehicle.canParkIn(type);
    }

    /**
     * Assign a vehicle to this spot
     * @return true if successful, false if spot occupied or vehicle cannot fit
     */
    public boolean assignVehicle(Vehicle vehicle) {
        if (!available) {
            return false;
        }

        if (!canFit(vehicle)) {
            return false;
        }

        this.currentVehicle = vehicle;
        this.available = false;
        vehicle.setEntryTime(LocalDateTime.now());  // record entry time

        return true;
    }

    /**
     * Remove the parked vehicle and free the spot
     * @return the removed vehicle (or null if spot was empty)
     */
    public Vehicle removeVehicle() {
        if (available) {
            return null;
        }

        Vehicle removed = this.currentVehicle;
        this.currentVehicle = null;
        this.available = true;

        return removed;
    }

    /**
     * Get the current parking fee for this spot (useful for display or calculation)
     */
    public double getCurrentRate() {
        if (currentVehicle == null) {
            return hourlyRate;
        }

        // Handicapped vehicle may override rate
        return currentVehicle.getEffectiveRate(type);
    }

    @Override
    public String toString() {
        String status = available ? "Available" : "Occupied by " + (currentVehicle != null ? currentVehicle : "Unknown");
        return spotId + " (" + type + ") - " + status + " @ RM" + String.format("%.2f/hr", hourlyRate);
    }
}