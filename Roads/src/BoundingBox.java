public class BoundingBox {
    private Location location;
    private double size;

    public BoundingBox(Location location, double size) {
        this.location = location;
        this.size = size;
    }

    public Location getLocation() {
        return location;
    }

    public double getSize() {
        return size;
    }

    public boolean containsPoint(Location p) {
        return p.x >= location.x && p.x < location.x + size && p.y >= location.y && p.y < location.y + size;
    }

    /**
     * Tests to see if the bounding box intersects another. This code is a modified version of the awt Rectangle
     *
     * @param other the other bounding box
     * @return true if they intersect
     * @see java.awt.Rectangle::intersects
     */
    public boolean intersects(BoundingBox other) {
        //if top left inside || top right || bottom left || bottom right

        double tw = this.size;
        double th = this.size;
        double ow = other.size;
        double oh = other.size;

        double tx = this.location.x;
        double ty = this.location.y;
        double ox = other.location.x;
        double oy = other.location.y;

        ow += ox;
        oh += oy;
        tw += tx;
        th += ty;

        return (ow < ox || ow > tx) && (oh < oy || oh > ty) && (tw < tx || tw > ox) && (th < ty || th > oy);
    }
}
