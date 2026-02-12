package app;

import data.DataStore;
import data.SQLiteDataStore;
import fine.*;
import service.ExitService;
import service.PaymentProcessor;
import javax.swing.SwingUtilities;

public class main {

    public static void main(String[] args) {
        // 1. Database Connection
        DataStore store = new SQLiteDataStore();
        store.connect();
        store.initSchema();

        // 2. Build and Seed Parking Lot (Physical Structure)
        seedParkingLot(store);

        // 3. Initialize Fine Logic (Requirement 4)
        // Defaulting to Progressive as the starting scheme
        FineScheme initialScheme = new ProgressiveFineScheme(); 
        
        // 4. Initialize Services (Requirement 4 & 5)
        // These services will handle all fine calculations and payments
        ExitService exitService = new ExitService(store, initialScheme);
        PaymentProcessor paymentProcessor = new PaymentProcessor(store);

        System.out.println("Parking Management System - Backend Services Ready.");
        System.out.println("Current Strategy: " + initialScheme.getSchemeName());

        // 5. Cleanup Hook (Ensures DB closes when app/window is closed)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            store.close();
            System.out.println("Database connection closed safely.");
        }));

        // 6. Launch UI (Pass all services to the MainFrame)
        SwingUtilities.invokeLater(() -> {
            try {
                // We pass 'exitService' so AdminPanel can change schemes
                // We pass 'paymentProcessor' so EntryExitPanel can handle payments
                new ui.MainFrame(store, exitService, paymentProcessor).setVisible(true);
            } catch (Exception e) {
                System.out.println("MainFrame not found yet. Implement ui.MainFrame to see the GUI.");
            }
        });
    }

    private static void seedParkingLot(DataStore store) {
        builder.ParkingLotBuilder builder = new builder.ParkingLotBuilder()
            .setName("University Parking Lot")
            .setNumFloors(3)
            .setRowsPerFloor(2)
            .setSpotsPerRow(10)
            .setSpotDistributionPerRow(2, 6, 1, 1);

        model.ParkingLot lot = builder.build();

        // Sync local memory structure to the SQLite database
        for (model.Floor floor : lot.getFloors()) {
            for (model.Row row : floor.getRows()) {
                for (model.ParkingSpot spot : row.getSpots()) {
                    store.upsertSpot(spot);
                }
            }
        }
        System.out.println("Parking structure seeded to database.");
    }
}