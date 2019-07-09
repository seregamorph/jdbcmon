package org.jdbcmon;

import static java.util.Comparator.reverseOrder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class SqlStat {

    private static final double AVG_FACTOR = 0.2d;
    private static final int MAP_SIZE = 256;
    /**
     * Multiplier constant to convert nanos to millis
     */
    private static final double NANOS_TO_MILLIS_MULTIPLIER = 0.000001d;

    private final long startNanos = System.nanoTime();

    private long totalPrepareCount;
    private long totalExecuteCount;
    private long totalExecuteTimeNanos;
    private long totalFetchTimeNanos;

    private final LimitedMap<String, SqlStatementStat> sqlMap = new LimitedMap<>(MAP_SIZE);

    SqlStat() {
    }

    synchronized List<Map<String, ?>> shortReport() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalExecuteCount", totalExecuteCount);
        map.put("totalExecuteTimeMs", TimeUnit.NANOSECONDS.toMillis(totalExecuteTimeNanos));
        map.put("avgActive", getAvgActive());
        return Arrays.asList(map);
    }

    synchronized List<Map<String, ?>> report(/*@Nullable*/ String sort, boolean plain) {
        long uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(getUptimeMs());

        List<Map<String, ?>> res = new ArrayList<>();

        res.addAll(shortReport());

        List<SqlStatementStat> statsToSort = sqlMap.copyValues();
        statsToSort.sort(getComparator(sort));

        for (SqlStatementStat statEntry : statsToSort) {
            res.add(statEntry.getStat(uptimeSeconds, plain));
        }

        return res;
    }

    private static Comparator<SqlStatementStat> getComparator(/*@Nullable*/ String sort) {
        if ("query".equals(sort)) {
            return Comparator.comparing(e -> e.sql, String.CASE_INSENSITIVE_ORDER);
        } else if ("totalExecuteTime".equals(sort)) {
            return Comparator.comparing(e -> e.totalExecuteTimeNanos, reverseOrder());
        } else if ("executeAvgTotalTime".equals(sort)) {
            return Comparator.comparing(e -> avg(e.totalExecuteTimeNanos, e.executeCount), reverseOrder());
        }
//        else if ("maxExecuteTime".equals(sort)) {
//            return Comparator.comparing(e -> e.maxExecuteTimeMs, reverseOrder());
//        }
        return Comparator.comparing(e -> e.executeCount, reverseOrder());
    }

    synchronized void registerPrepare(String sql, /*@Nullable*/ Throwable exception) {
        totalPrepareCount++;

        getSqlStat(sql).incPrepareCount(exception);
    }

    synchronized void registerExecute(String sql, long timeNanos, /*@Nullable*/ Throwable exception) {
        totalExecuteCount++;
        totalExecuteTimeNanos += timeNanos;

        getSqlStat(sql).incExecuteCount(timeNanos, exception);
    }

    synchronized void registerFetch(String sql, long timeNanos) {
        totalFetchTimeNanos += timeNanos;

        getSqlStat(sql).incFetchTime(timeNanos);
    }

    synchronized void registerBatchSize(String sql, int batchSize) {
        getSqlStat(sql).registerBatch(batchSize);
    }

    synchronized void registerUpdate(String sql, int result) {
        getSqlStat(sql).registerUpdate(result);
    }

    synchronized void registerResultSetSize(String sql, int resultSetSize) {
        getSqlStat(sql).registerResultSetSize(resultSetSize);
    }

    private String getAvgActive() {
        // average active connections since application started
        // evaluated as sum(connection active time) / (app lifetime)
        // this value can be more than 1.0
        long totalHoldTimeMs = TimeUnit.NANOSECONDS.toMillis(totalExecuteTimeNanos + totalFetchTimeNanos);
        return formatAvg(totalHoldTimeMs, getUptimeMs(), 1.0d);
    }

    private long getUptimeMs() {
        long uptime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        return uptime == 0 ? 1000L : uptime;
    }

    private static double avg(long total, long count) {
        if (count == 0L) {
            return 0.0d;
        }
        return ((double) total) / count;
    }

    static String formatAvg(long total, long count, double multiplier) {
        double value = avg(total, count) * multiplier;
        return String.format(Locale.ENGLISH, "%.2f", value);
    }

    synchronized void reset() {
        sqlMap.clear();
        totalPrepareCount = 0L;
        totalExecuteCount = 0L;
        totalExecuteTimeNanos = 0L;
        totalFetchTimeNanos = 0L;
    }

    //@Nonnull
    private SqlStatementStat getSqlStat(/*@Nullable*/ String sql) {
        assert Thread.holdsLock(this);
        String key = sql == null ? "[null]" : sql;
        return sqlMap.computeIfAbsent(key, () -> new SqlStatementStat(key));
    }

    //@NotThreadSafe
    private static class ExceptionStat {
        private int count;
        //@Nullable
        private String stackTrace;

        private ExceptionStat() {
        }

        int getCount() {
            return count;
        }

        //@Nullable
        String getStackTrace() {
            return stackTrace;
        }

        void setStackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
        }

        void incCount() {
            count++;
        }

        @Override
        public String toString() {
            return "ExceptionStat{" +
                    "count=" + count +
                    ", stackTrace=" + stackTrace +
                    '}';
        }
    }

    private static final double BASE = Math.sqrt(2.0d);

    /**
     * Not thread safe
     */
    private static class SqlStatementStat {
        final LimitedMap<Integer, AtomicInteger> update = new LimitedMap<>(16);
        final LimitedMap<Integer, AtomicInteger> batch = new LimitedMap<>(16);
        /**
         * Exception string -> stat
         */
        final LimitedMap<String, ExceptionStat> exceptions = new LimitedMap<>(5);
        final LogStat executeStat = new LogStat(BASE, RoundMode.ROUND);
        final AvgValue avgExecuteTimeMs = new AvgValue(AVG_FACTOR);
        final AvgValue avgFetchTimeMs = new AvgValue(AVG_FACTOR);

        final String sql;

        long totalExecuteTimeNanos;
        long totalEmptyResultSets;
        long totalResultSetSize;
        long totalFetchTimeNanos;
        long prepareCount;
        long executeCount;
        long failExecuteCount;

        SqlStatementStat(String sql) {
            this.sql = sql;
        }

        void incPrepareCount(/*@Nullable*/ Throwable exception) {
            prepareCount++;
        }

        void incExecuteCount(long timeNanos, /*@Nullable*/ Throwable exception) {
            executeCount++;
            totalExecuteTimeNanos += timeNanos;
            double timeMs = timeNanos * NANOS_TO_MILLIS_MULTIPLIER;
            avgExecuteTimeMs.update(timeMs);
            executeStat.addValue(timeMs);
            if (exception != null) {
                failExecuteCount++;
                registerException(exception);
            }
        }

        void incFetchTime(long timeNanos) {
            totalFetchTimeNanos += timeNanos;
            avgFetchTimeMs.update(timeNanos * NANOS_TO_MILLIS_MULTIPLIER);
        }

        void registerBatch(int batchSize) {
            batch.computeIfAbsent(batchSize, AtomicInteger::new).incrementAndGet();
        }

        void registerUpdate(int result) {
            update.computeIfAbsent(result, AtomicInteger::new).incrementAndGet();
        }

        void registerResultSetSize(int resultSetSize) {
            if (resultSetSize > 0) {
                totalResultSetSize += resultSetSize;
            } else {
                totalEmptyResultSets++;
            }
        }

        Map<String, Object> getStat(long uptimeSeconds, boolean plain) {
            Map<String, Object> map = new LinkedHashMap<>();

            map.put("sql", plain ? sql.replace('\n', ' ').replace('\r', ' ') : sql);

            if (prepareCount > 0) {
                map.put("prepareCount", prepareCount);
            }
            map.put("executeCount", executeCount);
            map.put("execPerMinute", formatAvg(executeCount, uptimeSeconds, 60));

            if (failExecuteCount != 0) {
                map.put("failExecuteCount", failExecuteCount);
            }

            map.put("totalExecuteTime", TimeUnit.NANOSECONDS.toMillis(totalExecuteTimeNanos));
            map.put("executeAvgTotalTime", formatAvg(totalExecuteTimeNanos, executeCount, NANOS_TO_MILLIS_MULTIPLIER));
            map.put("executeAvgFloatTime", avgExecuteTimeMs.toString(2));
            map.put("executeTime", executeStat.getReportStat(2));

            if (totalFetchTimeNanos > 0) {
                map.put("totalFetchTime", TimeUnit.NANOSECONDS.toMillis(totalFetchTimeNanos));
                map.put("fetchAvgTotalTime", formatAvg(totalFetchTimeNanos, executeCount, NANOS_TO_MILLIS_MULTIPLIER));
                map.put("fetchAvgFloatTime", avgFetchTimeMs.toString(2));
            }

            if (!batch.isEmpty()) {
                SortedMap<Integer, Integer> batchMap = new TreeMap<>();
                batch.forEach((key, value) -> batchMap.put(key, value.intValue()));
                map.put("batch", batchMap);
            }
            if (!update.isEmpty()) {
                SortedMap<Integer, Integer> updateMap = new TreeMap<>();
                update.forEach((key, value) -> updateMap.put(key, value.intValue()));
                map.put("update", updateMap);
            }

            if (totalResultSetSize > 0) {
                map.put("totalResultSetSize", totalResultSetSize);
            }
            if (totalEmptyResultSets > 0) {
                map.put("totalEmptyResultSets", totalEmptyResultSets);
            }

            List<Map<String, Object>> exList = new ArrayList<>();
            exceptions.forEach((key, exceptionStat) -> {
                Map<String, Object> exMap = new LinkedHashMap<>();
                exMap.put("message", key);
                exMap.put("count", exceptionStat.getCount());
                String stackTrace = exceptionStat.getStackTrace();
                if (stackTrace != null) {
                    exMap.put("stackTrace", stackTrace);
                }
                exList.add(exMap);
            });
            if (!exList.isEmpty()) {
                map.put("exceptions", exList);
            }

            return map;
        }

        void registerException(Throwable exception) {
            String key = exception.toString();
            ExceptionStat exceptionStat = this.exceptions.computeIfAbsent(key, ExceptionStat::new);
            exceptionStat.incCount();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            exceptionStat.setStackTrace(sw.toString());
        }
    }
}
