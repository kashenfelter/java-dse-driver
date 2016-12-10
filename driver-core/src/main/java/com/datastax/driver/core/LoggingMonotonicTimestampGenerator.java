/*
 *      Copyright (C) 2012-2016 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A monotonic timestamp generator that logs warnings when timestamps drift in the future
 * (see this class's constructors and {@link #onDrift(long, long)} for more information).
 */
public abstract class LoggingMonotonicTimestampGenerator extends AbstractMonotonicTimestampGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimestampGenerator.class);

    private final long warningThresholdMicros;
    private final long warningIntervalMillis;

    private final AtomicLong lastDriftWarning = new AtomicLong(Long.MIN_VALUE);

    /**
     * Creates a new instance.
     *
     * @param warningThreshold     how far in the future timestamps are allowed to drift before a warning is logged.
     * @param warningThresholdUnit the unit for {@code warningThreshold}.
     * @param warningInterval      how often the warning will be logged if timestamps keep drifting above the threshold.
     * @param warningIntervalUnit  the unit for {@code warningIntervalUnit}.
     */
    protected LoggingMonotonicTimestampGenerator(
            long warningThreshold, TimeUnit warningThresholdUnit,
            long warningInterval, TimeUnit warningIntervalUnit) {
        this.warningThresholdMicros = MICROSECONDS.convert(warningThreshold, warningThresholdUnit);
        this.warningIntervalMillis = MILLISECONDS.convert(warningInterval, warningIntervalUnit);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation logs a warning at regular intervals when timestamps drift more than a specified threshold in
     * the future. These messages are emitted at {@code WARN} level in the category
     * {@code com.datastax.driver.core.TimestampGenerator}.
     *
     * @param currentTick   the current clock tick.
     * @param lastTimestamp the last timestamp that was generated.
     */
    protected void onDrift(long currentTick, long lastTimestamp) {
        if (LOGGER.isWarnEnabled() && warningThresholdMicros >= 0 && lastTimestamp > currentTick + warningThresholdMicros) {
            long now = System.currentTimeMillis();
            long lastWarning = lastDriftWarning.get();
            if (now > lastWarning + warningIntervalMillis && lastDriftWarning.compareAndSet(lastWarning, now)) {
                LOGGER.warn(
                        "Clock skew detected: current tick ({}) was {} microseconds behind the last generated timestamp ({}), " +
                                "returned timestamps will be artificially incremented to guarantee monotonicity.",
                        currentTick, lastTimestamp - currentTick, lastTimestamp);
            }
        }
    }
}