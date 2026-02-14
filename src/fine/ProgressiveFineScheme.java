package fine;

public class ProgressiveFineScheme implements FineScheme {
    @Override
    public double calculateFine(long overstayHours) {

        if (overstayHours <= 0) return 0.0;

        double fine = 0;

        if (overstayHours > 0) fine += 50;        // 0–24 overstay
        if (overstayHours > 24) fine += 100;      // 24–48 overstay
        if (overstayHours > 48) fine += 150;      // 48–72 overstay
        if (overstayHours > 72) fine += 200;      // >72 overstay

        return fine;
    }


    @Override
    public String getSchemeName() { return "Progressive Fine Scheme"; }
}
