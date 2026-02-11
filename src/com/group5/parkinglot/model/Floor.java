package com.group5.parkinglot.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Floor {

    private final int floorNumber;
    private final List<ParkingSpot> spots = new ArrayList<>();

    public Floor(int floorNumber, int spotsPerRow, int numRows) {
        this.floorNumber = floorNumber;

        // Automatically generate spots for this floor
        // Example: 4 rows × (spotsPerRow), with mixed types
        // You can adjust the distribution logic as needed
        int spotCounter = 1;
        for (int row = 1; row <= numRows; row++) {
            for (int i = 1; i <= spotsPerRow; i++) {
                SpotType type;

                // Simple distribution example – you can change this pattern
                if (spotCounter % 10 == 1) {
                    type = SpotType.HANDICAPPED;     // every 10th spot handicapped
                } else if (spotCounter % 7 == 0) {
                    type = SpotType.RESERVED;        // every 7th reserved
                } else if (spotCounter % 3 == 0) {
                    type = SpotType.COMPACT;         // every 3rd compact
                } else {
                    type = SpotType.REGULAR;
                }

                ParkingSpot spot = new ParkingSpot(floorNumber, row, i, type);
                spots.add(spot);
                spotCounter++;
            }
        }
    }

    // Alternative constructor – if you want to pass pre-defined spots
    public Floor(int floorNumber, List<ParkingSpot> predefinedSpots) {
        this.floorNumber = floorNumber;
        this.spots.addAll(predefinedSpots);
    }

    // ---------------- Getters ----------------

    public int getFloorNumber() {
        return floorNumber;
    }

    public List<ParkingSpot> getSpots() {
        return new ArrayList<>(spots); // defensive copy
    }

    public int getTotalSpots() {
        return spots.size();
    }

    public int getOccupiedSpots() {
        int count = 0;
        for (ParkingSpot spot : spots) {
            if (!spot.isAvailable()) {
                count++;
            }
        }
        return count;
    }

    public double getOccupancyPercentage() {
        int total = getTotalSpots();
        if (total == 0) return 0.0;
        return (getOccupiedSpots() * 100.0) / total;
    }

    // ---------------- Query methods ----------------

    /**
     * Get all available spots of a specific type on this floor
     */
    public List<ParkingSpot> getAvailableSpotsByType(SpotType type) {
        List<ParkingSpot> result = new ArrayList<>();
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable() && spot.getType() == type) {
                result.add(spot);
            }
        }
        return result;
    }

    /**
     * Get all available spots that a specific vehicle can park in
     */
    public List<ParkingSpot> getAvailableSpotsForVehicle(Vehicle vehicle) {
        List<ParkingSpot> result = new ArrayList<>();
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable() && vehicle.canParkIn(spot.getType())) {
                result.add(spot);
            }
        }
        return result;
    }

    /**
     * Get count of available spots per type (useful for admin report)
     */
    public Map<SpotType, Integer> getAvailableCountByType() {
        Map<SpotType, Integer> counts = new HashMap<>();
        for (SpotType type : SpotType.values()) {
            counts.put(type, 0);
        }

        for (ParkingSpot spot : spots) {
            if (spot.isAvailable()) {
                SpotType t = spot.getType();
                counts.put(t, counts.get(t) + 1);
            }
        }
        return counts;
    }

    /**
     * Simple string representation for debugging / console output
     */
    @Override
    public String toString() {
        return String.format("Floor %d: %d/%d spots occupied (%.1f%%)",
                floorNumber, getOccupiedSpots(), getTotalSpots(), getOccupancyPercentage());
    }

    /**
     * For admin panel – list occupied spots with vehicle info
     */
    public String getOccupiedSpotsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Occupied spots on Floor ").append(floorNumber).append(":\n");
        for (ParkingSpot spot : spots) {
            if (!spot.isAvailable() && spot.getCurrentVehicle() != null) {
                sb.append("  • ").append(spot.getSpotId())
                  .append(" (").append(spot.getType()).append(") - ")
                  .append(spot.getCurrentVehicle()).append("\n");
            }
        }
        return sb.toString();
    }
}