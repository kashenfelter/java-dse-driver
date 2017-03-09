/*
 *      Copyright (C) 2012-2017 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dse.graph.internal;

import com.datastax.driver.dse.geometry.*;
import com.datastax.dse.graph.api.predicates.Geo;
import com.google.common.base.Preconditions;

/**
 * List of predicates for geolocation usage with DseGraph and Search indexes.
 * Should not be accessed directly but through the {@link Geo} static methods.
 */
public enum GeoPredicate implements DsePredicate {

    /**
     * Matches values within the distance specified by the condition over a Haversine geometry.
     */
    inside {
        @Override
        public boolean test(Object value, Object condition) {
            preEvaluate(condition);
            if (value == null) return false;
            Preconditions.checkArgument(value instanceof Geometry);
            Distance distance = (Distance) condition;
            if (value instanceof Point) {
                return haversineDistanceInDegrees(distance.getCenter(), (Point) value) <= distance.getRadius();
            } else if (value instanceof Polygon) {
                for (Point point : ((Polygon) value).getExteriorRing()) {
                    if (haversineDistanceInDegrees(distance.getCenter(), point) > distance.getRadius()) {
                        return false;
                    }
                }
            } else if (value instanceof LineString) {
                for (Point point : ((LineString) value).getPoints()) {
                    if (haversineDistanceInDegrees(distance.getCenter(), point) > distance.getRadius()) {
                        return false;
                    }
                }
            } else {
                throw new UnsupportedOperationException(String.format("Value type '%s' unsupported", value.getClass().getName()));
            }

            return true;
        }

        @Override
        public String toString() {
            return "inside";
        }
    },

    /**
     * Matches values contained in the geometric entity specified by the condition on a 2D Euclidean plane.
     */
    insideCartesian {
        @Override
        public boolean test(Object value, Object condition) {
            preEvaluate(condition);
            if (value == null) return false;
            Preconditions.checkArgument(value instanceof Geometry);
            return ((Geometry) condition).contains((Geometry) value);
        }

        @Override
        public String toString() {
            return "insideCartesian";
        }
    };

    @Override
    public boolean isValidCondition(Object condition) {
        return condition != null;
    }

    static double haversineDistanceInDegrees(Point p1, Point p2) {
        double dLat = Math.toRadians(p2.Y() - p1.Y());
        double dLon = Math.toRadians(p2.X() - p1.X());
        double lat1 = Math.toRadians(p1.Y());
        double lat2 = Math.toRadians(p2.Y());

        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return Math.toDegrees(c);

    }
}
