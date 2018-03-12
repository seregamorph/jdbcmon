package org.jdbcmon;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.jdbcmon.Utils.checkArgument;

/**
 * Logarithmic statistics
 */
class LogStat {

    private static final double DEFAULT_MANTISSA = 1.0d;
    private static final double DEFAULT_BASE = Math.sqrt(2.0d);
    private static final RoundMode DEFAULT_ROUND_MODE = RoundMode.ROUND;
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final ConcurrentMap<Long, AtomicLong> stat = new ConcurrentHashMap<>();

    private final double mantissa;
    private final double base;
    private final double logBase;
    private final RoundMode roundMode;

    LogStat() {
        this(DEFAULT_ROUND_MODE);
    }

    LogStat(RoundMode roundMode) {
        this(DEFAULT_BASE, roundMode);
    }

    LogStat(double base, RoundMode roundMode) {
        this(DEFAULT_MANTISSA, base, roundMode);
    }

    LogStat(double mantissa, double base, RoundMode roundMode) {
        checkArgument(mantissa > 0.0d, "Illegal base value %s", base);
        checkArgument(base > 1.0d, "Illegal base value %s", base);

        this.mantissa = mantissa;
        this.base = base;
        this.logBase = Math.log(base);
        this.roundMode = Objects.requireNonNull(roundMode, "roundMode");
    }

    double mantissa() {
        return mantissa;
    }

    double base() {
        return base;
    }

    RoundMode roundMode() {
        return roundMode;
    }

    void addValue(double value) {
        checkArgument(value >= 0.0d, "Illegal value %s", value);
        long key;
        if (value == 0.0d) {
            // special case
            key = Long.MIN_VALUE;
        } else {
            key = roundMode.round(Math.log(value / this.mantissa) / this.logBase);
        }
        stat.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    SortedMap<Double, Long> getStat() {
        return stat.entrySet().stream().collect(Utils.toTreeMap(
                e -> this.mantissa * Math.pow(this.base, e.getKey()),
                e -> e.getValue().get()
        ));
    }

    /**
     * Get formatted report. The double values are formatted with precision,
     * sorted ascending as double values.
     *
     * @return
     */
    Map<String, Long> getReportStat(int precision) {
        Utils.checkArgument(precision >= 0, "Illegal precision %s", precision);

        return getStat().entrySet().stream().collect(Collectors.toMap(
                e -> format(e.getKey(), precision),
                Map.Entry::getValue,
                Long::sum,
                LinkedHashMap::new
        ));
    }

    private static String format(double value, int precision) {
        return String.format(DEFAULT_LOCALE, "%." + precision + "f", value);
    }

    @Override
    public String toString() {
        return "LogStat{" +
                "stat=" + stat +
                ", base=" + base +
                ", logBase=" + logBase +
                ", mantissa=" + mantissa +
                ", roundMode=" + roundMode +
                '}';
    }
}
