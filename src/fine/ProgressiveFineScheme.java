package fine;

public class ProgressiveFineScheme implements FineScheme {
    @Override
    public double calculateFine(long overstayHours) {
        if (overstayHours <= 0) return 0.0;
        
        double totalFine = 50.0; // First 24 hours of overstay (Hours 0-24)
        
        if (overstayHours > 24) {
            totalFine += 100.0; // Hours 24-48
        }
        if (overstayHours > 48) {
            totalFine += 150.0; // Hours 48-72
        }
        if (overstayHours > 72) {
            totalFine += 200.0; // Above 72 hours
        }
        
        return totalFine;
    }

    @Override
    public String getSchemeName() {
        return "Progressive Fine (Tiered)";
    }
}