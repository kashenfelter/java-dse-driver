/*
 *      Copyright (C) 2012-2016 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dse.graph;

import org.apache.tinkerpop.gremlin.process.traversal.step.util.Tree;
import org.assertj.core.api.MapAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class TinkerTreeAssert<T> extends MapAssert<T, Tree<T>> {

    public TinkerTreeAssert(Tree<T> actual) {
        super(actual);
    }

    public TinkerTreeAssert<T> hasTree(T key) {
        assertThat(actual).containsKey(key);
        return this;
    }

    public TinkerTreeAssert<T> isLeaf() {
        assertThat(actual).hasSize(0);
        return this;
    }

    public TinkerTreeAssert<T> tree(T key) {
        hasTree(key);
        return new TinkerTreeAssert<>(actual.get(key));
    }

}
