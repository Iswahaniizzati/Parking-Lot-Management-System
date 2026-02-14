package service;

import data.DataStore;
import model.ParkingSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

//entry regstration system. coordinates spot and session creation.
public class EntryService {
    private final DataStore dataStore;

    public EntryService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public String registerVehicleEntry(String plate, String vehicleType, String spotId) {
        
        //ticket format
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String ticketNo = "T-" + plate.toUpperCase().replace(" ", "") + "-" + timestamp;

        model.ParkingSpot spot = dataStore.getAllSpots().stream()
                .filter(s -> s.getSpotId().equals(spotId))
                .findFirst()
                .orElse(null);

        if (spot == null) return null;
        String sType = spot.getType().toString();

        //vehicle type suitability
        if (vehicleType.equalsIgnoreCase("Motorcycle")) {
            if (!sType.equals("COMPACT")) { // Must be Compact
                return null; 
            }
        }
        if (vehicleType.equalsIgnoreCase("SUV/Truck")) {
            if (sType.equals("COMPACT")) { // Cannot be Compact
                return null;
            }
        }
        //update database
        String entryTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        ParkingSession session = new ParkingSession(ticketNo, plate, spotId, entryTime);

        dataStore.createSession(session);
        dataStore.setSpotOccupied(spotId, plate);

        return ticketNo;

    }
    
}
