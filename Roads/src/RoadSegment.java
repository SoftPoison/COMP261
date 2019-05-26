import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class RoadSegment {
    private int id;
    private double length;
    private int nodeID1;
    private int nodeID2;
    private List<Location> points = new ArrayList<>();

    public RoadSegment(String line) {
        String[] parts = line.split("\t");
        id = Integer.parseInt(parts[0]);
        length = Double.parseDouble(parts[1]);
        nodeID1 = Integer.parseInt(parts[2]);
        nodeID2 = Integer.parseInt(parts[3]);

        for (int i = 4; i < parts.length; i += 2) {
            points.add(Location.fromLatLon(Double.parseDouble(parts[i]), Double.parseDouble(parts[i + 1])));
        }
    }

    public int getID() {
        return id;
    }

    public double getLength() {
        return length;
    }

    public int getNodeID1() {
        return nodeID1;
    }

    public int getNodeID2() {
        return nodeID2;
    }

    public int getOtherNode(int node) {
        return node == nodeID1 ? nodeID2 : nodeID1;
    }

    /**
     * Draws the road segment to the given graphics object
     *
     * @param g         graphics object to draw the segment to
     * @param offset    window offset from the origin
     * @param centre    centre of the window
     * @param zoom      zoom factor of the map
     * @param roadClass classification of the road (5 = highlighted)
     */
    public void draw(Graphics g, Point offset, Point centre, double zoom, int roadClass) {
        //More important roads are displayed larger
        double size = roadClass == 5 ? 4 : (roadClass + 1) * zoom / 3;

        if (roadClass == 0 && zoom < MapViewer.CLOSE_ZOOM_CUTOFF || roadClass == 1 && zoom < MapViewer.MEDIUM_ZOOM_CUTOFF)
            return;

        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i).toPoint(MapViewer.WINDOW_SCALE, offset, zoom, centre);
            Point p2 = points.get(i + 1).toPoint(MapViewer.WINDOW_SCALE, offset, zoom, centre);

            //Only render if the line intersects the viewing area
            if (!(new Line2D.Double(p1.x, p1.y, p2.x, p2.y).intersects(g.getClipBounds())))
                continue;

            //Cast the graphics object so that the line width can be manipulated
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke((float) size));
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }
}
