package fine;

public class ProgressiveFineScheme implements FineScheme {
    @Override
    public double calculateFine(long overstayHours) {
        double fine = 0;
        if (overstayHours <= 0) return 0.0;
        if (overstayHours > 0) fine += 50;           // first 24h
        if (overstayHours > 24) fine += 100;        // 24–48h
        if (overstayHours > 48) fine += 150;        // 48–72h
        if (overstayHours > 72) fine += 200;        // >72h
        return fine;
    }

    @Override
    public String getSchemeName() { return "Progressive Fine Scheme"; }
}
