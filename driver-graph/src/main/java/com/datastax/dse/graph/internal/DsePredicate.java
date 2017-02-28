/*
 *      Copyright (C) 2012-2017 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dse.graph.internal;

import com.google.common.base.Preconditions;

import java.util.function.BiPredicate;

/**
 * An extension of TinkerPop's {@link BiPredicate} adding simple pre-condition checking
 * methods that have to be written in the implementations.
 */
public interface DsePredicate extends BiPredicate<Object, Object> {

    default void preEvaluate(Object condition) {
        Preconditions.checkArgument(this.isValidCondition(condition), "Invalid condition provided: %s", condition);
    }

    boolean isValidCondition(Object condition);
}
