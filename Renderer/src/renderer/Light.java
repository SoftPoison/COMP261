package renderer;

public class Light {
    public final Vector3D direction;
    public final int r;
    public final int g;
    public final int b;

    public Light(Vector3D direction, int r, int g, int b) {
        this.direction = direction;
        this.r = r;
        this.g = g;
        this.b = b;
    }
}
