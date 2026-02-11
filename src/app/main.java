package app;

import builder.ParkingLotBuilder;
import model.ParkingLot;

//this main is to test hani's part of creating parking lot, feel free to change for your part:)
public class main {

    public static void main(String[] args) {

        ParkingLot lot = new ParkingLotBuilder()
                .setName("MMU Parking Lot")
                .setNumFloors(3)
                .setRowsPerFloor(2)
                .setSpotsPerRow(6)
                .setSpotDistributionPerRow(2, 2, 1, 1)
                .build();

        System.out.println("Parking lot created successfully.");
        System.out.println("Total floors: " + lot.getFloors().size());
        System.out.println("Total spots: " + lot.getAllSpots().size());
    }
}


