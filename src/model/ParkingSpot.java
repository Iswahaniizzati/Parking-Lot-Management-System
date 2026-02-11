package model;

import enums.SpotType;
import enums.SpotStatus;

//represents a single parking space in the parking lot

public class ParkingSpot {

    private String spotId;
    private SpotType type;
    private SpotStatus status;

    // For now we keep vehicle as String 
    // Member 2 replace with Vehicle objec
    private String currentVehiclePlate;

    // Constructor
    public ParkingSpot(String spotId, SpotType type) {
        this.spotId = spotId;
        this.type = type;
        this.status = SpotStatus.AVAILABLE;  // default when created
        this.currentVehiclePlate = null;
    }

    // Getter methods
    public String getSpotId() {
        return spotId;
    }

    public SpotType getType() {
        return type;
    }

    public SpotStatus getStatus() {
        return status;
    }

    public String getCurrentVehiclePlate() {
        return currentVehiclePlate;
    }

    public double getHourlyRate() {
        return type.getHourlyRate(); //get rate from enum
    }

    // Check if spot is available
    public boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }

    // Occupy the spot
    public void occupy(String plateNumber) {
        this.status = SpotStatus.OCCUPIED;
        this.currentVehiclePlate = plateNumber;
    }

    // Release the spot
    public void release() {
        this.status = SpotStatus.AVAILABLE;
        this.currentVehiclePlate = null;
    }
}
