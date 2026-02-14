package service;

import data.DataStore;
import enums.PaymentMethod;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import model.FineRecord;
import model.ParkingSession;
import model.PaymentRecord;

public class PaymentProcessor {
    private DataStore dataStore;

    public PaymentProcessor(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Processes payment and applies it to fines + parking fee.
     * Supports partial payment.
     */
    public void processPayment(String ticketNo, String plate, double parkingFee,
                               double fineAmount, PaymentMethod method, double amountPaid) {

        double totalDue = parkingFee + fineAmount;
        double remaining = totalDue - amountPaid; // positive if still owed
        String now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // 1️⃣ Apply payment to fines first
        List<FineRecord> unpaidFines = dataStore.getUnpaidFinesByPlate(plate);
        double amountLeft = amountPaid;

        for (FineRecord fine : unpaidFines) {
            if (amountLeft <= 0) break;

            double fineRemaining = fine.getAmount();
            if (amountLeft >= fineRemaining) {
                // Fully pay this fine
                dataStore.reduceFineAmount(fine, fineRemaining); // marked as paid
                amountLeft -= fineRemaining;
            } else {
                // Partial payment
                dataStore.reduceFineAmount(fine, amountLeft);
                amountLeft = 0;
            }
        }

        // 2️⃣ Apply remaining payment to parking fee
        double parkingPaid = Math.min(parkingFee, amountLeft);
        amountLeft -= parkingPaid;

        // 3️⃣ Record the Payment
        PaymentRecord record = new PaymentRecord(
                ticketNo,
                plate,
                method.toString(),
                now,
                parkingFee,
                fineAmount,
                totalDue,
                amountPaid,
                amountLeft > 0 ? 0 : -remaining // balance (positive if overpaid)
        );
        dataStore.createPayment(record);

        // 4️⃣ Close parking session only if fully paid
        if (amountPaid >= totalDue) {
            dataStore.closeSession(ticketNo, now, 0, parkingFee);

            ParkingSession session = dataStore.getOpenSessionByPlate(plate);
            if (session != null) {
                dataStore.setSpotAvailable(session.getSpotId());
            }
        }

        // 5️⃣ Print receipt to console
        printReceipt(record, now);
    }

    private void printReceipt(PaymentRecord p, String exitTime) {
        System.out.println("\n========= PARKING RECEIPT =========");
        System.out.println("Plate:          " + p.getPlate());
        System.out.println("Ticket No:      " + p.getTicketNo());
        System.out.println("Exit Time:      " + exitTime);
        System.out.println("-----------------------------------");
        System.out.println("Parking Fee:    RM " + String.format("%.2f", p.getParkingFee()));
        System.out.println("Fines Due:      RM " + String.format("%.2f", p.getFinePaid()));
        System.out.println("TOTAL DUE:      RM " + String.format("%.2f", p.getTotalDue()));
        System.out.println("-----------------------------------");
        System.out.println("Payment Method: " + p.getMethod());
        System.out.println("Amount Paid:    RM " + String.format("%.2f", p.getAmountPaid()));
        System.out.println("Balance/Change: RM " + String.format("%.2f", p.getBalance()));
        System.out.println("===================================\n");
    }
}
