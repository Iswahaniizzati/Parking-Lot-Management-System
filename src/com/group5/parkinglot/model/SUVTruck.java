package com.group5.parkinglot.model;

public class SUVTruck extends Vehicle {

    public SUVTruck(String licensePlate) {
        super(licensePlate, VehicleType.SUV_TRUCK);
    }

    @Override
    public boolean canParkIn(SpotType spotType) {
        // SUVs/Trucks can ONLY park in REGULAR spots
        return spotType == SpotType.REGULAR;
    }
}