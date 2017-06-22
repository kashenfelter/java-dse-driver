/*
 * Copyright (C) 2012-2017 DataStax Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.mapping.annotations;

import com.datastax.driver.mapping.Mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines to which table a class must be mapped to.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    /**
     * The name of the keyspace the table is part of.
     *
     * @return the name of the keyspace.
     */
    String keyspace() default "";

    /**
     * The name of the table.
     *
     * @return the name of the table.
     */
    String name();

    /**
     * Whether the keyspace name is a case sensitive one.
     *
     * @return whether the keyspace name is a case sensitive one.
     */
    boolean caseSensitiveKeyspace() default false;

    /**
     * Whether the table name is a case sensitive one.
     *
     * @return whether the table name is a case sensitive one.
     */
    boolean caseSensitiveTable() default false;

    /**
     * The consistency level to use for the write operations provded by the {@link Mapper} class.
     *
     * @return the consistency level to use for the write operations provded by the {@link Mapper} class.
     */
    String writeConsistency() default "";

    /**
     * The consistency level to use for the read operations provded by the {@link Mapper} class.
     *
     * @return the consistency level to use for the read operations provded by the {@link Mapper} class.
     */
    String readConsistency() default "";
}
