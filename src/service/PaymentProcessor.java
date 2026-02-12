package service;

import data.DataStore;
import enums.PaymentMethod;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import model.PaymentRecord;

public class PaymentProcessor {
    private DataStore dataStore;

    public PaymentProcessor(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Processes the payment and updates all database records.
     */
    public void processPayment(String ticketNo, String plate, double parkingFee, 
                               double fineAmount, PaymentMethod method, double amountPaid) {
        
        double totalDue = parkingFee + fineAmount;
        double balance = amountPaid - totalDue;
        String now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // 1. Record the Payment in DB
        PaymentRecord record = new PaymentRecord(
            ticketNo, plate, method.toString(), now, 
            parkingFee, fineAmount, totalDue, amountPaid, balance
        );
        dataStore.createPayment(record);

        // 2. Close the Parking Session
        // Note: You might want to pass actual duration calculated in ExitService
        dataStore.closeSession(ticketNo, now, 0, parkingFee);

        // 3. Mark Fines as Paid
        if (fineAmount > 0) {
            dataStore.markAllFinesPaid(plate, now);
        }

        // 4. Mark the Spot as Available
        // We get the session to find which spot the vehicle was in
        var session = dataStore.getOpenSessionByPlate(plate);
        if (session != null) {
            dataStore.setSpotAvailable(session.getSpotId());
        }

        // 5. Generate Receipt
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
        System.out.println("Balance:        RM " + String.format("%.2f", p.getBalance()));
        System.out.println("===================================\n");
    }
}