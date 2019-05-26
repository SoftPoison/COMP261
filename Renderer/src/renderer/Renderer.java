package renderer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.List;

public class Renderer extends GUI {
    private static final float X_ROT_AMOUNT = 0.1f;
    private static final float Y_ROT_AMOUNT = 0.1f;
    private static final float TRANSLATE_AMOUNT = 10f;
    private static final float ZOOM_AMOUNT = 1.1f;
    private static final Vector3D ORIGIN = new Vector3D(CANVAS_WIDTH / 2, CANVAS_HEIGHT /2, 0);

    private Scene scene = null;

    @Override
    protected void onLoad(File file) {
        scene = null;

        List<Scene.Polygon> polygons = new ArrayList<>();
        Vector3D light;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int numLines = Integer.parseInt(reader.readLine());

            for (int i = 0; i < numLines; i++) {
                String[] parts = reader.readLine().split(",");
                if (parts.length == 3)
                    break;

                Color color = new Color(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                Vector3D p1 = new Vector3D(Float.parseFloat(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
                Vector3D p2 = new Vector3D(Float.parseFloat(parts[6]), Float.parseFloat(parts[7]), Float.parseFloat(parts[8]));
                Vector3D p3 = new Vector3D(Float.parseFloat(parts[9]), Float.parseFloat(parts[10]), Float.parseFloat(parts[11]));
                Scene.Polygon polygon = new Scene.Polygon(p1, p2, p3, color);

                polygons.add(polygon);
            }

            String[] parts = reader.readLine().split(",");
            light = new Vector3D(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
        }
        catch (Exception e) {
            System.err.println("Error: could not read file");
            return;
        }

        scene = new Scene(polygons, light);
        BoundingBox bb = new BoundingBox(scene);

        scene = Pipeline.translateScene(scene, bb.centre.scale(-1));
        scene = Pipeline.scaleScene(scene, CANVAS_WIDTH / bb.width / 2, new Vector3D(0, 0, 0));
        scene = Pipeline.translateScene(scene, ORIGIN);
    }

    /**
     * W = zoom in
     * S = zoom out
     * A = move scene left
     * D = move scene right
     * space = move scene up
     * C/shift = move scene down
     * arrow keys rotate scene in related direction
     */
    @Override
    protected void onKeyPress(KeyEvent ev) {
        switch (ev.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                scene = Pipeline.rotateScene(scene, 0, Y_ROT_AMOUNT, ORIGIN);
                break;
            case KeyEvent.VK_RIGHT:
                scene = Pipeline.rotateScene(scene, 0, -Y_ROT_AMOUNT, ORIGIN);
                break;
            case KeyEvent.VK_UP:
                scene = Pipeline.rotateScene(scene, X_ROT_AMOUNT, 0, ORIGIN);
                break;
            case KeyEvent.VK_DOWN:
                scene = Pipeline.rotateScene(scene, -X_ROT_AMOUNT, 0, ORIGIN);
                break;
            case KeyEvent.VK_SPACE:
                scene = Pipeline.translateScene(scene, new Vector3D(0, -TRANSLATE_AMOUNT, 0));
                break;
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_C:
                scene = Pipeline.translateScene(scene, new Vector3D(0, TRANSLATE_AMOUNT, 0));
                break;
            case KeyEvent.VK_W:
                scene = Pipeline.scaleScene(scene, ZOOM_AMOUNT, ORIGIN);
                break;
            case KeyEvent.VK_S:
                scene = Pipeline.scaleScene(scene, 1 / ZOOM_AMOUNT, ORIGIN);
                break;
            case KeyEvent.VK_A:
                scene = Pipeline.translateScene(scene, new Vector3D(-TRANSLATE_AMOUNT, 0, 0));
                break;
            case KeyEvent.VK_D:
                scene = Pipeline.translateScene(scene, new Vector3D(TRANSLATE_AMOUNT, 0, 0));
                break;
        }
    }

    @Override
    protected BufferedImage render() {
        if (scene == null)
            return null;

        Color[][] zBuffer = new Color[CANVAS_WIDTH][CANVAS_HEIGHT];
        float[][] zDepth = new float[CANVAS_WIDTH][CANVAS_HEIGHT];

        Color ambientLight = new Color(getAmbientLight()[0], getAmbientLight()[1], getAmbientLight()[2]);
        Color background = new Color(ambientLight.getRed() / 2, ambientLight.getGreen() / 2, ambientLight.getBlue() / 2);

        for (int x = 0; x < CANVAS_WIDTH; x++) {
            for (int y = 0; y < CANVAS_HEIGHT; y++) {
                zBuffer[x][y] = background;
                zDepth[x][y] = Float.POSITIVE_INFINITY;
            }
        }

        Map<Vector3D, List<Scene.Polygon>> adjacencyMap = new HashMap<>();

        for (Scene.Polygon polygon : scene.getPolygons()) {
            adjacencyMap.computeIfAbsent(polygon.getVertices()[0], v -> new ArrayList<>()).add(polygon);
            adjacencyMap.computeIfAbsent(polygon.getVertices()[1], v -> new ArrayList<>()).add(polygon);
            adjacencyMap.computeIfAbsent(polygon.getVertices()[2], v -> new ArrayList<>()).add(polygon);
        }

        //Get all of the lights
        List<Light> lights = new ArrayList<>(getLights());
        lights.add(new Light(scene.getLight(), 255, 255, 255));

        //Calculate the colour of each vertex
        Map<Vector3D, Color> vertexColors = Pipeline.computeVertexColors(adjacencyMap, ambientLight, lights);

        //Loop through the polygons, attempting to draw them
        for (Scene.Polygon polygon : scene.getPolygons()) {
            if (Pipeline.isHidden(polygon)) //Only draw the polygon if it's facing the right direction
                continue;

            EdgeList edgeList = Pipeline.computeEdgeList(polygon, vertexColors);
            Pipeline.computeZBuffer(zBuffer, zDepth, edgeList);
        }


        //Create some axes in the centre (for debug purposes)
        if (showAxes) {
            int axesOffset = 10; //Arbitrary constant
            int centreX = CANVAS_WIDTH / 2 + 1;
            for (int y = axesOffset; y < CANVAS_HEIGHT - axesOffset; y++) {
                if (zDepth[centreX][y] > 0)
                    zBuffer[centreX][y] = Color.WHITE;
            }

            int centreY = CANVAS_HEIGHT / 2 + 1;
            for (int x = axesOffset; x < CANVAS_WIDTH - axesOffset; x++) {
                if (zDepth[x][centreY] > 0)
                    zBuffer[x][centreY] = Color.WHITE;
            }
        }

        return convertBitmapToImage(zBuffer);
    }

    /**
     * Converts a 2D array of Colors to a BufferedImage. Assumes that bitmap is
     * indexed by column then row and has imageHeight rows and imageWidth
     * columns. Note that image.setRGB requires x (col) and y (row) are given in
     * that order.
     */
    private BufferedImage convertBitmapToImage(Color[][] bitmap) {
        BufferedImage image = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < CANVAS_WIDTH; x++) {
            for (int y = 0; y < CANVAS_HEIGHT; y++) {
                image.setRGB(x, y, bitmap[x][y].getRGB());
            }
        }
        return image;
    }

    public static void main(String[] args) {
        new Renderer();
    }
}

// code for comp261 assignments
