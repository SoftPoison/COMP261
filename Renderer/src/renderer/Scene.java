package renderer;

import java.awt.Color;
import java.util.List;

/**
 * The Scene class is where we store data about a 3D model and light source
 * inside our renderer. It also contains a static inner class that represents one
 * single polygon.
 * <p>
 * Method stubs have been provided, but you'll need to fill them in.
 * <p>
 * If you were to implement more fancy rendering, e.g. Phong shading, you'd want
 * to store more information in this class.
 */
public class Scene {
    private List<Polygon> polygons;
    private Vector3D lightPos;

    public Scene(List<Polygon> polygons, Vector3D lightPos) {
        this.polygons = polygons;
        this.lightPos = lightPos;
    }

    public Vector3D getLight() {
        return lightPos;
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }

    /**
     * Polygon stores data about a single polygon in a scene, keeping track of
     * (at least!) its three vertices and its reflectance.
     * <p>
     * This class has been done for you.
     */
    public static class Polygon {
        Vector3D[] vertices;
        Color reflectance;
        Vector3D normal;

        /**
         * @param points An array of floats with 9 elements, corresponding to the
         *               (x,y,z) coordinates of the three vertices that make up
         *               this polygon. If the three vertices are A, B, C then the
         *               array should be [A_x, A_y, A_z, B_x, B_y, B_z, C_x, C_y,
         *               C_z].
         * @param color  An array of three ints corresponding to the RGB values of
         *               the polygon, i.e. [r, g, b] where all values are between 0
         *               and 255.
         */
        public Polygon(float[] points, int[] color) {
            this.vertices = new Vector3D[3];

            float x, y, z;
            for (int i = 0; i < 3; i++) {
                x = points[i * 3];
                y = points[i * 3 + 1];
                z = points[i * 3 + 2];
                this.vertices[i] = new Vector3D(x, y, z);
            }

            int r = color[0];
            int g = color[1];
            int b = color[2];
            this.reflectance = new Color(r, g, b);

            this.parseNormal();
        }

        /**
         * An alternative constructor that directly takes three Vector3D objects
         * and a Color object.
         */
        public Polygon(Vector3D a, Vector3D b, Vector3D c, Color color) {
            this.vertices = new Vector3D[]{ a, b, c };
            this.reflectance = color;

            this.parseNormal();
        }

        /**
         * Computes the normal vector of the polygon
         */
        private void parseNormal() {
            normal = vertices[1].minus(vertices[0]).crossProduct(vertices[2].minus(vertices[1]));
        }

        public Vector3D[] getVertices() {
            return vertices;
        }

        public Color getReflectance() {
            return reflectance;
        }

        public Vector3D getNormal() {
            return normal;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder("polygon:");

            for (Vector3D p : vertices)
                str.append("\n  ").append(p.toString());

            str.append("\n  ").append(reflectance.toString());

            return str.toString();
        }
    }
}

// code for COMP261 assignments
