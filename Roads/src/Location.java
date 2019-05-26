import java.awt.Point;

/**
 * A Location is a point in a 2D coordinate system, with increasing x from west
 * to east and increasing y from south to north (ordinary mathematical
 * coordinates). Locations are represented with two doubles, (with an
 * unspecified length unit - could be kilometers, for example), and have a fixed
 * origin in the middle of Auckland.
 * <p>
 * Points, on the other hand, represent pixel positions on the screen. A Point
 * is described by two integers: x pixels across and y pixels down. Note the y
 * coordinate has its direction flipped from Location objects.
 * <p>
 * Methods are provided to convert between these two coordinate systems, but
 * this conversion requires an origin Location (a Location at the origin will be
 * converted to the point (0,0), which is probably the top-left of the screen),
 * and a scale specifying how many pixels per length unit. Typically the scale
 * will be ( windowSize /(maxLocation - minLocation) ).
 * <p>
 * Finally, a method is provided to convert out of the latitude-longitude
 * coordinate system used in the input files and into the Location coordinate
 * system.
 */

public class Location {

    // the center of Auckland City according to Google Maps
    private static final double CENTRE_LAT = -36.847622;
    private static final double CENTRE_LON = 174.763444;

    // how many kilometers per degree.
    private static final double SCALE_LAT = 111.0;
    private static final double DEG_TO_RAD = Math.PI / 180;

    // fields are public for easy access, but they are final so that the
    // location is immutable.
    public final double x;
    public final double y;

    public Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // -------------------------------------------
    // conversion methods. you want to use these.
    // -------------------------------------------

    /**
     * Converts the location into a point
     *
     * @param scale  window scale
     * @param offset offset from the origin (how much the map has been translated by)
     * @param zoom   zoom factor of the map
     * @param centre centre of the window
     * @return the converted location
     */
    public Point toPoint(double scale, Point offset, double zoom, Point centre) {
        int u = (int) ((offset.x + (x * scale)) * zoom) + centre.x;
        int v = (int) ((offset.y - (y * scale)) * zoom) + centre.y;

        return new Point(u, v);
    }

    /**
     * Converts the given point into a location
     *
     * @param point  point to convert
     * @param centre centre of the window
     * @param zoom   zoom factor of the map
     * @param offset offset from the origin (how much the map has been translated by)
     * @param scale  window scale
     * @return the converted point
     */
    public static Location fromPoint(Point point, Point centre, double zoom, Point offset, double scale) {
        double u = (((point.x - centre.x) / zoom) - offset.x) / scale;
        double v = (((point.y - centre.y) / zoom) - offset.y) / -scale;

        return new Location(u, v);
    }

    /**
     * Create a new Location object from the given latitude and longitude, which
     * is the format used in the data files.
     */
    public static Location fromLatLon(double lat, double lon) {
        double y = (lat - CENTRE_LAT) * SCALE_LAT;
        double x = (lon - CENTRE_LON) * (SCALE_LAT * Math.cos((lat - CENTRE_LAT) * DEG_TO_RAD));
        return new Location(x, y);
    }

    // ------------------------------------------
    // some utility methods for Location objects
    // ------------------------------------------

    /**
     * Returns a new Location object that is this Location object moved by the
     * given dx and dy, ie. this returns a Location representing (x + dx, y +
     * dy).
     */
    public Location moveBy(double dx, double dy) {
        return new Location(x + dx, y + dy);
    }

    /**
     * Return distance between this location and another
     */
    public double distance(Location other) {
        return Math.hypot(this.x - other.x, this.y - other.y);
    }

    /**
     * Return true if this location is within dist of other Uses manhattan
     * distance for greater speed. Equivalent to whether other is within a
     * diamond shape around this location.
     */
    public boolean isClose(Location other, double dist) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y) <= dist;
    }

    public String toString() {
        return String.format("(%.3f, %.3f)", x, y);
    }

    public boolean equals(Location other) {
        return isClose(other, 0.01);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Location)
            return equals((Location) obj);
        else
            return false;
    }
}

// code for COMP261 assignments
