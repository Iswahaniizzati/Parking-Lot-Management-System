package com.group5.parkinglot.model;

public class Motorcycle extends Vehicle {

    public Motorcycle(String licensePlate) {
        super(licensePlate, VehicleType.MOTORCYCLE);
    }

    @Override
    public boolean canParkIn(SpotType spotType) {
        // Motorcycles can ONLY park in COMPACT spots
        return spotType == SpotType.COMPACT;
    }
}