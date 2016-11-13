package org.jdbcmon;

import java.util.Locale;

/**
 * Value with statistics approximation
 */
class AvgValue {
    private final double avgFactor;

    private double currentValue;

    /**
     * Creates AvgValue with default value
     *
     * @param avgFactor    statistics approximation factor (usually less than 0.25)
     * @param defaultValue
     */
    AvgValue(double avgFactor, double defaultValue) {
        this.avgFactor = checkAvgFactor(avgFactor);
        this.currentValue = defaultValue;
    }

    /**
     * Creates AvgValue without default value (NaN)
     *
     * @param avgFactor statistics approximation factor (usually less than 0.25)
     */
    AvgValue(double avgFactor) {
        this.avgFactor = checkAvgFactor(avgFactor);
        this.currentValue = Double.NaN;
    }

    private static double checkAvgFactor(double avgFactor) throws IllegalArgumentException {
        if (avgFactor <= 0.0d || avgFactor > 1.0d) {
            throw new IllegalArgumentException(String.format("Illegal avg factor %f", avgFactor));
        }
        return avgFactor;
    }

    private void update(double value, double avgFactor) {
        checkAvgFactor(avgFactor);
        double prevValueWeight = 1.0d - avgFactor;
        synchronized (this) {
            if (Double.isNaN(currentValue)) {
                currentValue = value;
            } else {
                currentValue = currentValue * prevValueWeight + value * avgFactor;
            }
        }
    }

    /**
     * Update approximate value
     *
     * @param value current value
     */
    void update(double value) {
        update(value, this.avgFactor);
    }

    double getAvgFactor() {
        return avgFactor;
    }

    synchronized boolean isInitialized() {
        return !Double.isNaN(currentValue);
    }

    /**
     * Current approx value. NaN means that it is not initialized yet
     */
    synchronized double getCurrentValueOrNaN() {
        return currentValue;
    }

    synchronized Double getCurrentValueOrNull() {
        if (Double.isNaN(currentValue)) {
            return null;
        }
        return currentValue;
    }

    synchronized String toString(int precision) {
        if (precision < 0) {
            throw new IllegalArgumentException("Illegal precision " + precision);
        }
        return isInitialized() ? String.format(Locale.ENGLISH, "%." + precision + "f", currentValue) : "null";
    }

    @Override
    public String toString() {
        return toString(4);
    }
}
