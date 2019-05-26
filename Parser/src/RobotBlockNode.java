public interface RobotBlockNode extends RobotProgramNode {
    void preExec(Robot robot);
    void postExec(Robot robot);

    RobotBlockNode NULL = new RobotBlockNode() {
        public void preExec(Robot robot) { }

        public void postExec(Robot robot) { }

        public void execute(Robot robot) { }

        public String toString() {
            return "NULL";
        }
    };
}
