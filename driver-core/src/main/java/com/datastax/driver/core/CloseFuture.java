/*
 *      Copyright (C) 2012-2017 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.core;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.List;

/**
 * A future on the shutdown of a Cluster or Session instance.
 * <p/>
 * This is a standard future except for the fact that this class has an
 * additional {@link #force} method that can be used to expedite the shutdown
 * process (see below).
 * <p/>
 * Note that this class implements <a href="http://github.com/google/guava/">Guava</a>'s {@code
 * ListenableFuture} and can so be used with Guava's future utilities.
 */
public abstract class CloseFuture extends AbstractFuture<Void> {

    CloseFuture() {
    }

    static CloseFuture immediateFuture() {
        CloseFuture future = new CloseFuture() {
            @Override
            public CloseFuture force() {
                return this;
            }
        };
        future.set(null);
        return future;
    }

    /**
     * Try to force the completion of the shutdown this is a future of.
     * <p/>
     * This method will do its best to expedite the shutdown process. In
     * particular, all connections will be closed right away, even if there are
     * ongoing queries at the time this method is called.
     * <p/>
     * Note that this method does not block. The completion of this method does
     * not imply the shutdown process is done, you still need to wait on this
     * future to ensure that, but calling this method will ensure said
     * future will return in a timely way.
     *
     * @return this {@code CloseFuture}.
     */
    public abstract CloseFuture force();

    // Internal utility for cases where we want to build a future that wait on other ones
    static class Forwarding extends CloseFuture {

        private final List<CloseFuture> futures;

        Forwarding(List<CloseFuture> futures) {
            this.futures = futures;

            Futures.addCallback(Futures.allAsList(futures), new FutureCallback<List<Void>>() {
                @Override
                public void onFailure(Throwable t) {
                    Forwarding.this.setException(t);
                }

                @Override
                public void onSuccess(List<Void> v) {
                    Forwarding.this.onFuturesDone();
                }
            });
        }

        @Override
        public CloseFuture force() {
            for (CloseFuture future : futures)
                future.force();
            return this;
        }

        protected void onFuturesDone() {
            set(null);
        }
    }
}
