/*
 *      Copyright (C) 2012-2016 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.core;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;

import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class AtomicMonotonicTimestampGeneratorTest {

    @Test(groups = "unit")
    public void should_generate_incrementing_timestamps_for_all_threads() throws InterruptedException {
        // Create a generator with a fixed millisecond value
        final long fixedTime = 1;
        final AtomicMonotonicTimestampGenerator generator = new AtomicMonotonicTimestampGenerator();
        generator.clock = new MockClocks.FixedTimeClock(fixedTime);

        MemoryAppender appender = new MemoryAppender();
        Logger logger = Logger.getLogger(TimestampGenerator.class);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.WARN);
        logger.addAppender(appender);

        try {
            // Generate 1000 timestamps shared among multiple threads
            final int testThreadsCount = 2;
            assertEquals(1000 % testThreadsCount, 0);
            final SortedSet<Long> allTimestamps = new ConcurrentSkipListSet<Long>();
            ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(testThreadsCount));

            List<ListenableFuture<?>> futures = Lists.newArrayListWithExpectedSize(testThreadsCount);
            for (int i = 0; i < testThreadsCount; i++) {
                futures.add(executor.submit(
                        new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < 1000 / testThreadsCount; i++)
                                    allTimestamps.add(generator.next());
                            }
                        }));
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);

            try {
                Futures.allAsList(futures).get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof AssertionError)
                    throw (AssertionError) cause;
                else
                    fail("Error in a test thread", cause);
            }

            // Ensure that the 1000 microseconds for the mocked millisecond value have been generated
            int i = 0;
            for (Long timestamp : allTimestamps) {
                Long expected = fixedTime + i;
                assertEquals(timestamp, expected);
                i += 1;
            }
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(originalLevel);
        }
    }
}
