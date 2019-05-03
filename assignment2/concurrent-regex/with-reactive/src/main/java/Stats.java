import java.text.DecimalFormat;

public class Stats {

    static public final int DEFAULT_DIGITS = 1;

    private int total = 0;
    private int withMatches = 0;
    private double withMatchesPercent = 0;
    private double avgMatches = 0;
    final private DecimalFormat decimalFormat;

    public Stats(int decimalDigits) {
        decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(decimalDigits);
    }

    public Stats() {
        this(DEFAULT_DIGITS);
    }

    public void recordMatches(int matches) {
        if (matches > 0) {
            avgMatches = ((withMatches * avgMatches) + matches) / (withMatches + 1);
            withMatches++;
        }
        total++;
        withMatchesPercent = (double)withMatches / total * 100;
    }

    @Override
    /**
     * Example: "Files analyzed: 7 | With matches: 57.1% (avg matches while matching: 5.2) "
     */
    public String toString() {
        String s = "Files analyzed: "+total+" | With matches: "+decimalFormat.format(withMatchesPercent)+"% ";
        if (withMatches > 0) {
            s += "(avg matches while matching: "+decimalFormat.format(avgMatches)+") ";
        }
        return s;
    }

    public int getTotal() { return total; }
    public int getWithMatches() { return withMatches; }
    public double getWithMatchesPercent() { return withMatchesPercent; }
    public double getAvgMatches() { return avgMatches; }

}
