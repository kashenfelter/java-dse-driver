/*
 *      Copyright (C) 2012-2016 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dse.graph;

import com.datastax.driver.core.Assertions;
import org.apache.tinkerpop.gremlin.structure.Edge;

public class TinkerEdgeAssert extends TinkerElementAssert<TinkerEdgeAssert, Edge> {

    public TinkerEdgeAssert(Edge actual) {
        super(actual, TinkerEdgeAssert.class);
    }

    public TinkerEdgeAssert hasInVLabel(String label) {
        Assertions.assertThat(actual.inVertex().label()).isEqualTo(label);
        return myself;
    }

    public TinkerEdgeAssert hasOutVLabel(String label) {
        Assertions.assertThat(actual.outVertex().label()).isEqualTo(label);
        return myself;
    }

}
