package service;

import data.DataStore;
import enums.FineReason;
import fine.FineScheme;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import model.FineRecord;
import model.ParkingSession;
import model.ParkingSpot;
import model.PaymentRecord;
import model.Vehicle;

public class ExitService {

    private final DataStore dataStore;
    private FineScheme activeFineScheme;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ExitService(DataStore dataStore, FineScheme activeFineScheme) {
        this.dataStore = dataStore;
        this.activeFineScheme = activeFineScheme;
    }

    // --- Process vehicle exit using ParkingSession (new overload) ---
    public PaymentRecord processExit(ParkingSession session, LocalDateTime exitTime) {
        if (session == null) return null;

        String exitTimeStr = exitTime.format(FORMATTER);
        Vehicle vehicle = session.getVehicle();
        String plate = vehicle.getPlate();

        // 1️⃣ Duration in hours (ceiling rounding)
        long hours = calculateHoursCeiling(session.getEntryTime(), exitTime);

        // 2️⃣ Determine hourly rate & parking fee
        double hourlyRate = getHourlyRate(session, vehicle);
        double parkingFee = hours * hourlyRate;

        // 3️⃣ Determine fine scheme for this session
        FineScheme sessionScheme = mapStringToScheme(session.getFineScheme());
        if (sessionScheme == null) sessionScheme = activeFineScheme; // fallback

        // 4️⃣ Overstay fine (>24h) using session scheme
        if (hours > 24) {
            double overstayFine = sessionScheme.calculateFine(hours - 24);
            if (overstayFine > 0) {
                dataStore.addFine(new FineRecord(
                        plate,
                        FineReason.OVERSTAY_24H,
                        overstayFine,
                        exitTimeStr,
                        false
                ));
            }
        }

        // 5️⃣ Reserved spot violation (non-VIP in Reserved)
        if (session.getSpotId().contains("RES") && !vehicle.isVIP()) {
            double resFine = 100.0;
            dataStore.addFine(new FineRecord(
                    plate,
                    FineReason.RESERVED_VIOLATION,
                    resFine,
                    exitTimeStr,
                    false
            ));
        }

        // 6️⃣ Sum all unpaid fines
        List<FineRecord> unpaidFines = dataStore.getUnpaidFinesByPlate(plate);
        double totalUnpaidFines = unpaidFines.stream()
                .mapToDouble(FineRecord::getAmount)
                .sum();

        // 7️⃣ Calculate total due (parking fee + unpaid fines)
        double totalDue = parkingFee + totalUnpaidFines;

        // 8️⃣ Already paid for preview (optional)
        double totalPaid = dataStore.getPaymentsByTicket(session.getTicketNo()).stream()
                .mapToDouble(PaymentRecord::getAmountPaid)
                .sum();

        double amountPaidNow = Math.min(totalPaid, totalDue);

        // 9️⃣ Return PaymentRecord
        return new PaymentRecord(
                session.getTicketNo(),
                plate,
                null, // PaymentMethod (set during actual payment)
                exitTime,
                (int) hours,
                parkingFee,
                totalUnpaidFines,
                amountPaidNow
        );
    }

    private FineScheme mapStringToScheme(String schemeName) {
        if (schemeName == null) return null;

        switch (schemeName) {
            case "Fixed Fine (RM 50)":
                return new fine.FixedFineScheme();
            case "Progressive (Tiered)":
                return new fine.ProgressiveFineScheme();
            case "Hourly (RM 20/hr)":
                return new fine.HourlyFineScheme();
            default:
                return null;
        }
    }


    // --- Determine hourly rate based on spot & vehicle ---
    private double getHourlyRate(ParkingSession session, Vehicle vehicle) {
        ParkingSpot spot = dataStore.getAllSpots().stream()
                .filter(s -> s.getSpotId().equals(session.getSpotId()))
                .findFirst()
                .orElse(null);

        if (spot == null) return 5.0; // fallback default

        String spotType = spot.getType().toString().toUpperCase();
        double rate;

        switch (spotType) {
            case "COMPACT": rate = 2.0; break;
            case "REGULAR": rate = 5.0; break;
            case "HANDICAPPED": rate = vehicle.hasHcCard() ? 0.0 : 2.0; break;
            case "RESERVED": rate = 10.0; break;
            default: rate = 5.0;
        }

        return rate;
    }

    // --- Ceiling rounding for hours ---
    private long calculateHoursCeiling(String entryTimeStr, LocalDateTime exitTime) {
        LocalDateTime entryTime = LocalDateTime.parse(entryTimeStr, FORMATTER);
        Duration duration = Duration.between(entryTime, exitTime);
        long totalMinutes = duration.toMinutes();
        return Math.max(1, (long) Math.ceil(totalMinutes / 60.0));
    }

    // --- Finalize exit after payment ---
    public void finalizeExit(String ticketNo, String spotId, int durationHours, double parkingFee) {
        String exitTimeStr = LocalDateTime.now().format(FORMATTER);
        dataStore.closeSession(ticketNo, exitTimeStr, durationHours, parkingFee);
        dataStore.setSpotAvailable(spotId);
        System.out.println("Exit finalized for ticket: " + ticketNo);
    }

    // --- Fine scheme getter/setter ---
    public void setActiveFineScheme(FineScheme scheme) {
        this.activeFineScheme = scheme;
    }

    public FineScheme getActiveFineScheme() {
        return activeFineScheme;
    }
}
