/*
 * Copyright (C) 2012-2017 DataStax Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.mapping.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the CQL query that an {@link Accessor} method must implement.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Query {
    /**
     * The CQL query to use.
     * <p/>
     * In that query string, the parameter of the annotated method can be referenced using
     * name bind markers. For instance, the first parameter can be refered by {@code :arg0},
     * the second one by {@code :arg1}, ... Alternatively, if a parameter of the annonated
     * method has a {@link Param} annotation, the value of that latter annoation should be
     * used instead.
     *
     * @return the CQL query to use.
     */
    String value();
}
