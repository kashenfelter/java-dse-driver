/*
 *      Copyright (C) 2012-2016 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.dse;

import com.datastax.driver.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import static com.datastax.driver.core.CCMBridge.Builder.RANDOM_PORT;

@CCMConfig(ccmProvider = "configureCCM")
public class CCMDseTestsSupport extends CCMTestsSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CCMDseTestsSupport.class);

    @Override
    protected void initTestContext(Object testInstance, Method testMethod) throws Exception {
        super.initTestContext(testInstance, testMethod);
        // TODO remove this once DSE startup stabilizes.
        for (InetSocketAddress node : getContactPointsWithPorts()) {
            LOGGER.debug("Waiting for binary protocol to show up for {}", node);
            TestUtils.waitUntilPortIsUp(node);
        }
    }

    @Override
    public DseCluster.Builder createClusterBuilder() {
        return DseCluster.builder()
                .withCodecRegistry(new CodecRegistry())
                .withQueryOptions(TestUtils.nonDebouncingQueryOptions());
    }

    public CCMBridge.Builder configureCCM() {
        CCMBridge.Builder builder = CCMBridge.builder();
        // Acquire a unique port for the netty lease port.
        if (VersionNumber.parse(CCMBridge.getDSEVersion()).getMajor() >= 5) {
            builder = builder.withDSEConfiguration("lease_netty_server_port", RANDOM_PORT)
                    .withDSEConfiguration("internode_messaging_options.port", RANDOM_PORT);
        }
        return builder;
    }


}
