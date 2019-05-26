import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Polygon implements Comparable<Polygon> {
    private static final Map<Integer, Color> COLOR_MAP = new HashMap<>();
    private static final Map<Integer, Integer> SORTING_MAP = new HashMap<>();
    private static final Color COLOR_GREENERY = new Color(0, 182, 0);
    private static final Color COLOR_WATER = new Color(47, 43, 240);
    private static final Color COLOR_LAKE = new Color(76, 103, 240);

    static {
        //Mappings from polygon type to display colour and sorting level
        COLOR_MAP.put(0x00, Color.RED); SORTING_MAP.put(0x00, 0); //error
        COLOR_MAP.put(0x01, Color.LIGHT_GRAY); SORTING_MAP.put(0x01, 10); //city
        COLOR_MAP.put(0x02, Color.LIGHT_GRAY); SORTING_MAP.put(0x02, 10); //city
        COLOR_MAP.put(0x03, Color.LIGHT_GRAY); SORTING_MAP.put(0x03, 10); //city
        COLOR_MAP.put(0x04, Color.GRAY); SORTING_MAP.put(0x04, 1); //military
        COLOR_MAP.put(0x05, Color.GRAY); SORTING_MAP.put(0x05, 1); //car park
        COLOR_MAP.put(0x06, Color.GRAY); SORTING_MAP.put(0x06, 1); //parking garage
        COLOR_MAP.put(0x07, Color.GRAY); SORTING_MAP.put(0x07, 1); //airport
        COLOR_MAP.put(0x08, Color.GRAY); SORTING_MAP.put(0x08, 1); //shopping
        COLOR_MAP.put(0x09, Color.GRAY); SORTING_MAP.put(0x09, 2); //marina
        COLOR_MAP.put(0x0a, Color.GRAY); SORTING_MAP.put(0x0a, 1); //university
        COLOR_MAP.put(0x0b, Color.GRAY); SORTING_MAP.put(0x0b, 1); //hospital
        COLOR_MAP.put(0x0c, Color.GRAY); SORTING_MAP.put(0x0c, 2); //industrial
        COLOR_MAP.put(0x0d, Color.GRAY); SORTING_MAP.put(0x0d, 2); //reservation
        COLOR_MAP.put(0x0e, Color.GRAY); SORTING_MAP.put(0x0e, 2); //airport runway
        COLOR_MAP.put(0x13, Color.LIGHT_GRAY); SORTING_MAP.put(0x13, 3); //man made area
        COLOR_MAP.put(0x14, COLOR_GREENERY); SORTING_MAP.put(0x14, 4); //national park
        COLOR_MAP.put(0x15, COLOR_GREENERY); SORTING_MAP.put(0x15, 4); //national park
        COLOR_MAP.put(0x16, COLOR_GREENERY); SORTING_MAP.put(0x16, 4); //national park
        COLOR_MAP.put(0x17, COLOR_GREENERY); SORTING_MAP.put(0x17, 4); //city park
        COLOR_MAP.put(0x18, COLOR_GREENERY); SORTING_MAP.put(0x18, 4); //golf course
        COLOR_MAP.put(0x19, COLOR_GREENERY); SORTING_MAP.put(0x19, 4); //sports/activity grounds
        COLOR_MAP.put(0x1a, COLOR_GREENERY); SORTING_MAP.put(0x1a, 3); //cemetery
        COLOR_MAP.put(0x1e, COLOR_GREENERY); SORTING_MAP.put(0x1e, 4); //state park
        COLOR_MAP.put(0x1f, COLOR_GREENERY); SORTING_MAP.put(0x1f, 4); //state park
        COLOR_MAP.put(0x28, COLOR_WATER); SORTING_MAP.put(0x28, 5); //ocean
        COLOR_MAP.put(0x32, COLOR_WATER); SORTING_MAP.put(0x32, 5); //sea
        COLOR_MAP.put(0x3b, COLOR_WATER); SORTING_MAP.put(0x3b, 5); //blue-unknown
        COLOR_MAP.put(0x3c, COLOR_LAKE); SORTING_MAP.put(0x3c, 3); //lake
        COLOR_MAP.put(0x3d, COLOR_LAKE); SORTING_MAP.put(0x3d, 3); //lake
        COLOR_MAP.put(0x3e, COLOR_LAKE); SORTING_MAP.put(0x3e, 3); //lake
        COLOR_MAP.put(0x3f, COLOR_LAKE); SORTING_MAP.put(0x3f, 3); //lake
        COLOR_MAP.put(0x40, COLOR_LAKE); SORTING_MAP.put(0x40, 3); //lake
        COLOR_MAP.put(0x41, COLOR_LAKE); SORTING_MAP.put(0x41, 3); //lake
        COLOR_MAP.put(0x42, COLOR_LAKE); SORTING_MAP.put(0x42, 3); //lake
        COLOR_MAP.put(0x43, COLOR_LAKE); SORTING_MAP.put(0x43, 3); //lake
        COLOR_MAP.put(0x44, COLOR_LAKE); SORTING_MAP.put(0x44, 3); //lake
        COLOR_MAP.put(0x45, COLOR_LAKE); SORTING_MAP.put(0x45, 3); //blue-unknown
        COLOR_MAP.put(0x46, COLOR_WATER); SORTING_MAP.put(0x46, 3); //river
        COLOR_MAP.put(0x47, COLOR_WATER); SORTING_MAP.put(0x47, 3); //river
        COLOR_MAP.put(0x48, COLOR_WATER); SORTING_MAP.put(0x48, 3); //river
        COLOR_MAP.put(0x49, COLOR_WATER); SORTING_MAP.put(0x49, 3); //river
        COLOR_MAP.put(0x4b, MapViewer.BACKGROUND_COLOR); SORTING_MAP.put(0x4b, 20); //background
        COLOR_MAP.put(0x4c, COLOR_LAKE); SORTING_MAP.put(0x4c, 3); //intermittent river/lake
        COLOR_MAP.put(0x4d, Color.CYAN); SORTING_MAP.put(0x4d, 10); //glacier
        COLOR_MAP.put(0x4e, Color.GREEN); SORTING_MAP.put(0x4e, 10); //orchard/plantation
        COLOR_MAP.put(0x4f, COLOR_GREENERY); SORTING_MAP.put(0x4f, 10); //scrub
        COLOR_MAP.put(0x50, COLOR_GREENERY); SORTING_MAP.put(0x50, 10); //woods
        COLOR_MAP.put(0x51, COLOR_GREENERY); SORTING_MAP.put(0x51, 10); //wetland
        COLOR_MAP.put(0x52, COLOR_GREENERY); SORTING_MAP.put(0x52, 10); //tundra
        COLOR_MAP.put(0x53, COLOR_GREENERY); SORTING_MAP.put(0x53, 10); //flats
    }

    private int type;
    private String label;
    private int endLevel; //3 = always visible, 2 = minZoom(1), 1 = minZoom(5)
    private List<Location> points = new ArrayList<>();
    private List<List<Location>> holes = new ArrayList<>();
    private Location approximateCentre;

    public Polygon(String type, String label, String endLevel, List<String> data) {
        this.type = Integer.parseInt(type, 16);
        this.label = label;
        this.endLevel = endLevel.equals("") ? 1 : Integer.parseInt(endLevel);

        String line = data.get(0);
        String[] parts = line.split(",");
        double lat = 0, lon = 0;
        for (int j = 0; j < parts.length; j++) {
            if (j % 2 == 0) {
                lat = Double.parseDouble(parts[j].substring(1));
            }
            else {
                lon = Double.parseDouble(parts[j].substring(0, parts[j].length() - 1));

                points.add(Location.fromLatLon(lat, lon));
            }
        }

        for (int i = 1; i < data.size(); i++) {
            List<Location> hole = new ArrayList<>();
            line = data.get(i);
            parts = line.split(",");
            lat = 0;
            lon = 0;
            for (int j = 0; j < parts.length; j++) {
                if (j % 2 == 0) {
                    lat = Double.parseDouble(parts[j].substring(1));
                }
                else {
                    lon = Double.parseDouble(parts[j].substring(0, parts[j].length() - 1));

                    hole.add(Location.fromLatLon(lat, lon));
                }
            }
            holes.add(hole);

        }

        //Find the average of the points to find the approximate middle
        double sumX = 0;
        double sumY = 0;
        for (Location point : points) {
            sumX += point.x;
            sumY += point.y;
        }

        approximateCentre = new Location(sumX / points.size(), sumY / points.size());
    }

    public int getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public int getEndLevel() {
        return endLevel;
    }

    /**
     * Internal method for drawing the polygon
     */
    private void draw(List<Location> points, int type, Graphics g, Point offset, Point centre, double zoom) {
        g.setColor(COLOR_MAP.getOrDefault(type, COLOR_MAP.get(0)));

        int[] xPoints = new int[points.size()];
        int[] yPoints = new int[points.size()];

        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i).toPoint(MapViewer.WINDOW_SCALE, offset, zoom, centre);

            xPoints[i] = point.x;
            yPoints[i] = point.y;
        }

        //Create an awt polygon from the points in order to see if the polygon should be drawn
        java.awt.Polygon p = new java.awt.Polygon(xPoints, yPoints, points.size());
        if (!p.intersects(g.getClipBounds()))
            return;

        g.fillPolygon(p);
    }

    /**
     * Draws the polygon to the given graphics object
     *
     * @param g      graphics object to draw the polygon to
     * @param offset window offset from the origin
     * @param centre approximateCentre of the window
     * @param zoom   zoom factor of the map
     */
    public void draw(Graphics g, Point offset, Point centre, double zoom) {
        //Don't draw if the zoom value is less than the given cutoff
        if (endLevel == 1 && zoom < MapViewer.CLOSE_ZOOM_CUTOFF || endLevel == 2 && zoom < MapViewer.MEDIUM_ZOOM_CUTOFF)
            return;

        draw(points, type, g, offset, centre, zoom);

        //Draw all of the holes in the polygon with the background colour
        for (List<Location> hole : holes)
            draw(hole, 0x4b, g, offset, centre, zoom);
    }

    public void drawLabel(Graphics g, Point offset, Point centre, double zoom) {
        if (label == null || label.equals("") || zoom < MapViewer.LABEL_ZOOM_CUTOFF)
            return;

        double labelSize = endLevel == 3 ? MapViewer.LABEL_SIZE : MapViewer.LABEL_SIZE * zoom / 6;

        //Don't draw if the render size is too small
        if (labelSize < 8)
            return;

        g.setFont(MapViewer.LABEL_FONT.deriveFont((float) labelSize));
        Point labelLocation = approximateCentre.toPoint(MapViewer.WINDOW_SCALE, offset, zoom, centre);
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(label, g);

        if (!g.getClipBounds().contains(labelLocation))
            return;

        labelLocation.translate((int) (-bounds.getWidth() / 2), (int) (-bounds.getHeight() / 2));
        g.drawString(label, labelLocation.x, labelLocation.y);
    }

    @Override
    public int compareTo(Polygon p) {
        return Integer.compare(SORTING_MAP.get(p.type), SORTING_MAP.get(type));
    }
}
