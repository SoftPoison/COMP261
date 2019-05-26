public class RoadInfo {
    private int id;
    private int type;
    private String label;
    private String city;
    private boolean oneWay;
    private int speed;
    private int roadClass;
    private boolean notForCars;
    private boolean notForPedestrians;
    private boolean notForCyclists;

    /**
     * Holds information about a road
     */
    public RoadInfo(String line) {
        String[] parts = line.split("\t");
        id = Integer.parseInt(parts[0]);
        type = Integer.parseInt(parts[1]);
        label = parts[2];
        if (label.equals("-"))
            label = "Unnamed road";
        city = parts[3];
        oneWay = parts[4].equals("1");
        speed = Integer.parseInt(parts[5]);
        roadClass = Integer.parseInt(parts[6]);
        notForCars = parts[7].equals("1");
        notForPedestrians = parts[8].equals("1");
        notForCyclists = parts[9].equals("1");
    }

    public int getID() {
        return id;
    }

    public int getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getCity() {
        return city;
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public int getSpeed() {
        return speed;
    }

    public double getActualSpeed() {
        switch (speed) {
            case 0:
                return 5;
            case 1:
                return 20;
            case 2:
                return 40;
            case 3:
                return 60;
            case 4:
                return 80;
            case 5:
                return 100;
            case 6:
                return 110;
            default:
                return Double.MAX_VALUE;
        }
    }

    public int getRoadClass() {
        return roadClass;
    }

    public double getWeightedRoadSpeed() {
        //The constants are arbitrary, and *sort of* follow an exponential curve
        switch (roadClass) {
            case 0:
                return getActualSpeed() * 0.72;
            case 1:
                return getActualSpeed() * 0.81;
            case 2:
                return getActualSpeed() * 0.89;
            case 3:
                return getActualSpeed() * 0.95;
            default:
                return getActualSpeed();
        }
    }

    public boolean isNotForCars() {
        return notForCars;
    }

    public boolean isNotForPedestrians() {
        return notForPedestrians;
    }

    public boolean isNotForCyclists() {
        return notForCyclists;
    }
}
