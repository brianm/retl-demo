package org.skife.retl;

import com.github.davidmoten.rtree.geometry.Point;

public class Geo {

    private static final int EARTH_RADIUS_KM = 6371;

    public static double haversineDistance(Point start, Point end) {

        double distanceLat = Math.toRadians(end.y() - start.y());
        double distanceLon = Math.toRadians(end.x() - start.x());

        double startLat = Math.toRadians(start.y());
        double endLat = Math.toRadians(end.y());

        double a = Math.pow(Math.sin(distanceLat / 2), 2) +
                   Math.cos(startLat) *
                   Math.cos(endLat) *
                   Math.pow(Math.sin(distanceLon / 2), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

}
