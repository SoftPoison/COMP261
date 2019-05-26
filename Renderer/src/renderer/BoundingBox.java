package renderer;

public class BoundingBox {
    public final float width;
    public final float height;
    public final float depth;
    public final Vector3D centre;

    public BoundingBox(Scene scene) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (Scene.Polygon polygon : scene.getPolygons()) {
            for (Vector3D vertex : polygon.getVertices()) {
                if (vertex.x < minX) minX = vertex.x;
                if (vertex.x > maxX) maxX = vertex.x;
                if (vertex.y < minY) minY = vertex.y;
                if (vertex.y > maxY) maxY = vertex.y;
                if (vertex.z < minZ) minZ = vertex.z;
                if (vertex.z > maxZ) maxZ = vertex.z;
            }
        }

        width = maxX - minX;
        height = maxY - minY;
        depth = maxZ - minZ;

        centre = new Vector3D(minX + width / 2, minY + height / 2, minZ + depth / 2);
    }
}
