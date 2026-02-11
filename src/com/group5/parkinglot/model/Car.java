package com.group5.parkinglot.model;

public class Car extends Vehicle {

    public Car(String licensePlate) {
        super(licensePlate, VehicleType.CAR);
    }

    @Override
    public boolean canParkIn(SpotType spotType) {
        // Regular cars can park in COMPACT or REGULAR spots
        return spotType == SpotType.COMPACT || spotType == SpotType.REGULAR;
    }
}