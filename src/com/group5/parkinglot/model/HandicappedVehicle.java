package com.group5.parkinglot.model;

public class HandicappedVehicle extends Vehicle {

    private final boolean hasHandicapCard;

    public HandicappedVehicle(String licensePlate, boolean hasHandicapCard) {
        super(licensePlate, VehicleType.HANDICAPPED_VEHICLE);
        this.hasHandicapCard = hasHandicapCard;
    }

    @Override
    public boolean canParkIn(SpotType spotType) {
        // Handicapped vehicles can park in ANY spot type
        return true;
    }

    // Special rate logic (as per assignment)
    @Override
    public double getEffectiveRate(SpotType spotType) {
        if (hasHandicapCard && spotType == SpotType.HANDICAPPED) {
            return 0.0;  // FREE if parking in handicapped spot with valid card
        }
        return 2.0;      // Otherwise RM 2/hour (as per requirement)
    }

    public boolean hasHandicapCard() {
        return hasHandicapCard;
    }
}