package model;

//one payment receipt stored in database.
// for now its data only. later add payment logic.

public class PaymentRecord {

    private String ticketNo;
    private String plate;
    private String method;   
    private String paidTime;

    private double parkingFee;
    private double finePaid;
    private double totalDue;
    private double amountPaid;
    private double balance;

    public PaymentRecord(String ticketNo, String plate, String method, String paidTime,
                         double parkingFee, double finePaid,
                         double totalDue, double amountPaid, double balance) {

        this.ticketNo = ticketNo;
        this.plate = plate;
        this.method = method;
        this.paidTime = paidTime;
        this.parkingFee = parkingFee;
        this.finePaid = finePaid;
        this.totalDue = totalDue;
        this.amountPaid = amountPaid;
        this.balance = balance;
    }

    public String getTicketNo() { return ticketNo; }
    public String getPlate() { return plate; }
    public String getMethod() { return method; }
    public String getPaidTime() { return paidTime; }

    public double getParkingFee() { return parkingFee; }
    public double getFinePaid() { return finePaid; }
    public double getTotalDue() { return totalDue; }
    public double getAmountPaid() { return amountPaid; }
    public double getBalance() { return balance; }
}

