package app;

import builder.ParkingLotBuilder;
import data.DataStore;
import data.SQLiteDataStore;
import enums.SpotType;
import model.ParkingLot;
import model.ParkingSpot;
import model.ParkingSession;

public class main {

    public static void main(String[] args) {

        DataStore store = new SQLiteDataStore();

        
        store.connect(); //connect to database
        store.initSchema(); //create tables if not exist

    // Build parking lot structure (from builder)
    builder.ParkingLotBuilder builder = new builder.ParkingLotBuilder()
        .setName("University Parking Lot")
        .setNumFloors(3)
        .setRowsPerFloor(2)
        .setSpotsPerRow(10)
        .setSpotDistributionPerRow(2, 6, 1, 1); // compact, regular, handicapped, reserved

    model.ParkingLot lot = builder.build();

    // Seed all spots into database (loop through structure)
    for (model.Floor floor : lot.getFloors()) {
        for (model.Row row : floor.getRows()) {
            for (model.ParkingSpot spot : row.getSpots()) {
            store.upsertSpot(spot);
        }
    }
}

        store.close(); //close db connection
    }
}
