package service;

import data.DataStore;
import enums.FineReason;
import fine.FineScheme;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import model.FineRecord;
import model.ParkingSession;
import model.ParkingSpot;
import model.PaymentRecord;
import model.Vehicle;

public class ExitService {

    private final DataStore dataStore;
    private final FineScheme activeFineScheme;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ExitService(DataStore dataStore, FineScheme activeFineScheme) {
        this.dataStore = dataStore;
        this.activeFineScheme = activeFineScheme;
    }

    // ===============================
    //  PREVIEW EXIT (NO DB WRITES)
    // ===============================
    public PaymentRecord previewExit(ParkingSession session, LocalDateTime exitTime) {
        if (session == null) return null;

        // 1️⃣ Calculate duration
        long hours = calculateHoursCeiling(session.getEntryTime(), exitTime);

        // 2️⃣ Parking fee
        double parkingFee = hours * getHourlyRate(session, session.getVehicle());

        // 3️⃣ New fines generated during this exit (preview)
        double newFine = calculatePreviewFines(session, hours);

        // 4️⃣ Past unpaid fines
        List<FineRecord> pastUnpaid = dataStore.getUnpaidFinesByPlate(session.getVehicle().getPlate());
        double pastFineTotal = pastUnpaid.stream().mapToDouble(FineRecord::getAmount).sum();

        // 5️⃣ Total fines = past unpaid + new fine
        double totalFines = pastFineTotal + newFine;

        // 6️⃣ Build preview record
        return new PaymentRecord(
            session.getTicketNo(),
            session.getVehicle().getPlate(),
            null,                 // method unknown in preview
            exitTime,
            (int) hours,
            parkingFee,
            totalFines,           // include past fines
            0                     // amountPaid 0 in preview
        );
    }

    // ===============================
    //  FINALIZE EXIT (WRITE TO DB)
    // ===============================
    public PaymentRecord confirmExit(ParkingSession session, LocalDateTime exitTime,
                                     PaymentRecord payment, boolean markAllFinesPaid) {

        Vehicle vehicle = session.getVehicle();
        String plate = vehicle.getPlate();
        String exitTimeStr = exitTime.format(FORMATTER);

        long hours = calculateHoursCeiling(session.getEntryTime(), exitTime);
        double parkingFee = hours * getHourlyRate(session, vehicle);

        FineScheme scheme = mapStringToScheme(session.getFineScheme());
        if (scheme == null) scheme = activeFineScheme;

        // Generate new fines (if any)
        List<FineRecord> newFines = generateFinalFines(session, hours, exitTimeStr, scheme);
        for (FineRecord fine : newFines) {
            dataStore.addFine(fine);
        }

        // Optionally mark all unpaid fines fully paid
        double amountLeft = payment.getAmountPaid() - payment.getParkingFee();
        List<FineRecord> unpaidFines = dataStore.getUnpaidFinesByPlate(plate);

        for (FineRecord fine : unpaidFines) {
            if (amountLeft <= 0) break;
            double toPay = Math.min(amountLeft, fine.getAmount());
            dataStore.reduceFineAmount(fine, toPay);
            amountLeft -= toPay;
        }


        // Close the parking session and free spot
        dataStore.closeSession(session.getTicketNo(), exitTimeStr, (int) hours, parkingFee);
        dataStore.setSpotAvailable(session.getSpotId());

        // Create PaymentRecord with actual method & paid amount
        PaymentRecord finalizedPayment = new PaymentRecord(
                session.getTicketNo(),
                plate,
                payment.getMethod(),
                exitTime,
                (int) hours,
                parkingFee,
                payment.getFinePaid(),
                payment.getAmountPaid()
        );

        dataStore.createPayment(finalizedPayment);

        System.out.println("Exit finalized for ticket: " + session.getTicketNo());
        return finalizedPayment;
    }

    // ===============================
    //  Preview fine calculation
    // ===============================
    private double calculatePreviewFines(ParkingSession session, long hours) {
        double total = 0;

        // Overstay fine
        if (hours > 24) total += activeFineScheme.calculateFine(hours - 24);

        // Reserved spot violation
        if (session.getSpotId().contains("RES") && !session.getVehicle().isVIP()) total += 100.0;

        return total;
    }

    // ===============================
    //  Final fine creation
    // ===============================
    public List<FineRecord> generateFinalFines(ParkingSession session, long hours,
                                                String exitTimeStr, FineScheme scheme) {
        List<FineRecord> fines = new ArrayList<>();
        String plate = session.getVehicle().getPlate();

        if (hours > 24 && !fineExists(plate, FineReason.OVERSTAY_24H)) {
            fines.add(new FineRecord(plate, FineReason.OVERSTAY_24H, scheme.calculateFine(hours - 24),
                    exitTimeStr, false));
        }

        if (session.getSpotId().contains("RES") && !session.getVehicle().isVIP()
                && !fineExists(plate, FineReason.RESERVED_VIOLATION)) {
            fines.add(new FineRecord(plate, FineReason.RESERVED_VIOLATION, 100.0,
                    exitTimeStr, false));
        }

        return fines;
    }

    private boolean fineExists(String plate, FineReason reason) {
        return dataStore.getUnpaidFinesByPlate(plate).stream()
                .anyMatch(f -> f.getReason() == reason);
    }

    private double getHourlyRate(ParkingSession session, Vehicle vehicle) {
        ParkingSpot spot = dataStore.getAllSpots().stream()
                .filter(s -> s.getSpotId().equals(session.getSpotId()))
                .findFirst().orElse(null);

        if (spot == null) return 5.0;
        switch (spot.getType().toString().toUpperCase()) {
            case "COMPACT": return 2.0;
            case "REGULAR": return 5.0;
            case "HANDICAPPED": return vehicle.hasHcCard() ? 0.0 : 2.0;
            case "RESERVED": return 10.0;
            default: return 5.0;
        }
    }

    private long calculateHoursCeiling(String entryTimeStr, LocalDateTime exitTime) {
        LocalDateTime entry = LocalDateTime.parse(entryTimeStr, FORMATTER);
        long minutes = Duration.between(entry, exitTime).toMinutes();
        return Math.max(1, (long) Math.ceil(minutes / 60.0));
    }

    private FineScheme mapStringToScheme(String schemeName) {
        if (schemeName == null) return null;
        return switch (schemeName) {
            case "Fixed Fine (RM 50)" -> new fine.FixedFineScheme();
            case "Progressive (Tiered)" -> new fine.ProgressiveFineScheme();
            case "Hourly (RM 20/hr)" -> new fine.HourlyFineScheme();
            default -> null;
        };
    }

    public void setActiveFineScheme(FineScheme scheme) { /* immutable */ }
    public FineScheme getActiveFineScheme() { return activeFineScheme; }
}
