import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class MapViewer extends GUI {
    public static final double ZOOM_RATE = 1.5;
    public static final double TRANSLATE_RATE = 50;
    public static final double WINDOW_SCALE = 100;
    public static final double LABEL_SIZE = 16;

    public static final double CLOSE_ZOOM_CUTOFF = 1.4;
    public static final double MEDIUM_ZOOM_CUTOFF = 0.2;
    public static final double LABEL_ZOOM_CUTOFF = 0.3;

    public static final Font LABEL_FONT = new Font("TimesRoman", Font.BOLD, (int) LABEL_SIZE);
    public static final Color BACKGROUND_COLOR = new Color(226, 226, 226);

    private HashMap<Integer, Node> nodeLookupTable = new HashMap<>();
    private HashMap<Integer, List<RoadSegment>> nodeAdjacencyTable = new HashMap<>();
    private HashMap<Integer, List<Node>> neighbouringNodesTable = new HashMap<>();
    private HashMap<Integer, List<RoadSegment>> roadToRoadSegmentsTable = new HashMap<>();
    private HashMap<Integer, RoadInfo> roadInfoLookupTable = new HashMap<>();
    private HashMap<Integer, List<Restriction>> restrictionsMap = new HashMap<>();
    private List<Polygon> polygons = new ArrayList<>();

    private SearchTrie<RoadInfo> roadSearchTrie = new SearchTrie<>();
    private QuadTree nodeQuadTree = QuadTree.EMPTY;

    private int xOffset = 0;
    private int yOffset = 0;
    private double zoom = 1;

    private Point lastMouseLocation = null;

    private Node highlightedNodeA = null;
    private Node highlightedNodeB = null;
    private Set<RoadSegment> highlightedSegments = new HashSet<>();
    private Set<RoadSegment> pathFinding = new HashSet<>();
    private Set<Node> articulationPoints = new HashSet<>();

    /**
     * Draws all of the roads, intersections, and polygons to the given graphics object
     *
     * @param g graphics object to draw to
     */
    @Override
    protected void redraw(Graphics g) {
        if (nodeLookupTable == null || nodeLookupTable.size() == 0)
            return;

        //Fill the background with a single colour
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, getDrawingAreaDimension().width, getDrawingAreaDimension().height);

        //Pre-calculate drawing offsets
        Point centre = new Point(getDrawingAreaDimension().width / 2, getDrawingAreaDimension().height / 2);
        Point offset = new Point(xOffset, yOffset);

        //Draw polygons first as they lie underneath the roads
        polygons.forEach(polygon -> polygon.draw(g, offset, centre, zoom));

        //Draw all of the roads that are not highlighted next
        g.setColor(Color.BLACK);
        nodeAdjacencyTable.forEach((id, segments) -> segments.forEach((segment) -> {
            if (!highlightedSegments.contains(segment) && !pathFinding.contains(segment))
                segment.draw(g, offset, centre, zoom, roadInfoLookupTable.get(segment.getID()).getRoadClass());
        }));

        //Next draw any path found by A*
        g.setColor(Color.RED);
        pathFinding.forEach(segment -> segment.draw(g, offset, centre, zoom, 5));

        //Draw the highlighted roads on top
        g.setColor(Color.MAGENTA);
        highlightedSegments.forEach(segment -> segment.draw(g, offset, centre, zoom, 5));

        //Draw the polygon labels above that
        g.setColor(Color.BLACK);
        polygons.forEach(polygon -> polygon.drawLabel(g, offset, centre, zoom));

        //Draw all of the nodes
        g.setColor(Color.WHITE);
        nodeLookupTable.forEach((id, node) -> {
            if (articulationPoints.contains(node)) {
                g.setColor(Color.CYAN);
                node.draw(g, offset, centre, zoom, zoom * 2);
                g.setColor(Color.WHITE);
            }

            if (node == highlightedNodeA || node == highlightedNodeB) {
                g.setColor(Color.red);
                node.draw(g, offset, centre, zoom, zoom * 2);
                g.setColor(Color.WHITE);
            }

            node.draw(g, offset, centre, zoom, zoom * 1.25);
        });
    }

    /**
     * Event handler for when the mouse is clicked on the graphics pane
     */
    @Override
    protected void onClick(MouseEvent e) {
        //Don't do anything if this is the end of a drag
        if (lastMouseLocation != null) {
            lastMouseLocation = null;
            return;
        }

        //Pre-calculate offsets and locations
        Point centre = new Point(getDrawingAreaDimension().width / 2, getDrawingAreaDimension().height / 2);
        Point offset = new Point(xOffset, yOffset);
        Location clickLocation = Location.fromPoint(e.getPoint(), centre, zoom, offset, WINDOW_SCALE);
        Location bbLocation = new Location(clickLocation.x - 0.01, clickLocation.y - 0.01);

        //Query the quad tree with a small bounding box and find the closest node to the mouse click in that range
        //A linear search of the returned nodes is good enough as there will (in almost every case) be only a few to
        //compare
        Node closest = null;
        double closestDist = Double.POSITIVE_INFINITY;
        for (Node node : nodeQuadTree.queryRange(new BoundingBox(bbLocation, 0.02))) {
            double dist = node.getLocation().distance(clickLocation);
            if (dist < closestDist) {
                closestDist = dist;
                closest = node;
            }
        }

        Node selectedNode = closest;

        if (selectedNode == null) {
            highlightedNodeA = null;
            highlightedNodeB = null;
            pathFinding.clear();
            return;
        }

        if (highlightedNodeA == null) {
            highlightedNodeA = selectedNode;
            highlightedNodeB = null;
        }
        else {
            highlightedNodeB = selectedNode;
        }

        int id = selectedNode.getID();
        Set<String> roadNames = new HashSet<>();

        nodeAdjacencyTable.get(id).forEach(segment -> roadNames.add(roadInfoLookupTable.get(segment.getID()).getLabel()));

        //Parentheses fix weird bug where "[" doesn't get displayed.
        //I think it's because adding a char to an int results in another int
        getTextOutputArea().append('[' + (id + "]: ") + String.join(", ", roadNames) + '\n');

        if (highlightedNodeA != null && highlightedNodeB != null) {
            aStarSearch();
        }
    }

    /**
     * Event handler for when the mouse is dragged across the graphics pane
     */
    @Override
    protected void onDrag(MouseEvent e) {
        if (lastMouseLocation == null) {
            lastMouseLocation = e.getPoint();
            return;
        }

        //Move the map by the amount the mouse has moved
        double dx = e.getX() - lastMouseLocation.x;
        double dy = e.getY() - lastMouseLocation.y;
        xOffset += dx / zoom;
        yOffset += dy / zoom;

        lastMouseLocation = e.getPoint();
    }

    /**
     * Event handler for when the user has typed something into the text box
     *
     * @param term the term/prefix to search for
     * @return all of the results that match the term
     */
    @Override
    protected List<String> onSearch(String term) {
        List<String> suggestions = new ArrayList<>();
        highlightedSegments.clear();

        //No results if there is no input
        if (term == null || term.equals(""))
            return suggestions;

        List<RoadInfo> result = roadSearchTrie.findAll(term);

        if (result.size() == 0) {
            getTextOutputArea().append("No matches found\n");
            return suggestions;
        }

        //Build the list of unique street names
        Set<String> streetNames = new HashSet<>();
        result.forEach(roadInfo -> streetNames.add(roadInfo.getLabel()));
        suggestions.addAll(streetNames);
        suggestions.sort(String::compareTo);

        //Highlight all of the road segments
        for (RoadInfo roadInfo : result)
            if (roadToRoadSegmentsTable.containsKey(roadInfo.getID()))
                highlightedSegments.addAll(roadToRoadSegmentsTable.get(roadInfo.getID()));

        return suggestions;
    }

    /**
     * Event handler for the movement buttons
     */
    @Override
    protected void onMove(Move m) {
        switch (m) {
            case NORTH:
                yOffset += TRANSLATE_RATE / zoom;
                break;
            case SOUTH:
                yOffset -= TRANSLATE_RATE / zoom;
                break;
            case EAST:
                xOffset -= TRANSLATE_RATE / zoom;
                break;
            case WEST:
                xOffset += TRANSLATE_RATE / zoom;
                break;
            case ZOOM_IN:
                zoom *= ZOOM_RATE;
                break;
            case ZOOM_OUT:
                zoom /= ZOOM_RATE;
                break;
        }
    }

    @Override
    protected void onArtPtsButton() {
        if (nodeLookupTable.size() == 0)
            return;

        articulationPoints = calculateAPs();
        getTextOutputArea().append(String.format("Found %d articulation points\n", articulationPoints.size()));
    }

    @Override
    protected void onAStarModeChange() {
        if (highlightedNodeA != null && highlightedNodeB != null) {
            aStarSearch();
        }
    }

    /**
     * Event handler for when the user scrolls in or out
     */
    @Override
    protected void onScroll(MouseWheelEvent e) {
        //Scroll in or out depending on the scroll direction
        if (e.getUnitsToScroll() > 0) {
            zoom /= ZOOM_RATE;
        }
        else {
            zoom *= ZOOM_RATE;
        }
    }

    /**
     * Event handler for when the data folder has been selected
     *
     * @param nodes         a File for nodeID-lat-lon.tab
     * @param roads         a File for roadID-roadInfo.tab
     * @param segments      a File for roadSeg-roadID-length-nodeID-nodeID-coords.tab
     * @param polygons      a File for polygon-shapes.mp
     * @param restrictions  a File for restrictions.tab
     * @param trafficLights a File for traffic-lights.tab
     */
    @Override
    protected void onLoad(File nodes, File roads, File segments, File polygons, File restrictions, File trafficLights) {
        //Reset all of the variables
        xOffset = 0;
        yOffset = 0;
        zoom = 1;

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        nodeLookupTable.clear();
        nodeAdjacencyTable.clear();
        neighbouringNodesTable.clear();
        roadToRoadSegmentsTable.clear();
        roadInfoLookupTable.clear();
        restrictionsMap.clear();
        this.polygons.clear();

        roadSearchTrie = new SearchTrie<>();
        nodeQuadTree = QuadTree.EMPTY;

        highlightedNodeA = null;
        highlightedNodeB = null;
        highlightedSegments.clear();
        pathFinding.clear();
        articulationPoints.clear();

        //Attempt to read the nodes file
        try (BufferedReader reader = new BufferedReader(new FileReader(nodes))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Node node = new Node(line);
                nodeLookupTable.put(node.getID(), node);

                Location location = node.getLocation();
                if (location.x < minX) minX = location.x;
                if (location.y < minY) minY = location.y;
                if (location.x > maxX) maxX = location.x;
                if (location.y > maxY) maxY = location.y;
            }

            //Calculate the size of the bounding box for the quad tree
            double width = maxX - minX;
            double height = maxY - minY;
            double size = width > height ? width : height;

            //Initialise the quad tree
            nodeQuadTree = new QuadTree(new BoundingBox(new Location(minX, minY), size));
            nodeLookupTable.values().forEach(node -> nodeQuadTree.insert(node));
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //Attempt to read the roads file
        try (BufferedReader reader = new BufferedReader(new FileReader(roads))) {
            reader.readLine(); //Ignore header line

            String line;
            while ((line = reader.readLine()) != null) {
                RoadInfo roadInfo = new RoadInfo(line);

                //Add the roadInfo to the lookup table and the trie
                roadInfoLookupTable.put(roadInfo.getID(), roadInfo);
                roadSearchTrie.insert(roadInfo.getLabel(), roadInfo);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //Attempt to read the segments file
        try (BufferedReader reader = new BufferedReader(new FileReader(segments))) {
            reader.readLine(); //Ignore header line

            String line;
            while ((line = reader.readLine()) != null) {
                RoadSegment roadSegment = new RoadSegment(line);

                //Add the roadSegment to all of the required tables and positions
                nodeAdjacencyTable.computeIfAbsent(roadSegment.getNodeID1(), v -> new ArrayList<>()).add(roadSegment);
                List<RoadSegment> list = nodeAdjacencyTable.computeIfAbsent(roadSegment.getNodeID2(), v -> new ArrayList<>());
                if (!roadInfoLookupTable.get(roadSegment.getID()).isOneWay())
                    list.add(roadSegment);

                neighbouringNodesTable.computeIfAbsent(roadSegment.getNodeID1(), v -> new ArrayList<>()).add(nodeLookupTable.get(roadSegment.getNodeID2()));
                neighbouringNodesTable.computeIfAbsent(roadSegment.getNodeID2(), v -> new ArrayList<>()).add(nodeLookupTable.get(roadSegment.getNodeID1()));
                roadToRoadSegmentsTable.computeIfAbsent(roadSegment.getID(), v -> new ArrayList<>()).add(roadSegment);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //Attempt to read the restrictions file
        try (BufferedReader reader = new BufferedReader(new FileReader(restrictions))) {
            reader.readLine(); //Ignore header line

            String line;
            while ((line = reader.readLine()) != null) {
                Restriction restriction = new Restriction(line);

                restrictionsMap.computeIfAbsent(restriction.getNodeID(), v -> new ArrayList<>()).add(restriction);
            }
        }
        catch (NullPointerException ignored) { }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Attempt to read the polygons file
        try (BufferedReader reader = new BufferedReader(new FileReader(polygons))) {
            String line, type = "0", label = "", endLevel = "";
            List<String> data = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.equals("[POLYGON]")) { //Starting construction of a new polygon
                    type = "0";
                    label = "";
                    endLevel = "";
                    data.clear();
                }
                else if (line.startsWith("Type")) { //Only parse the lines we want
                    type = line.substring(7);
                }
                else if (line.startsWith("Label")) {
                    label = line.substring(6);
                }
                else if (line.startsWith("EndLevel")) {
                    endLevel = line.substring(9);
                }
                else if (line.startsWith("Data")) {
                    data.add(line.substring(6));
                }
                else if (line.equals("[END]")) { //Finished construction of the polygon
                    this.polygons.add(new Polygon(type, label, endLevel, data));
                }
            }

            //Sort the polygon list
            Collections.sort(this.polygons);
        }
        catch (NullPointerException ignored) { }
        catch (Exception e) {
            e.printStackTrace();
        }

        List<Location> lights = new ArrayList<>();
        //Attempt to read the traffic lights file
        //Traffic lights data from https://raw.githubusercontent.com/d1Ng0/COMP261/master/comp261-a02/data/large/NZtrafficLightCoords.txt
        try (BufferedReader reader = new BufferedReader(new FileReader(trafficLights))) {
            reader.readLine(); //Ignore the header line

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (parts.length < 2)
                    continue;

                lights.add(Location.fromLatLon(Double.parseDouble(parts[1]), Double.parseDouble(parts[0])));
            }
        }
        catch (NullPointerException ignored) { }
        catch (Exception e) {
            e.printStackTrace();
        }

        for (int nodeID : neighbouringNodesTable.keySet()) {
            Node node = nodeLookupTable.get(nodeID);
            //Can't use a set.contains method here because of the imprecision of doubles, so we use an equality method instead
            for (Location light : lights) {
                if (light.equals(node.getLocation())) {
                    node.setHasTrafficLight(true);
                    break;
                }
            }
        }
    }

    /**
     * Uses A* to search the graph for a route between the two highlighted nodes, then displays the route on the map
     */
    private void aStarSearch() {
        List<Node> nodePath = aStarSearch(highlightedNodeA, highlightedNodeB);

        pathFinding.clear();

        //A path is only found if there are two or more nodes in the path
        if (nodePath.size() <= 1) {
            getTextOutputArea().append("No path found\n");
            return;
        }

        //Construct the list of segments along the route
        ArrayList<String> pathNames = new ArrayList<>();
        ArrayList<Double> pathCosts = new ArrayList<>();
        int pathIndex = -1;
        for (int i = 0; i < nodePath.size() - 1; i++) {
            int nodeID1 = nodePath.get(i).getID();
            int nodeID2 = nodePath.get(i + 1).getID();

            //Prevents null pointer exception
            String prevName;
            if (pathIndex < 0)
                prevName = "";
            else
                prevName = pathNames.get(pathIndex);

            //Find the segment between the two nodes
            for (RoadSegment r : nodeAdjacencyTable.get(nodeID1)) {
                if (r.getOtherNode(nodeID1) == nodeID2) {
                    String roadName = roadInfoLookupTable.get(r.getID()).getLabel();

                    if (roadName.equals(prevName)) {
                        pathCosts.set(pathIndex, pathCosts.get(pathIndex) + r.getLength());
                    }
                    else {
                        pathNames.add(roadName);
                        pathCosts.add(r.getLength());
                        pathIndex++;
                    }

                    pathFinding.add(r);
                    break;
                }
            }
        }

        //Write the path (without duplicates) to the text output area
        getTextOutputArea().append("Path:\n");
        for (int i = 0; i < pathNames.size(); i++) {
            getTextOutputArea().append(String.format("%s: %.3fkm\n", pathNames.get(i), pathCosts.get(i)));
        }
    }

    /**
     * Implements A* to search the map for a path from the given node to the goal
     *
     * @param start the node to start searching from
     * @param goal  the node to look for a path to
     * @return the path between the two nodes
     */
    private List<Node> aStarSearch(Node start, Node goal) {
        if (start.equals(goal))
            return new ArrayList<>();

        HashMap<Node, Node> exploredPath = new HashMap<>(); //Maps parent -> child

        //Maps to keep track of distance costs
        HashMap<Node, Double> costToGoal = new HashMap<>();
        HashMap<Node, Double> pathCost = new HashMap<>();

        //The fringe is a priority queue with sorting based on the estimated cost from the node to the goal
        PriorityQueue<Node> fringe = new PriorityQueue<>(Comparator.comparingDouble(costToGoal::get));
        Set<Node> visited = new HashSet<>();

        //We have to scale the heuristic function if the search is speed based so that it's still a lower bound
        double heuristicScaleFactor = isSpeedHeuristic ? 0.0083 : 1;

        //Add the starting node to the fringe, and add its costs to the maps
        costToGoal.put(start, start.getLocation().distance(goal.getLocation()) * heuristicScaleFactor);
        pathCost.put(start, 0d);
        fringe.add(start);

        //Loop until either the goal is found or we run out of nodes
        while (!fringe.isEmpty()) {
            //Get the node at the top of the queue
            Node node = fringe.poll();

            //Don't revisit already visited nodes
            if (visited.contains(node))
                continue;

            visited.add(node);

            if (node.equals(goal))
                break;

            //Create a set of nodes that we cannot path to, based off of data from the restrictions map
            Set<Node> restrictedNodes = restrictionsMap.getOrDefault(node.getID(), new ArrayList<>())
                    .stream()
                    .filter(r -> exploredPath.containsKey(node) && r.getNodeID1() == exploredPath.get(node).getID())
                    .map(r -> nodeLookupTable.get(r.getNodeID2()))
                    .collect(Collectors.toSet());

            for (RoadSegment segment : nodeAdjacencyTable.get(node.getID())) {
                Node neighbour = nodeLookupTable.get(segment.getOtherNode(node.getID()));
                RoadInfo roadInfo = roadInfoLookupTable.get(segment.getID());

                if (visited.contains(neighbour) || restrictedNodes.contains(neighbour))
                    continue;

                //The heuristic function for this implementation is the segments length, augmented by the road weight
                // (the road speed and class) if we are using speed as the heuristic, further augmented by a weight
                // which depends on if we want to avoid traffic lights or not (this weight is arbitrary).
                //This function should be admissible as it always tries to underestimate the cost
                double cost = pathCost.get(node) + segment.getLength()
                        / (isSpeedHeuristic ? roadInfo.getWeightedRoadSpeed() : 1)
                        * (useTrafficLights && neighbour.hasTrafficLight() ? 1 : 2);

                //Assign the relevant costs to the neighbour, queue it, and put it into the explored path map
                pathCost.put(neighbour, cost);
                costToGoal.put(neighbour, cost + neighbour.getLocation().distance(goal.getLocation()) * heuristicScaleFactor);
                fringe.add(neighbour);
                exploredPath.put(neighbour, node);
            }
        }

        //Reconstruct the path
        List<Node> path = new ArrayList<>();
        path.add(goal);

        Node current = goal;
        while (exploredPath.containsKey(current)) {
            current = exploredPath.get(current);
            path.add(current);
        }

        //Put the start node at the beginning, and the goal node at the end.
        // Kind of unnecessary for this implementation, but it makes a little sense, and doesn't really add much to the
        // run time.
        Collections.reverse(path);

        return path;
    }

    /**
     * Calculates all of the articulation points in the entire graph
     *
     * @return a set of all of the articulation points
     */
    private Set<Node> calculateAPs() {
        Set<Node> aps = new HashSet<>();

        Set<Node> visited = new HashSet<>();

        for (int nodeID : neighbouringNodesTable.keySet()) {
            Node node = nodeLookupTable.get(nodeID);

            if (!visited.contains(node)) {
                //Get the aps in the component
                aps.addAll(calculateAPs(node));

                //Visit the rest of the nodes in the component
                Stack<Node> toCheck = new Stack<>();
                toCheck.push(node);
                while (!toCheck.empty()) {
                    Node n = toCheck.pop();

                    if (visited.contains(n))
                        continue;

                    for (Node neighbour : neighbouringNodesTable.get(n.getID()))
                        toCheck.push(neighbour);

                    visited.add(n);
                }
            }
        }

        return aps;
    }

    /**
     * Finds all of the articulation points in the given component
     *
     * @param root the root node to start the search from
     * @return the set of all articulation points in the component
     */
    private Set<Node> calculateAPs(Node root) {
        Set<Node> aps = new HashSet<>();

        Set<Node> explored = new HashSet<>(); //Keep track of the nodes we've visited so there's no doubling up
        Stack<Node> fringe = new Stack<>(); //Stack of nodes to examine
        HashMap<Node, Node> parents = new HashMap<>(); //Maps node -> parent node
        HashMap<Node, Stack<Node>> children = new HashMap<>(); //Using a stack so it's easier to get and remove children
        HashMap<Node, Integer> depths = new HashMap<>(); //Maps node -> current depth of node
        HashMap<Node, Integer> reachBacks = new HashMap<>(); //Maps node -> node's reach back value

        //Initialise in the maps for the root node
        depths.put(root, 0);
        reachBacks.put(root, 0);

        int numSubTrees = 0;
        for (Node rootNeighbour : neighbouringNodesTable.get(root.getID())) {
            parents.put(rootNeighbour, root);

            if (!explored.contains(rootNeighbour)) { //If the root neighbour hasn't yet been explored
                fringe.push(rootNeighbour);

                while (!fringe.empty()) {
                    Node node = fringe.peek();

                    if (!explored.contains(node)) { //If the node has not yet been explored
                        int depth = depths.get(parents.get(node)) + 1;
                        depths.put(node, depth);
                        reachBacks.put(node, depth);
                        Stack<Node> neighbours = new Stack<>();
                        neighbouringNodesTable.get(node.getID()).forEach((n) -> {
                            if (!n.equals(parents.get(node)))
                                neighbours.push(n);
                        });
                        children.put(node, neighbours);
                        explored.add(node);
                    }
                    else if (children.containsKey(node) && !children.get(node).isEmpty()) {
                        Node child = children.get(node).pop();

                        if (depths.containsKey(child)) {
                            reachBacks.put(node, Math.min(depths.get(child), reachBacks.get(node)));
                        }
                        else {
                            depths.put(child, depths.get(node) + 1);
                            reachBacks.put(child, depths.get(node) + 1);
                            parents.put(child, node);
                            fringe.push(child);
                        }
                    }
                    else {
                        if (!node.equals(rootNeighbour)) {
                            Node parent = parents.get(node);

                            reachBacks.put(parent, Math.min(reachBacks.get(node), reachBacks.get(parent)));

                            if (reachBacks.get(node) >= depths.get(parent))
                                aps.add(parent);
                        }

                        fringe.pop();
                    }
                }

                numSubTrees++;
            }
        }

        //If there exist more than two subtrees, then the root is an ap
        if (numSubTrees > 1)
            aps.add(root);

        return aps;
    }

    public static void main(String[] args) {
        new MapViewer();
    }
}
