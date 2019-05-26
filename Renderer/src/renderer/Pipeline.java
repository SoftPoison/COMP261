package renderer;

import renderer.Scene.Polygon;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * The Pipeline class has method stubs for all the major components of the
 * rendering pipeline, for you to fill in.
 * <p>
 * Some of these methods can get quite long, in which case you should strongly
 * consider moving them out into their own file. You'll need to update the
 * imports in the test suite if you do.
 */
public class Pipeline {
    /**
     * Returns true if the given polygon is facing away from the camera (and so
     * should be hidden), and false otherwise.
     */
    public static boolean isHidden(Polygon poly) {
        return poly.getNormal().z >= 0;
    }

    private static int clamp(float a) {
        return (int) Math.max(Math.min(a, 255), 0);
    }

    private static int divClamp(float a) {
        return clamp(a / 255);
    }

    /**
     * Computes the coloured light intensity towards the given normal
     *
     * @return an array containing the red, green, and blue intensities
     */
    private static float[] computeIntensity(Vector3D normal, List<Light> lights) {
        float ri = 0;
        float gi = 0;
        float bi = 0;
        for (Light light : lights) {
            float cosTheta = normal.dotProduct(light.direction) / (normal.mag * light.direction.mag);

            //Ignore back-lighting
            if (cosTheta < 0)
                cosTheta = 0;

            ri += light.r * cosTheta;
            gi += light.g * cosTheta;
            bi += light.b * cosTheta;
        }

        return new float[]{ ri, gi, bi };
    }

    /**
     * Computes the average colour at each vertex
     *
     * @param adjacencyMap a map containing every vertex in the scene, along with all of the polygons adjacent to each vertex
     * @param ambientLight the ambient light colour of the scene
     * @param lights       a list of all of the lights in the scene
     * @return a map containing the colour of each vertex
     */
    public static Map<Vector3D, Color> computeVertexColors(Map<Vector3D, List<Scene.Polygon>> adjacencyMap, Color ambientLight, List<Light> lights) {
        Map<Vector3D, Color> colors = new HashMap<>();

        //Iterate over the key value pairs
        for (Map.Entry<Vector3D, List<Polygon>> pair : adjacencyMap.entrySet()) {
            Vector3D vertex = pair.getKey();
            List<Polygon> polygons = pair.getValue();

            //Compute the average normal and colour from the neighbouring polygons
            Vector3D averageNormal = new Vector3D(0, 0, 0);
            int rAvg = 0, gAvg = 0, bAvg = 0;
            for (Polygon polygon : polygons) {
                averageNormal = averageNormal.plus(polygon.getNormal());
                rAvg += polygon.getReflectance().getRed();
                gAvg += polygon.getReflectance().getGreen();
                bAvg += polygon.getReflectance().getBlue();
            }
            averageNormal = averageNormal.scale(1f / polygons.size());
            rAvg /= polygons.size();
            gAvg /= polygons.size();
            bAvg /= polygons.size();

            //Get the light intensity of the average normal
            float[] intensity = computeIntensity(averageNormal, lights);

            //Calculate the colour of the vertex
            float r = ambientLight.getRed() * rAvg + intensity[0] * rAvg;
            float g = ambientLight.getGreen() * gAvg + intensity[1] * gAvg;
            float b = ambientLight.getBlue() * bAvg + intensity[2] * bAvg;

            colors.put(vertex, new Color(divClamp(r), divClamp(g), divClamp(b)));
        }

        return colors;
    }

    /**
     * Applies the given transform to all of the polygons in the scene
     */
    private static Scene transformScene(Scene scene, Transform transform) {
        List<Polygon> transformed = new ArrayList<>();

        for (Polygon polygon : scene.getPolygons()) {
            transformed.add(new Polygon(
                    transform.multiply(polygon.getVertices()[0]),
                    transform.multiply(polygon.getVertices()[1]),
                    transform.multiply(polygon.getVertices()[2]),
                    polygon.getReflectance()
            ));
        }

        return new Scene(transformed, scene.getLight());
    }

    /**
     * Translates the scene by a given vector
     */
    public static Scene translateScene(Scene scene, Vector3D translationVector) {
        Transform transform = Transform.newTranslation(translationVector);

        return transformScene(scene, transform);
    }

    /**
     * Scales the scene by a given factor
     */
    public static Scene scaleScene(Scene scene, float factor, Vector3D origin) {
        Transform transform = Transform.newTranslation(origin);
        transform = transform.compose(Transform.newScale(factor, factor, factor));
        transform = transform.compose(Transform.newTranslation(origin.scale(-1)));

        return transformScene(scene, transform);
    }

    /**
     * This method should rotate the polygons and light such that the viewer is looking down the Z-axis. The idea is
     * that it returns an entirely new Scene object, filled with new Polygons, that have been rotated.
     *
     * @param scene The original Scene.
     * @param xRot  Rotation amount about the x axis
     * @param yRot  Rotation amount about the y axis
     * @return A new Scene where all the polygons and the light source have been rotated accordingly.
     */
    public static Scene rotateScene(Scene scene, float xRot, float yRot, Vector3D origin) {
        Transform transform = Transform.newTranslation(origin);
        if (xRot != 0)
            transform = transform.compose(Transform.newXRotation(xRot));
        if (yRot != 0)
            transform = transform.compose(Transform.newYRotation(yRot));
        transform = transform.compose(Transform.newTranslation(origin.scale(-1)));

        return transformScene(scene, transform);
    }

    /**
     * Computes the edgelist of a single provided polygon, using a custom (slightly less efficient, much more accurate) method
     * This method sorts the the vertices based on y value (so v0.y <= v1.y <= v2.y). It then traverses down the longest
     * side first (updating either the lhs or rhs) and then the other two sides (updating the other side).
     * This took a fair bit of napkin maths to get working, but as a result it's a hell of a lot more accurate than the
     * method described in the lecture slides and gets rid of over 99% of whiskers
     */
    public static EdgeList computeEdgeList(Polygon poly, Map<Vector3D, Color> vertexColors) {
        //Sort the vertices of the polygon
        Vector3D[] vertices = Arrays.copyOf(poly.getVertices(), 3);
        Arrays.sort(vertices, Comparator.comparing(Vector3D::getY));

        Vector3D[][] edges = {
                { vertices[0], vertices[2] },
                { vertices[0], vertices[1] },
                { vertices[1], vertices[2] }
        };
        EdgeList edgeList = new EdgeList((int) vertices[0].y, (int) vertices[2].y);

        //Calculate if the longest side is left of the other sides. If so, we want to update the lhs of edgeList first
        Vector3D v1 = vertices[2].minus(vertices[0]);
        Vector3D v2 = vertices[1].minus(vertices[0]);
        boolean updateLeft = (v1.x * -v2.y + v1.y * v2.x) > 0;

        //Iterate through the edges
        for (int i = 0; i < edges.length; i++) {
            Vector3D[] edge = edges[i];

            Color startColor = vertexColors.get(edge[0]);
            Color targetColor = vertexColors.get(edge[1]);

            float dy = edge[1].y - edge[0].y;
            if (dy < 1) //Fixes most whiskers
                dy = 1;

            //Calculate the relevant slopes
            float xSlope = (edge[1].x - edge[0].x) / dy;
            float zSlope = (edge[1].z - edge[0].z) / dy;
            float rSlope = (targetColor.getRed() - startColor.getRed()) / dy;
            float gSlope = (targetColor.getGreen() - startColor.getGreen()) / dy;
            float bSlope = (targetColor.getBlue() - startColor.getBlue()) / dy;

            //Starting values
            float x = edge[0].x;
            float z = edge[0].z;
            float r = startColor.getRed();
            float g = startColor.getGreen();
            float b = startColor.getBlue();

            int y = (int) edge[0].y;
            int targetY = (int) edge[1].y;
            while (y <= targetY) {
                edgeList.add(y, x, z, new Color(clamp(r), clamp(g), clamp(b)), updateLeft);

                //Increment values by relevant slopes
                x += xSlope;
                z += zSlope;
                r += rSlope;
                g += gSlope;
                b += bSlope;
                y++;
            }

            if (i == 0)
                updateLeft = !updateLeft;
        }

        return edgeList;
    }

    /**
     * Fills a zbuffer with the contents of a single edge list according to the
     * lecture slides.
     * <p>
     * The idea here is to make zbuffer and zdepth arrays in your main loop, and
     * pass them into the method to be modified.
     *
     * @param zBuffer  A double array of colours representing the Color at each pixel
     *                 so far.
     * @param zDepth   A double array of floats storing the z-value of each pixel
     *                 that has been coloured in so far.
     * @param edgeList The edgelist of the polygon to add into the zbuffer.
     */
    public static void computeZBuffer(Color[][] zBuffer, float[][] zDepth, EdgeList edgeList) {
        //Go down the triangle from top to bottom
        for (int y = Math.max(edgeList.getStartY(), 0); y <= edgeList.getEndY() && y < zBuffer[0].length; y++) {
            Color startColor = edgeList.getLeftColor(y);
            Color targetColor = edgeList.getRightColor(y);

            float dx = edgeList.getRightX(y) - edgeList.getLeftX(y);

            //Calculate the relevant slopes
            float slope = (edgeList.getRightZ(y) - edgeList.getLeftZ(y)) / dx;
            float rSlope = (targetColor.getRed() - startColor.getRed()) / dx;
            float gSlope = (targetColor.getGreen() - startColor.getGreen()) / dx;
            float bSlope = (targetColor.getBlue() - startColor.getBlue()) / dx;

            int x = (int) edgeList.getLeftX(y);
            int targetX = (int) edgeList.getRightX(y);

            //Starting values
            float z = edgeList.getLeftZ(y);
            float r = startColor.getRed();
            float g = startColor.getGreen();
            float b = startColor.getBlue();

            //Try to draw a horizontal line
            while (x < targetX && x < zBuffer.length) {
                //Only draw the pixel if it's in front of the other pixel in the buffer (and it's on screen)
                if (x >= 0 && z < zDepth[x][y]) {
                    zBuffer[x][y] = new Color(clamp(r), clamp(g), clamp(b));
                    zDepth[x][y] = z;
                }

                //Increment values by relevant slopes
                z += slope;
                r += rSlope;
                g += gSlope;
                b += bSlope;
                x++;
            }
        }
    }
}

// code for comp261 assignments
