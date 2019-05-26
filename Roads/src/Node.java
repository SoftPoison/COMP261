import java.awt.*;

public class Node {
    private int id;
    private Location location;
    private boolean trafficLight = false;

    public Node(String line) {
        String[] parts = line.split("\t");
        id = Integer.parseInt(parts[0]);
        location = Location.fromLatLon(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }

    public int getID() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public void setHasTrafficLight(boolean hasTrafficLight) {
        trafficLight = hasTrafficLight;
    }

    public boolean hasTrafficLight() {
        return trafficLight;
    }

    /**
     * Draws the node
     *
     * @param g      graphics
     * @param offset offset of map
     * @param centre centre of the screen
     * @param zoom   factor to zoom by
     * @param size   size of circle
     */
    public void draw(Graphics g, Point offset, Point centre, double zoom, double size) {
        if (zoom < MapViewer.CLOSE_ZOOM_CUTOFF) //Don't draw if it's too small
            return;

        int hSize = (int) (size / 2);
        Point p = location.toPoint(MapViewer.WINDOW_SCALE, offset, zoom, centre);

        if (!g.getClipBounds().contains(p)) //Only draw if on screen
            return;

        p.translate(-hSize, -hSize);

        g.fillOval(p.x, p.y, (int) size, (int) size);
    }
}
