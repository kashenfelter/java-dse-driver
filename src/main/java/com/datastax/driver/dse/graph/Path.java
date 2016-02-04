/*
 *      Copyright (C) 2012-2016 DataStax Inc.
 */
package com.datastax.driver.dse.graph;

import com.google.common.base.Objects;

import java.util.List;
import java.util.Set;

/**
 * A walk through a graph as defined by a traversal.
 * <p/>
 * Clients typically obtain instances of this class by calling {@link GraphResult#asVertex()}, for example:
 * <pre>
 *     GraphResult r = dseSession.executeGraph("g.V().hasLabel('some_vertex').outE().inV().path()").one();
 *     Path path = r.asPath();
 * </pre>
 */
public class Path {
    private List<Set<String>> labels;
    private List<GraphResult> objects;

    Path(List<Set<String>> labels, List<GraphResult> objects) {
        this.labels = labels;
        this.objects = objects;
    }

    /**
     * Returns the sets of labels of the steps traversed.
     *
     * @return the labels.
     */
    public List<Set<String>> getLabels() {
        return labels;
    }

    /**
     * Returns the objects traversed.
     *
     * @return the objects.
     */
    public List<GraphResult> getObjects() {
        return objects;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof Path) {
            Path that = (Path) other;
            return this.labels.equals(that.labels)
                    && this.objects.equals(that.objects);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(labels, objects);
    }
}