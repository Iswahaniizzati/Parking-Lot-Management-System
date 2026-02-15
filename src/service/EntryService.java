package service;

import data.DataStore;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import model.ParkingSession;
import model.ParkingSpot;
import model.Vehicle;

public class EntryService {

    private final DataStore dataStore;
    private static final DateTimeFormatter ENTRY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EntryService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public String registerVehicleEntry(Vehicle vehicle, String spotId) {
        // Ticket format
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String ticketNo = "T-" + vehicle.getPlate().toUpperCase().replace(" ", "") + "-" + timestamp;

        ParkingSpot spot = dataStore.getAllSpots().stream()
                .filter(s -> s.getSpotId().equals(spotId))
                .findFirst()
                .orElse(null);

        if (spot == null) return null;

        // Check spot suitability
        if (!isSpotSuitable(vehicle, spot.getType().toString())) return null;

        // Get active fine scheme from DataStore
        String fineScheme = dataStore.getActiveFineScheme();  // <- new

        // Save session
        String entryTime = LocalDateTime.now().format(ENTRY_FORMAT);
        ParkingSession session = new ParkingSession(ticketNo, vehicle, spotId, entryTime, fineScheme);
        dataStore.createSession(session);
        dataStore.setSpotOccupied(spotId, vehicle.getPlate());

        return ticketNo;
    }


    private boolean isSpotSuitable(Vehicle vehicle, String spotType) {
        switch (spotType.toUpperCase()) {
            case "COMPACT":
                return vehicle.getType().equalsIgnoreCase("MOTORCYCLE") || vehicle.getType().equalsIgnoreCase("CAR");
            case "REGULAR":
                return vehicle.getType().equalsIgnoreCase("CAR") || vehicle.getType().equalsIgnoreCase("SUV/TRUCK");
            case "HANDICAPPED":
                return vehicle.getType().equalsIgnoreCase("HANDICAPPED");
            case "RESERVED":
                return vehicle.isVIP(); // only VIP cars
            default:
                return false;
        }
    }
}
