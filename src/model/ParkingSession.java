package model;
 
//one parking record in database 
//data only, not logic
//use this when vehicle enters/exits

public class ParkingSession {

    private String ticketNo;   // Unique ticket number
    private String plate;      // Vehicle plate number
    private String spotId;     // Assigned parking spot
    private String entryTime;  // ISO time string

    // Constructor used when vehicle enters
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