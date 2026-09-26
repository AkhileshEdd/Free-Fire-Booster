package in.akhilesh.ember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure, testable calculations; never manufactures a missing reading. */
public final class Metrics {
    private Metrics() {}
    public static final class NetworkResult {
        public final List<Double> samples;
        public final Double median, variation;
        public final int failures;
        public NetworkResult(List<Double> values) {
            samples = Collections.unmodifiableList(new ArrayList<>(values));
            List<Double> good = new ArrayList<>();
            double sum = 0; int pairs = 0; Double previous = null;
            for (Double value : values) {
                if (value != null) {
                    good.add(value);
                    if (previous != null) { sum += Math.abs(value - previous); pairs++; }
                }
                previous = value; // Do not bridge a failed sample.
            }
            failures = values.size() - good.size();
            Collections.sort(good);
            int n = good.size();
            median = n == 0 ? null : n % 2 == 1 ? good.get(n / 2) : (good.get(n / 2 - 1) + good.get(n / 2)) / 2;
            variation = pairs == 0 ? null : sum / pairs;
        }
    }
    public static List<String> warnings(int battery, Float temperature, boolean saver, boolean connected, boolean charging) {
        List<String> out = new ArrayList<>();
        if (!connected) out.add("No validated internet connection. Check Wi-Fi or mobile data before launching.");
        if (battery >= 0 && battery < 20) out.add("Battery below 20%. Plan a shorter session or charge before playing.");
        if (temperature != null && temperature >= 40) out.add("Battery is warm. Let the phone cool naturally before a long session.");
        if (saver) out.add("Battery Saver is on. It may limit performance; review it in system settings.");
        if (charging) out.add("Charging can add heat during play. Watch temperature and comfort.");
        return out;
    }
    public static long duration(long start, long end) { return Math.max(0L, end - start); }
    public static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}
