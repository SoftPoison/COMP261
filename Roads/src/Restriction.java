public class Restriction {
    private int nodeID1;
    private int roadID1;
    private int nodeID;
    private int roadID2;
    private int nodeID2;

    /**
     * Holds information about a restriction
     */
    public Restriction(String line) {
        String[] parts = line.split("\t");
        nodeID1 = Integer.parseInt(parts[0]);
        roadID1 = Integer.parseInt(parts[1]);
        nodeID = Integer.parseInt(parts[2]);
        roadID2 = Integer.parseInt(parts[3]);
        nodeID2 = Integer.parseInt(parts[4]);
    }

    public int getNodeID1() {
        return nodeID1;
    }

    public int getRoadID1() {
        return roadID1;
    }

    public int getNodeID() {
        return nodeID;
    }

    public int getRoadID2() {
        return roadID2;
    }

    public int getNodeID2() {
        return nodeID2;
    }
}
