package com.group5.parkinglot.service;

import com.group5.parkinglot.model.*;
import com.group5.parkinglot.strategy.FineStrategy;
import java.util.List;
import java.util.Map;

public class ParkingService {

    private final ParkingLot parkingLot;

    public ParkingService() {
        parkingLot = ParkingLot.getInstance();
    }

    public ActiveTicket parkVehicle(Vehicle vehicle) {
        ParkingSpot spot = parkingLot.findAvailableSpot(vehicle);
        if (spot == null) throw new IllegalStateException("No available spots!");
        return parkingLot.parkVehicle(vehicle, spot);
    }

    public Map<String, ActiveTicket> getActiveTickets() {
        return parkingLot.getActiveTickets();
    }

    public ParkingSpot findAvailableSpot(Vehicle vehicle) {
        return parkingLot.findAvailableSpot(vehicle);
    }

    public ActiveTicket parkVehicle(Vehicle vehicle, ParkingSpot spot) {
        return parkingLot.parkVehicle(vehicle, spot);
    }

    public double exitVehicle(String licensePlate, String paymentMethod) {
        return parkingLot.exitVehicle(licensePlate, paymentMethod);
    }

    public int getTotalSpots() {
        return parkingLot.getTotalSpots();
    }

    public int getTotalOccupiedSpots() {
        return parkingLot.getTotalOccupiedSpots();
    }

    public double getOccupancyPercentage() {
        return parkingLot.getOccupancyPercentage();
    }

    public double getTotalRevenue() {
        return parkingLot.getTotalRevenue();
    }

    public void setFineStrategy(FineStrategy strategy) {
        parkingLot.setFineStrategy(strategy);
    }

    public List<Floor> getFloors() {
        return parkingLot.getFloors();
    }
}
