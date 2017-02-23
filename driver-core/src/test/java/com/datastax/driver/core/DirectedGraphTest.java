/*
 *      Copyright (C) 2012-2017 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.core;

import com.datastax.driver.core.exceptions.DriverInternalError;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectedGraphTest {
    @Test(groups = "unit")
    public void should_sort_empty_graph() {
        DirectedGraph<String> g = new DirectedGraph<String>();
        assertThat(g.topologicalSort()).isEmpty();
    }

    @Test(groups = "unit")
    public void should_sort_graph_with_one_node() {
        DirectedGraph<String> g = new DirectedGraph<String>("A");
        assertThat(g.topologicalSort())
                .containsExactly("A");
    }

    @Test(groups = "unit")
    public void should_sort_complex_graph() {
        //         H   G
        //        / \ /\
        //       F   |  E
        //        \ /  /
        //         D  /
        //        / \/
        //        B  C
        //        |
        //        A
        DirectedGraph<String> g = new DirectedGraph<String>("A", "B", "C", "D", "E", "F", "G", "H");
        g.addEdge("H", "F");
        g.addEdge("G", "E");
        g.addEdge("H", "D");
        g.addEdge("F", "D");
        g.addEdge("G", "D");
        g.addEdge("D", "C");
        g.addEdge("E", "C");
        g.addEdge("D", "B");
        g.addEdge("B", "A");

        // Topological sort order should be : GH,FE,D,CB,A
        // There's no guarantee on the order within the same level, so we use sublists:
        List<String> sorted = g.topologicalSort();
        assertThat(sorted.subList(0, 2))
                .contains("G", "H");
        assertThat(sorted.subList(2, 4))
                .contains("F", "E");
        assertThat(sorted.subList(4, 5))
                .contains("D");
        assertThat(sorted.subList(5, 7))
                .contains("C", "B");
        assertThat(sorted.subList(7, 8))
                .contains("A");
    }

    @Test(groups = "unit", expectedExceptions = DriverInternalError.class)
    public void should_fail_to_sort_if_graph_has_a_cycle() {
        DirectedGraph<String> g = new DirectedGraph<String>("A", "B", "C");
        g.addEdge("A", "B");
        g.addEdge("B", "C");
        g.addEdge("C", "B");

        g.topologicalSort();
    }

    @Test(groups = "unit", expectedExceptions = DriverInternalError.class)
    public void should_fail_to_sort_if_graph_is_a_cycle() {
        DirectedGraph<String> g = new DirectedGraph<String>("A", "B", "C");
        g.addEdge("A", "B");
        g.addEdge("B", "C");
        g.addEdge("C", "A");

        g.topologicalSort();
    }
}