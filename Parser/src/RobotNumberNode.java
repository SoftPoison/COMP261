public interface RobotNumberNode {
    int execute(Robot robot);

    RobotNumberNode FUEL_LEFT = new RobotNumberNode() {
        public int execute(Robot robot) {
            return robot.getFuel();
        }

        public String toString() {
            return "fuelLeft";
        }
    };

    RobotNumberNode OPP_LR = new RobotNumberNode() {
        public int execute(Robot robot) {
            return robot.getOpponentLR();
        }

        public String toString() {
            return "oppLR";
        }
    };

    RobotNumberNode OPP_FB = new RobotNumberNode() {
        public int execute(Robot robot) {
            return robot.getOpponentFB();
        }

        public String toString() {
            return "oppFB";
        }
    };

    RobotNumberNode NUM_BARRELS = new RobotNumberNode() {
        public int execute(Robot robot) {
            return robot.numBarrels();
        }

        public String toString() {
            return "numBarrels";
        }
    };

    RobotNumberNode BARREL_LR = new RobotNumberNode() {
        public int execute(Robot robot) {
            return robot.getClosestBarrelLR();
        }

        public String toString() {
            return "barrelLR";
        }
    };

    RobotNumberNode BARREL_FB = new RobotNumberNode() {
        public int execute(Robot robot) {
            return robot.getClosestBarrelFB();
        }

        public String toString() {
            return "barrelFB";
        }
    };

    RobotNumberNode WALL_DIST = new RobotNumberNode() {
        public int execute(Robot robot) {
            return robot.getDistanceToWall();
        }

        public String toString() {
            return "wallDist";
        }
    };
}
