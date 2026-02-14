package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//one parking record in database 
//data only, not logic
//use this when vehicle enters/exits

public class ParkingSession {

    private String ticketNo;   // Unique ticket number
    private String plate;      // Vehicle plate number
    private String spotId;     // Assigned parking spot
    private String entryTime;  // ISO time string

    // New constructor - time is set automatically
    public ParkingSession(String ticketNo, String plate, String spotId) {
        this.ticketNo = ticketNo;
        this.plate = plate;
        this.spotId = spotId;
        
        // Set time in the desired format
        this.entryTime = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Keep old constructor if needed for loading from database
    public ParkingSession(String ticketNo, String plate, String spotId, String entryTime) {
        this.ticketNo = ticketNo;
        this.plate = plate;
        this.spotId = spotId;
        this.entryTime = entryTime;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public String getPlate() {
        return plate;
    }

    public String getSpotId() {
        return spotId;
    }

    public String getEntryTime() {
        return entryTime;
    }
}