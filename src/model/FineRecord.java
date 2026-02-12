package model;

 //FineRecord = fine linked to a vehicle plate (DATA only).
 //use this for fine/payment, for now only have the data model + DB layer


public class FineRecord {

    private String plate;
    private String reason;    
    private double amount;
    private String issuedAt;   // ISO time string
    private boolean paid;

    public FineRecord(String plate, String reason, double amount, String issuedAt, boolean paid) {
        this.plate = plate;
        this.reason = reason;
        this.amount = amount;
        this.issuedAt = issuedAt;
        this.paid = paid;
    }

    public String getPlate() {
        return plate;
    }

    public String getReason() {
        return reason;
    }

    public double getAmount() {
        return amount;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public boolean isPaid() {
        return paid;
    }
}

