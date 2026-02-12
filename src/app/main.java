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

        // 2. Build and Seed Parking Lot
        seedParkingLot(store);

        // 3. Initialize Fine Logic
        FineScheme initialScheme = new ProgressiveFineScheme(); 
        
        // 4. Initialize Services
        ExitService exitService = new ExitService(store, initialScheme);
        PaymentProcessor paymentProcessor = new PaymentProcessor(store);

        // 5. Cleanup Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            store.close();
            System.out.println("Database connection closed safely.");
        }));

        // 6. Launch UI
        SwingUtilities.invokeLater(() -> {
            try {
                // Show Login First
                ui.LoginDialog loginDlg = new ui.LoginDialog(null, store);
                loginDlg.setVisible(true);

                if (loginDlg.isSucceeded()) {
                    String role = loginDlg.getAuthenticatedRole();
                    // Launch MainFrame with all dependencies
                    new ui.MainFrame(store, exitService, paymentProcessor, role).setVisible(true);
                } else {
                    System.exit(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }); // <-- This was likely the missing/incorrect line
    }

    private static void seedParkingLot(DataStore store) {
        builder.ParkingLotBuilder builder = new builder.ParkingLotBuilder()
            .setName("University Parking Lot")
            .setNumFloors(3)
            .setRowsPerFloor(2)
            .setSpotsPerRow(10)
            .setSpotDistributionPerRow(2, 6, 1, 1);

        model.ParkingLot lot = builder.build();

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