/*
 *      Copyright (C) 2012-2016 DataStax Inc.
 */
package com.datastax.driver.dse;

import com.datastax.driver.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

@CCMConfig(ccmProvider = "configureCCM")
public class CCMDseTestsSupport extends CCMTestsSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CCMDseTestsSupport.class);

    @Override
    protected void initTestContext(Object testInstance, Method testMethod) throws Exception {
        super.initTestContext(testInstance, testMethod);
        // TODO remove this once DSE startup stabilizes.
        LOGGER.debug("Waiting for binary protocol to show up");
        for (InetSocketAddress node : getContactPointsWithPorts()) {
            TestUtils.waitUntilPortIsUp(node);
        }
    }

    @Override
    public Cluster.Builder createClusterBuilder() {
        return DseCluster.builder()
                .withCodecRegistry(new CodecRegistry())
                .withQueryOptions(TestUtils.nonDebouncingQueryOptions());
    }

    @SuppressWarnings("unused")
    public CCMBridge.Builder configureCCM() {
        CCMBridge.Builder builder = CCMBridge.builder();
        // Acquire a unique port for the netty lease port.
        if (VersionNumber.parse(CCMBridge.getDSEVersion()).getMajor() >= 5) {
            int leasePort = TestUtils.findAvailablePort();
            builder = builder.withDSEConfiguration("lease_netty_server_port", leasePort);
        }
        return builder;
    }


}