/*
 *      Copyright (C) 2012-2016 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.core.policies;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.Statement;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sample load balancing policy that limits the number of nodes the driver connects to.
 * <p/>
 * If more nodes are available, they are marked as IGNORED. When one of the "chosen" nodes
 * goes down, the policy picks one of the ignored nodes to replace it.
 * <p/>
 * This kind of policy can be used to alleviate the load on a cluster that has a lot of
 * clients.
 * <p/>
 * For simplicity, this policy does not distinguish LOCAL and REMOTE nodes.
 */
public class LimitingLoadBalancingPolicy extends DelegatingLoadBalancingPolicy {
    private final int maxHosts;
    private final int threshold;
    private final Set<Host> liveHosts = Sets.newSetFromMap(new ConcurrentHashMap<Host, Boolean>());
    private final Set<Host> chosenHosts = Sets.newSetFromMap(new ConcurrentHashMap<Host, Boolean>());
    private final Lock updateLock = new ReentrantLock();

    private volatile Cluster cluster;

    /**
     * @param delegate  the underlying policy that will be fed with the chosen nodes
     * @param maxHosts  the maximum number of chosen nodes
     * @param threshold how many chosen nodes we accept to lose before we start picking new ones
     */
    public LimitingLoadBalancingPolicy(LoadBalancingPolicy delegate, int maxHosts, int threshold) {
        super(delegate);
        this.maxHosts = maxHosts;
        this.threshold = threshold;
    }

    @Override
    public void init(Cluster cluster, Collection<Host> hosts) {
        this.cluster = cluster;

        Iterator<Host> hostIt = hosts.iterator();
        while (hostIt.hasNext() && chosenHosts.size() <= maxHosts - threshold) {
            chosenHosts.add(hostIt.next());
        }

        this.delegate.init(cluster, new ArrayList<Host>(chosenHosts));
    }

    private void updateChosenHosts() {
        if (chosenHosts.size() > maxHosts - threshold || liveHosts.size() == 0)
            return;

        // We lock to prevent two events from triggering this simultaneously.
        updateLock.lock();
        try {
            int missing = maxHosts - chosenHosts.size();
            if (missing < threshold || liveHosts.size() == 0)
                return;
            Set<Host> newlyChosen = new HashSet<Host>();

            for (Host host : liveHosts) {
                // Note that this picks hosts whatever their distance is.
                // We can't reliably call childPolicy.distance() here, because the childPolicy
                // might require hosts to be already added to compute their distance properly
                // (this is the case for DCAware policy).
                newlyChosen.add(host);
                missing -= 1;
                if (missing == 0)
                    break;
            }

            chosenHosts.addAll(newlyChosen);
            liveHosts.removeAll(newlyChosen);
            for (Host host : newlyChosen) {
                delegate.onAdd(host);

                // delegate should have updated the distance, inform the driver so that it can
                // recreate the pool.
                cluster.getConfiguration().getPoolingOptions().refreshConnectedHost(host);
            }
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public HostDistance distance(Host host) {
        if (chosenHosts.contains(host))
            return delegate.distance(host);
        else
            return HostDistance.IGNORED;
    }

    @Override
    public Iterator<Host> newQueryPlan(String loggedKeyspace, Statement statement) {
        // Since we only add chosen nodes to the child policy, its query plan will only contain chosen nodes
        return delegate.newQueryPlan(loggedKeyspace, statement);
    }

    @Override
    public void onAdd(Host host) {
        liveHosts.add(host);
        // update in case we didn't have enough chosen hosts before the addition
        updateChosenHosts();
    }

    @Override
    public void onUp(Host host) {
        onAdd(host);
    }

    @Override
    public void onDown(Host host) {
        delegate.onDown(host);

        liveHosts.remove(host);
        chosenHosts.remove(host);
        updateChosenHosts();
    }

    @Override
    public void onRemove(Host host) {
        delegate.onRemove(host);

        liveHosts.remove(host);
        chosenHosts.remove(host);
        updateChosenHosts();
    }
}
