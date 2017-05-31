/*
 *      Copyright (C) 2012-2017 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.mapping;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default {@link HierarchyScanStrategy}.
 * <p/>
 * This strategy assumes that there exists a common ancestor
 * for all mapped classes in the application, and allows all its
 * descendants (optionally including itself) to be scanned for annotations.
 */
public class DefaultHierarchyScanStrategy implements HierarchyScanStrategy {

    private final Class<?> highestAncestor;

    private final boolean included;

    /**
     * Creates a new instance with defaults:
     * the common ancestor is {@link Object} excluded, which implies
     * that every ancestor of a mapped class, except {@code Object} itself,
     * will be scanned for annotations.
     */
    public DefaultHierarchyScanStrategy() {
        this(Object.class, false);
    }

    /**
     * Creates a new instance with the given highest common ancestor.
     *
     * @param highestAncestor The highest ancestor class to consider; cannot be {@code null}.
     * @param included        Whether or not to include the highest ancestor itself.
     */
    public DefaultHierarchyScanStrategy(Class<?> highestAncestor, boolean included) {
        checkNotNull(highestAncestor);
        this.highestAncestor = highestAncestor;
        this.included = included;
    }

    @Override
    public List<Class<?>> filterClassHierarchy(Class<?> mappedClass) {
        List<Class<?>> classesToScan = new ArrayList<Class<?>>();
        Class<?> highestAncestor = this.highestAncestor;
        for (Class<?> clazz = mappedClass; clazz != null; clazz = clazz.getSuperclass()) {
            if (!clazz.equals(highestAncestor) || included) {
                classesToScan.add(clazz);
            }
            if (clazz.equals(highestAncestor)) {
                break;
            }
        }
        return classesToScan;
    }
}
