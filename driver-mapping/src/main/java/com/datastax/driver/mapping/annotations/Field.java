/*
 * Copyright (C) 2012-2017 DataStax Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.mapping.annotations;

import com.datastax.driver.core.TypeCodec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that allows to specify the name of the CQL UDT field to which the
 * Java field or bean property should be mapped.
 * <p/>
 * Note that this annotation is generally optional in the sense that any field
 * or any getter method of a Java bean property of a class annotated by {@link UDT}
 * will be mapped by default to a UDT field
 * having the same name than this Java field / bean property unless that Java field or method has the
 * {@link Transient} annotation. As such, this annotation is mainly useful when
 * the UDT field name does not correspond to the Java field or property name itself.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Field {
    /**
     * Name of the column being mapped in Cassandra. By default, the name of the
     * Java field or bean property will be used.
     *
     * @return the name of the mapped column in Cassandra, or {@code ""} to use
     * the field name.
     */
    String name() default "";

    /**
     * Whether the column name is a case sensitive one.
     *
     * @return whether the column name is a case sensitive one.
     */
    boolean caseSensitive() default false;

    /**
     * A custom codec that will be used to serialize and deserialize the column.
     *
     * @return the codec's class. It must have a no-argument constructor (the mapper
     * will create an instance and cache it).
     */
    Class<? extends TypeCodec<?>> codec() default Defaults.NoCodec.class;
}
