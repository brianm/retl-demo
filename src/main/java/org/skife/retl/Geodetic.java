package org.skife.retl;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.google.common.base.Preconditions;

public class Geodetic {

    public static final int EARTH_RADIUS_KM = 6371;
    public static final double EARTH_CIRCUMFERENCE_KM = 40075;
    private final int radius;

    public Geodetic(int radius) {
        this.radius = radius;
    }

    public static final Geodetic Earth = new Geodetic(EARTH_RADIUS_KM);

    public double distance(Point start, Point end) {

        double startLat = Math.toRadians(start.y());
        double endLat = Math.toRadians(end.y());

        double distanceLat = Math.toRadians(end.y() - start.y());
        double distanceLon = Math.toRadians(end.x() - start.x());

        double a = Math.pow(Math.sin(distanceLat / 2), 2) +
                   Math.cos(startLat) *
                   Math.cos(endLat) *
                   Math.pow(Math.sin(distanceLon / 2), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return this.radius * c;
    }

    public static Point latLong(double latitude, double longitude) {
        Preconditions.checkArgument(latitude >= -90 && latitude <= 90,
                                    "latitude must be between -90 and 90");
        Preconditions.checkArgument(longitude >= -180 && longitude <= 180,
                                    "longitude must be between -180 and 180");
//        return new GeodeticPoint(Geometries.pointGeographic(longitude, latitude));
        return Geometries.pointGeographic(longitude, latitude);
    }

    public double circumference() {
        return 2 * Math.PI * radius;
    }

    private static class GeodeticPoint implements Point {

        private final Geodetic geodetic;
        private final Point delegate;

        private GeodeticPoint(Geodetic geodetic, Point delegate) {
            this.geodetic = geodetic;
            this.delegate = delegate;
        }

        @Override
        public double x() {
            return delegate.x();
        }

        @Override
        public double y() {
            return delegate.y();
        }

        @Override
        public double x1() {
            return delegate.x1();
        }

        @Override
        public double y1() {
            return delegate.y1();
        }

        @Override
        public double x2() {
            return delegate.x2();
        }

        @Override
        public double y2() {
            return delegate.y2();
        }

        @Override
        public double area() {
            return delegate.area();
        }

        @Override
        public double intersectionArea(Rectangle r) {
            return delegate.intersectionArea(r);
        }

        @Override
        public double perimeter() {
            return delegate.perimeter();
        }

        @Override
        public Rectangle add(Rectangle r) {
            return delegate.add(r);
        }

        @Override
        public boolean contains(double x, double y) {
            return delegate.contains(x, y);
        }

        @Override
        public double distance(Rectangle r) {
            Preconditions.checkState(r instanceof Point, "only works with points so far, sorry");
            Point p = (Point) r;
            return this.geodetic.distance(this, p);
        }

        @Override
        public Rectangle mbr() {
            return delegate.mbr();
        }

        @Override
        public boolean intersects(Rectangle r) {
            return delegate.intersects(r);
        }

        @Override
        public boolean isDoublePrecision() {
            return delegate.isDoublePrecision();
        }

        @Override
        public Geometry geometry() {
            return this;
        }
    }

}
