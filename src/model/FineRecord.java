package model;

public class FineRecord {
    private String plate;
    private String type;
    private double amount;       // current unpaid amount
    private String issuedTime;
    private boolean paid;

    public FineRecord(String plate, String type, double amount, String issuedTime, boolean paid) {
        this.plate = plate;
        this.type = type;
        this.amount = amount;
        this.issuedTime = issuedTime;
        this.paid = paid;
    }

    public String getPlate() { return plate; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getIssuedTime() { return issuedTime; }
    public boolean isPaid() { return paid; }

    public void setPaid(boolean paid) { this.paid = paid; }

    // NEW: Reduce fine by partial payment
    public void reduceAmount(double paidAmount) {
        if (paidAmount >= amount) {
            amount = 0;
            paid = true;
        } else {
            amount -= paidAmount;
        }
    }
}
