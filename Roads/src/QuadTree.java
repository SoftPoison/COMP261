import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuadTree {
    public static final QuadTree EMPTY = new QuadTree(new BoundingBox(new Location(0, 0), 0)) {
        public boolean insert(Node node) {
            return false;
        }

        public List<Node> queryRange(BoundingBox range) {
            return Collections.emptyList();
        }
    };

    private static final int NODES_CAPACITY = 4;

    private BoundingBox boundary;
    private List<Node> nodes = new ArrayList<>(NODES_CAPACITY);
    private QuadTree[] children = new QuadTree[4];

    public QuadTree(BoundingBox boundary) {
        this.boundary = boundary;
    }

    /**
     * Attempts to insert a node into the quad tree
     *
     * @param node the node to insert
     * @return if successful
     */
    public boolean insert(Node node) {
        //Check to see if the boundary could contain the node
        if (!boundary.containsPoint(node.getLocation()))
            return false;

        //Add the node if there is enough room
        if (nodes.size() < NODES_CAPACITY && children[0] == null) {
            nodes.add(node);
            return true;
        }

        //Subdivide if there are no children
        if (children[0] == null)
            subDivide();

        //Add the node to one of the children
        for (QuadTree child : children)
            if (child.insert(node))
                return true;

        //Should never get this far
        return false;
    }

    /**
     * Splits the quad tree into 4 equally sized children
     */
    private void subDivide() {
        double halfSize = boundary.getSize() / 2;
        Location location = boundary.getLocation();

        children[0] = new QuadTree(new BoundingBox(new Location(location.x, location.y), halfSize));
        children[1] = new QuadTree(new BoundingBox(new Location(location.x + halfSize, location.y), halfSize));
        children[2] = new QuadTree(new BoundingBox(new Location(location.x, location.y + halfSize), halfSize));
        children[3] = new QuadTree(new BoundingBox(new Location(location.x + halfSize, location.y + halfSize), halfSize));
    }

    /**
     * Finds all of the nodes within the given range
     *
     * @param range range to search
     * @return all of the matching nodes
     */
    public List<Node> queryRange(BoundingBox range) {
        List<Node> result = new ArrayList<>();

        //Stop looking down this path if the range isn't within the boundary
        if (!boundary.intersects(range))
            return result;

        //Add any nodes that are within the range to the result list
        for (Node node : nodes)
            if (range.containsPoint(node.getLocation()))
                result.add(node);

        if (children[0] == null)
            return result;

        for (QuadTree child : children)
            result.addAll(child.queryRange(range));

        return result;
    }
}
