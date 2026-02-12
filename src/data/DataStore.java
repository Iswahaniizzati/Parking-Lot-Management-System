package data;

import java.util.List;
import model.ParkingSpot;

// database interface
//all modules only call these methods

public interface DataStore {

    void connect(); //open database connection
    void close(); //close database connection

    void initSchema(); //create required tables if not exist

    // Parking spot operations
    void upsertSpot(ParkingSpot spot); //insert or update parking spit
    List<ParkingSpot> findAvailableSpots(String spotType); //get all AVAILABLE spots of a specific type
    void setSpotOccupied(String spotId, String plate); //mark a spot as OCCUPIED and store plate num
    void setSpotAvailable(String spotId); //mark a spot as AVAILABLE and clear plate number
    void createSession(model.ParkingSession session); //insert a new parking session(vehicle entry)
    model.ParkingSession getOpenSessionByPlate(String plate); //get the latest open session for a plate
    void closeSession(String ticketNo, String exitTimeISO, int durationHours, double parkingFee); //update session with exit time,duration and fee
    void addFine(model.FineRecord fine); //insert a fine record
    java.util.List<model.FineRecord> getUnpaidFines(String plate);
    void markAllFinesPaid(String plate, String paidTimeISO); //mark all unpaid fines as paid
    void createPayment(model.PaymentRecord payment); //insert a payment record


    
}
