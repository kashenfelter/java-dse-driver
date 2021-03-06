/*
 * Copyright (C) 2012-2017 DataStax Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.core;

/**
 * Collection of (typed) CQL values that can be set either by index (starting at zero) or by name.
 */
public interface SettableData<T extends SettableData<T>> extends SettableByIndexData<T>, SettableByNameData<T> {
}
