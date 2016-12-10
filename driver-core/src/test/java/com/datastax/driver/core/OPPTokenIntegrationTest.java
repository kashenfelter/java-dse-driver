/*
 *      Copyright (C) 2012-2016 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.core;

import com.datastax.driver.core.Token.OPPToken;

@CCMConfig(options = "-p ByteOrderedPartitioner")
public class OPPTokenIntegrationTest extends TokenIntegrationTest {

    public OPPTokenIntegrationTest() {
        super(DataType.blob(), false);
    }

    @Override
    protected Token.Factory tokenFactory() {
        return OPPToken.FACTORY;
    }
}