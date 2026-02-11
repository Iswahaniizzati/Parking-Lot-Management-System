package com.group5.parkinglot.model;

import com.group5.parkinglot.service.DatabaseService;
import com.group5.parkinglot.service.FeeCalculationService;
import com.group5.parkinglot.service.FineService;
import com.group5.parkinglot.strategy.FineStrategy;
import com.group5.parkinglot.strategy.FixedFineStrategy;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ParkingLot singleton – manages floors, spots, active tickets, revenue
 */
public class ParkingLot {

    private static ParkingLot instance;

    public static ParkingLot getInstance() {
        if (instance == null) {
            synchronized (ParkingLot.class) {
                if (instance == null) instance = new ParkingLot();
            }
        }
        return instance;
    }

    private final List<Floor> floors = new ArrayList<>();
    private final Map<String, ActiveTicket> activeTickets = new HashMap<>();
    private final DatabaseService dbService;
    private final FeeCalculationService feeService;
    private final FineService fineService;

    private double totalRevenue = 0.0;

    private ParkingLot() {
        dbService = new DatabaseService();
        dbService.initializeDatabase();

        feeService = new FeeCalculationService();
        fineService = new FineService(new FixedFineStrategy());

        initializeFloors(5, 20, 5);
        loadActiveTicketsFromDb();
    }

    private void initializeFloors(int numFloors, int spotsPerRow, int rowsPerFloor) {
        for (int f = 1; f <= numFloors; f++) {
            floors.add(new Floor(f, spotsPerRow, rowsPerFloor));
        }
    }

    private void loadActiveTicketsFromDb() {
        List<ActiveTicket> tickets = dbService.loadActiveTickets();
        for (ActiveTicket ticket : tickets) {
            activeTickets.put(ticket.getLicensePlate(), ticket);
        }
    }

    public ParkingSpot findAvailableSpot(Vehicle vehicle) {
        for (Floor floor : floors) {
            List<ParkingSpot> spots = floor.getAvailableSpotsForVehicle(vehicle);
            if (!spots.isEmpty()) return spots.get(0);
        }
        return null;
    }

    public ActiveTicket parkVehicle(Vehicle vehicle, ParkingSpot spot) {
        if (spot == null || !spot.assignVehicle(vehicle)) {
            throw new IllegalStateException("Cannot park vehicle in selected spot");
        }

        LocalDateTime now = LocalDateTime.now();
        vehicle.setEntryTime(now);

        String ticketId = "T-" + vehicle.getLicensePlate() + "-" + System.currentTimeMillis();
        ActiveTicket ticket = new ActiveTicket(ticketId,
                vehicle.getLicensePlate(),
                vehicle.getType().name(),
                spot.getSpotId(),
                now);

        activeTickets.put(vehicle.getLicensePlate(), ticket);
        dbService.saveActiveTicket(ticket);

        return ticket;
    }

    public double exitVehicle(String licensePlate, String paymentMethod) {
        ActiveTicket ticket = activeTickets.get(licensePlate);
        if (ticket == null) return 0.0;

        ParkingSpot spot = findSpotById(ticket.getSpotId());

        double fee = feeService.calculateFee(ticket, spot);
        long overstay = Math.max(0, ticket.calculateHours(LocalDateTime.now()) - 24);
        double fine = fineService.calculateFine(licensePlate, overstay);
        double unpaid = fineService.getUnpaidFines(licensePlate);

        double totalDue = fee + fine + unpaid;

        fineService.markFinesPaid(licensePlate);
        if (spot != null) spot.removeVehicle();
        activeTickets.remove(licensePlate);

        totalRevenue += totalDue;
        return totalDue;
    }

    private ParkingSpot findSpotById(String spotId) {
        for (Floor floor : floors) {
            for (ParkingSpot spot : floor.getSpots()) {
                if (spot.getSpotId().equals(spotId)) return spot;
            }
        }
        return null;
    }

    // ─ Admin/Reports ─
    public void setFineStrategy(FineStrategy strategy) {
        fineService.setStrategy(strategy);
        System.out.println("Fine strategy changed to: " + strategy.getStrategyName());
    }

    public int getTotalOccupiedSpots() {
        return floors.stream().mapToInt(Floor::getOccupiedSpots).sum();
    }

    public int getTotalSpots() {
        return floors.stream().mapToInt(Floor::getTotalSpots).sum();
    }

    public double getOccupancyPercentage() {
        int total = getTotalSpots();
        return total == 0 ? 0.0 : (getTotalOccupiedSpots() * 100.0) / total;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public List<Floor> getFloors() {
        return Collections.unmodifiableList(floors);
    }

    public Map<String, ActiveTicket> getActiveTickets() {
        return Collections.unmodifiableMap(activeTickets);
    }
}
