/*
 *      Copyright (C) 2012-2017 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.core;

import org.apache.log4j.Level;
import org.jboss.byteman.contrib.bmunit.BMNGListener;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.datastax.driver.core.Cluster.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Simple test of the Sessions methods against a one node cluster.
 */
@BMUnitConfig(loadDirectory = "target/test-classes")
@Listeners(BMNGListener.class)
@CCMConfig(createCluster = false)
public class SessionErrorTest extends ScassandraTestBase {

    private ScassandraCluster scassandra;
    private Cluster cluster;

    @BeforeClass(groups = "short")
    public void setUp() throws Exception {
        scassandra = ScassandraCluster.builder().withNodes(2).build();
        scassandra.init();
        cluster = builder()
                .addContactPoints(scassandra.address(1).getAddress())
                .withPort(scassandra.getBinaryPort())
                .build();
        cluster.init();
    }

    @AfterClass(groups = "short")
    public void tearDown() throws Exception {
        cluster.close();
        scassandra.stop();
    }

    @Test(groups = "short")
    @BMRule(name = "emulate OOME",
            targetClass = "com.datastax.driver.core.Connection$4",
            targetMethod = "apply(Void)",
            action = "throw new OutOfMemoryError(\"not really\")"
    )
    public void should_propagate_errors() {
        try {
            cluster.connect();
            fail("Expecting OOME");
        } catch (OutOfMemoryError e) {
            assertThat(e).hasMessage("not really");
        }
    }

    @Test(groups = "short")
    @BMRule(name = "emulate NPE",
            targetClass = "com.datastax.driver.core.Connection$4",
            targetMethod = "apply(Void)",
            action = "throw new NullPointerException(\"not really\")"
    )
    public void should_not_propagate_unchecked_exceptions() {
        Level previous = TestUtils.setLogLevel(HostConnectionPool.class, Level.WARN);
        MemoryAppender logs = new MemoryAppender().enableFor(HostConnectionPool.class);
        try {
            Session session = cluster.connect();
            // Pool to host1 should be still open because host1 is the control host,
            // but its pool should have no active connection
            // Pool to host2 should have been closed because host2 has no
            // more active connections
            Session.State state = session.getState();
            Host host1 = scassandra.host(cluster, 1, 1);
            Host host2 = scassandra.host(cluster, 1, 2);
            assertThat(state.getConnectedHosts()).hasSize(1).containsExactly(host1);
            assertThat(state.getOpenConnections(host1)).isEqualTo(0); // pool open but empty
            assertThat(state.getOpenConnections(host2)).isEqualTo(0); // pool closed
            assertThat(logs.get())
                    .contains(
                            "Unexpected error during transport initialization",
                            "not really",
                            NullPointerException.class.getSimpleName(),
                            "com.datastax.driver.core.Connection$4.apply");
        } finally {
            TestUtils.setLogLevel(HostConnectionPool.class, previous);
            logs.disableFor(HostConnectionPool.class);
        }
    }

}
