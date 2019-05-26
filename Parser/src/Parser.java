import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;

/**
 * The parser and interpreter. The top level parse function, a main method for
 * testing, and several utility methods are provided. You need to implement
 * parseProgram and all the rest of the parser.
 */
public class Parser {

    /**
     * Top level parse method, called by the World
     */
    static RobotProgramNode parseFile(File code) {
        Scanner scan = null;
        try {
            scan = new Scanner(code);

            // the only time tokens can be next to each other is
            // when one of them is one of (){},;
            scan.useDelimiter("\\s+|(?=[{}(),;])|(?<=[{}(),;])");

            RobotProgramNode n = parseProgram(scan); // You need to implement this!!!

            scan.close();
            return n;
        }
        catch (FileNotFoundException e) {
            System.out.println("Robot program source file not found");
        }
        catch (ParserFailureException e) {
            System.out.println("Parser error:");
            System.out.println(e.getMessage());
            scan.close();
        }
        return null;
    }

    /**
     * For testing the parser without requiring the world
     */

    public static void main(String[] args) {
        if (args.length > 0) {
            for (String arg : args) {
                File f = new File(arg);
                if (f.exists()) {
                    System.out.println("Parsing '" + f + "'");
                    RobotProgramNode prog = parseFile(f);
                    System.out.println("Parsing completed ");
                    if (prog != null) {
                        System.out.println("================\nProgram:");
                        System.out.println(prog);
                    }
                    System.out.println("=================");
                }
                else {
                    System.out.println("Can't find file '" + f + "'");
                }
            }
        }
        else {
            while (true) {
                JFileChooser chooser = new JFileChooser(".");// System.getProperty("user.dir"));
                int res = chooser.showOpenDialog(null);
                if (res != JFileChooser.APPROVE_OPTION) {
                    break;
                }
                RobotProgramNode prog = parseFile(chooser.getSelectedFile());
                System.out.println("Parsing completed");
                if (prog != null) {
                    System.out.println("Program: \n" + prog);
                }
                System.out.println("=================");
            }
        }
        System.out.println("Done");
    }

    // Useful Patterns

    static Pattern NUMPAT = Pattern.compile("-?[1-9][0-9]*|0");
    static Pattern VARPAT = Pattern.compile("\\$[A-Za-z][A-Za-z0-9]*");
    static Pattern OPENPAREN = Pattern.compile("\\(");
    static Pattern CLOSEPAREN = Pattern.compile("\\)");
    static Pattern OPENBRACE = Pattern.compile("\\{");
    static Pattern CLOSEBRACE = Pattern.compile("\\}");

    /**
     * PROG ::= STMT+
     */
    static RobotProgramNode parseProgram(Scanner s) {
        return parsePROG(s);
    }

    // utility methods for the parser

    /**
     * Report a failure in the parser.
     */
    static void fail(String message, Scanner s) {
        StringBuilder msg = new StringBuilder(message + "\n   @ ...");
        for (int i = 0; i < 5 && s.hasNext(); i++) {
            msg.append(" ").append(s.next());
        }
        throw new ParserFailureException(msg + "...");
    }

    /**
     * Requires that the next token matches a pattern if it matches, it consumes
     * and returns the token, if not, it throws an exception with an error
     * message
     */
    static String require(String p, String message, Scanner s) {
        if (s.hasNext(p)) {
            return s.next();
        }
        fail(message, s);
        return null;
    }

    static String require(Pattern p, String message, Scanner s) {
        if (s.hasNext(p)) {
            return s.next();
        }
        fail(message, s);
        return null;
    }

    /**
     * Requires that the next token matches a pattern (which should only match a
     * number) if it matches, it consumes and returns the token as an integer if
     * not, it throws an exception with an error message
     */
    static int requireInt(String p, String message, Scanner s) {
        if (s.hasNext(p) && s.hasNextInt()) {
            return s.nextInt();
        }
        fail(message, s);
        return -1;
    }

    static int requireInt(Pattern p, String message, Scanner s) {
        if (s.hasNext(p) && s.hasNextInt()) {
            return s.nextInt();
        }
        fail(message, s);
        return -1;
    }

    /**
     * Checks whether the next token in the scanner matches the specified
     * pattern, if so, consumes the token and return true. Otherwise returns
     * false without consuming anything.
     */
    static boolean checkFor(String p, Scanner s) {
        if (s.hasNext(p)) {
            s.next();
            return true;
        }
        else {
            return false;
        }
    }

    static boolean checkFor(Pattern p, Scanner s) {
        if (s.hasNext(p)) {
            s.next();
            return true;
        }
        else {
            return false;
        }
    }

    private static RobotProgramNode parsePROG(Scanner s) {
        RobotProgramNode decl = parseDECL(s);

        //Build the list of child nodes
        List<RobotProgramNode> children = new ArrayList<>();

        if (decl != null)
            children.add(decl);

        while (s.hasNext()) {
            RobotProgramNode n = parseSTMT(s);
            if (n != null)
                children.add(n);
            else
                fail("Malformed STMT", s);
        }

        return new RobotProgramNode() {
            public void execute(Robot robot) {
                for (RobotProgramNode child : children)
                    child.execute(robot);
            }

            public String toString() {
                StringBuilder sb = new StringBuilder();
                for (RobotProgramNode child : children)
                    sb.append(child.toString());
                return sb.toString();
            }
        };
    }

    private static RobotProgramNode parseDECL(Scanner s) {
        if (checkFor("vars", s)) {
            List<String> vars = new ArrayList<>();
            do {
                String var = require(VARPAT, "Malformed variable", s);
                if (var == null)
                    return null;
                vars.add(var.substring(1));
            }
            while (checkFor(",", s));

            require(";", "Missing semicolon", s);

            return new RobotProgramNode() {
                public void execute(Robot robot) {
                    for (String var : vars)
                        if (robot.getVariable(var) != null)
                            robot.setVariable(var, 0);
                }

                public String toString() {
                    return vars.stream().collect(Collectors.joining(", ", "vars ", ";\n"));
                }
            };
        }

        return null;
    }

    private static RobotProgramNode parseSTMT(Scanner s) {
        //Try to parse any of the possible things it could be

        RobotProgramNode stmt = parseACT(s);
        if (stmt != null) {
            require(";", "Missing semicolon", s);
            return stmt;
        }

        stmt = parseLOOP(s);
        if (stmt != null)
            return stmt;

        stmt = parseIF(s);
        if (stmt != null)
            return stmt;

        stmt = parseWHILE(s);
        if (stmt != null)
            return stmt;

        stmt = parseASSGN(s);
        if (stmt != null)
            return stmt;

        //If it doesn't match anything
        return null;
    }

    private static RobotProgramNode parseACT(Scanner s) {
        //A whole bunch of if statements switching through different actions the robot can do

        if (checkFor("move", s)) {
            //Has argument
            if (checkFor(OPENPAREN, s)) {
                RobotNumberNode exp = parseEXP(s);
                require(CLOSEPAREN, "Missing ')'", s);

                return new RobotProgramNode() {
                    public void execute(Robot robot) {
                        int dist = exp.execute(robot);
                        for (int i = 0; i < dist; i++)
                            robot.move();
                    }

                    public String toString() {
                        return "move(" + exp + ");\n";
                    }
                };
            }

            return new RobotProgramNode() {
                public void execute(Robot robot) {
                    robot.move();
                }

                public String toString() {
                    return "move;\n";
                }
            };
        }
        if (checkFor("wait", s)) {
            //Has argument
            if (checkFor(OPENPAREN, s)) {
                RobotNumberNode exp = parseEXP(s);
                require(CLOSEPAREN, "Missing ')'", s);

                return new RobotProgramNode() {
                    public void execute(Robot robot) {
                        int dist = exp.execute(robot);
                        for (int i = 0; i < dist; i++)
                            robot.idleWait();
                    }

                    public String toString() {
                        return "wait(" + exp + ");\n";
                    }
                };
            }

            return new RobotProgramNode() {
                public void execute(Robot robot) {
                    robot.idleWait();
                }

                public String toString() {
                    return "wait;\n";
                }
            };
        }
        if (checkFor("turnL", s))
            return new RobotProgramNode() {
                public void execute(Robot robot) {
                    robot.turnLeft();
                }

                public String toString() {
                    return "turnL;\n";
                }
            };
        if (checkFor("turnR", s))
            return new RobotProgramNode() {
                public void execute(Robot robot) {
                    robot.turnRight();
                }

                public String toString() {
                    return "turnR;\n";
                }
            };
        if (checkFor("turnAround", s))
            return new RobotProgramNode() {
                public void execute(Robot robot) {
                    robot.turnAround();
                }

                public String toString() {
                    return "turnAround;\n";
                }
            };
        if (checkFor("shieldOn", s))
            return new RobotProgramNode() {
                public void execute(Robot robot) {
                    robot.setShield(true);
                }

                public String toString() {
                    return "shieldOn;\n";
                }
            };
        if (checkFor("shieldOff", s))
            return new RobotProgramNode() {
                public void execute(Robot robot) {
                    robot.setShield(false);
                }

                public String toString() {
                    return "shieldOff;\n";
                }
            };
        if (checkFor("takeFuel", s))
            return new RobotProgramNode() {
                public void execute(Robot robot) {
                    robot.takeFuel();
                }

                public String toString() {
                    return "takeFuel;\n";
                }
            };

        return null;
    }

    private static RobotProgramNode parseLOOP(Scanner s) {
        if (checkFor("loop", s)) {
            RobotBlockNode block = parseBLOCK(s);
            if (block == null)
                return null;

            return new RobotProgramNode() {
                public void execute(Robot r) {
                    block.preExec(r);
                    while (r.getFuel() > 0) //Run until the robot is out of fuel
                        block.execute(r);
                    block.postExec(r);
                }

                public String toString() {
                    return "loop " + block;
                }
            };
        }

        return null;
    }

    private static RobotProgramNode parseIF(Scanner s) {
        if (checkFor("if", s)) {
            List<RobotConditionNode> conditions = new ArrayList<>();
            List<RobotBlockNode> blocks = new ArrayList<>();

            //Parse if and elifs
            do {
                require(OPENPAREN, "Missing '('", s);

                conditions.add(parseCOND(s));

                require(CLOSEPAREN, "Missing ')'", s);

                blocks.add(parseBLOCK(s));
            } while (checkFor("elif", s));

            //Check for optional else block
            RobotBlockNode elseBlock = RobotBlockNode.NULL;
            if (checkFor("else", s)) {
                elseBlock = parseBLOCK(s);
                if (elseBlock == null)
                    elseBlock = RobotBlockNode.NULL;
            }

            if (s.hasNext("elif"))
                fail("elif must come before else", s);

            //Copy the else block so it can be passed into the anonymous class
            final RobotBlockNode finalElseBlock = elseBlock;
            return new RobotProgramNode() {
                public void execute(Robot r) {
                    for (int i = 0; i < conditions.size(); i++) {
                        if (conditions.get(i).execute(r)) {
                            RobotBlockNode block = blocks.get(i);
                            block.preExec(r);
                            block.execute(r);
                            block.postExec(r);
                            return;
                        }
                    }

                    finalElseBlock.preExec(r);
                    finalElseBlock.execute(r);
                    finalElseBlock.postExec(r);
                }

                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("if (");
                    sb.append(conditions.get(0));
                    sb.append(") ");
                    sb.append(blocks.get(0));

                    for (int i = 1; i < conditions.size(); i++) {
                        sb.append("if (");
                        sb.append(conditions.get(i));
                        sb.append(") ");
                        sb.append(blocks.get(i));
                    }

                    if (finalElseBlock != RobotBlockNode.NULL) {
                        sb.append("else ");
                        sb.append(finalElseBlock);
                    }

                    return sb.toString();
                }
            };
        }

        return null;
    }

    private static RobotProgramNode parseWHILE(Scanner s) {
        if (checkFor("while", s)) {
            require(OPENPAREN, "Missing '('", s);

            RobotConditionNode cond = parseCOND(s);
            if (cond == null)
                return null;

            require(CLOSEPAREN, "Missing ')'", s);

            RobotBlockNode block = parseBLOCK(s);
            if (block == null)
                return null;

            return new RobotProgramNode() {
                public void execute(Robot r) {
                    block.preExec(r);
                    while (cond.execute(r))
                        block.execute(r);
                    block.postExec(r);
                }

                public String toString() {
                    return "while (" + cond + ") " + block;
                }
            };
        }

        return null;
    }

    private static RobotProgramNode parseASSGN(Scanner s) {
        if (s.hasNext(VARPAT)) {
            //Remove the dollar sign
            String name = s.next().substring(1);

            require("=", "Missing '='", s);

            RobotNumberNode value = parseEXP(s);

            require(";", "Missing semicolon", s);

            return new RobotProgramNode() {
                public void execute(Robot robot) {
                    robot.setVariable(name, value.execute(robot));
                }

                public String toString() {
                    return "$" + name + " = " + value + ";\n";
                }
            };
        }

        return null;
    }

    private static RobotBlockNode parseBLOCK(Scanner s) {
        require(OPENBRACE, "Missing '{'", s);

        //Parse the children
        List<RobotProgramNode> children = new ArrayList<>();

        RobotProgramNode decl = parseDECL(s);
        if (decl != null)
            children.add(decl);

        while (!s.hasNext(CLOSEBRACE)) {
            RobotProgramNode n = parseSTMT(s);
            if (n != null)
                children.add(n);
            else if (!s.hasNext(CLOSEBRACE))
                fail("Missing '}'", s);
            else
                fail("Unknown stmt", s);
        }

        require(CLOSEBRACE, "Missing '}'", s);

        if (children.size() == 0) {
            fail("Block must have at least one child", s);
            return null;
        }

        return new RobotBlockNode() {
            //Deletes variables once they go out of scope
            Set<String> declaredVars = null;

            public void preExec(Robot robot) {
                declaredVars = robot.getDeclaredVariables();
            }

            public void execute(Robot robot) {
                for (RobotProgramNode child : children)
                    child.execute(robot);
            }

            public void postExec(Robot robot) {
                if (declaredVars != null) {
                    //Delete variables that have gone out of scope
                    Set<String> newVars = robot.getDeclaredVariables();
                    if (newVars.removeAll(declaredVars)) {
                        for (String var : newVars)
                            robot.deleteVariable(var);
                    }

                    /*
                        //Should be:
                        Set<String> newVars = robot.getDeclaredVariables();
                        newVars.removeAll(declaredVars);
                        for (String var : newVars)
                            robot.deleteVariable(var);
                     */
                }
            }

            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("{\n");
                for (RobotProgramNode child : children)
                    sb.append(child.toString());
                sb.append("}\n");
                return sb.toString();
            }
        };
    }

    private static RobotNumberNode parseEXP(Scanner s) {
        RobotNumberNode exp = buildEXP(s);
        if (exp != null)
            return exp;

        exp = parseSEN(s);
        if (exp != null)
            return exp;

        exp = parseVAR(s);
        if (exp != null)
            return exp;

        //Last case is parsing it as a number, which can fail if it isn't one
        return parseNUM(s);
    }

    private static RobotNumberNode buildEXP(Scanner s) {
        //Cuts down on repeated code
        String type = null;
        if (checkFor("add", s))
            type = "add";
        else if (checkFor("sub", s))
            type = "sub";
        else if (checkFor("mul", s))
            type = "mul";
        else if (checkFor("div", s))
            type = "div";

        if (type != null) {
            require(OPENPAREN, "Missing '('", s);

            RobotNumberNode argA = parseEXP(s);
            if (argA == null)
                return null;

            require(",", "Missing ','", s);

            RobotNumberNode argB = parseEXP(s);
            if (argB == null)
                return null;

            require(CLOSEPAREN, "Missing ')'", s);

            switch (type) {
                case "add":
                    return new RobotNumberNode() {
                        public int execute(Robot r) {
                            return argA.execute(r) + argB.execute(r);
                        }

                        public String toString() {
                            return "(" + argA + " + " + argB + ")";
                        }
                    };
                case "sub":
                    return new RobotNumberNode() {
                        public int execute(Robot r) {
                            return argA.execute(r) - argB.execute(r);
                        }

                        public String toString() {
                            return "(" + argA + " - " + argB + ")";
                        }
                    };
                case "mul":
                    return new RobotNumberNode() {
                        public int execute(Robot r) {
                            return argA.execute(r) * argB.execute(r);
                        }

                        public String toString() {
                            return "(" + argA + " * " + argB + ")";
                        }
                    };
                case "div":
                    return new RobotNumberNode() {
                        public int execute(Robot r) {
                            return argA.execute(r) / argB.execute(r);
                        }

                        public String toString() {
                            return "(" + argA + " / " + argB + ")";
                        }
                    };
            }
        }

        return null;
    }

    private static RobotConditionNode parseCOND(Scanner s) {
        RobotRelopNode relop = parseRELOP(s);
        if (relop == null) {
            RobotConditionNode cond = buildCOND(s);
            if (cond == null)
                fail("Unknown condition", s);

            return cond;
        }

        require(OPENPAREN, "Missing '('", s);

        RobotNumberNode argA = parseEXP(s);
        if (argA == null)
            return null;

        require(",", "Missing ','", s);

        RobotNumberNode argB = parseEXP(s);
        if (argB == null)
            return null;

        require(CLOSEPAREN, "Missing ')'", s);

        final RobotNumberNode a = argA, b = argB;
        return new RobotConditionNode() {
            public boolean execute(Robot r) {
                return relop.execute(a.execute(r), b.execute(r));
            }

            public String toString() {
                return relop.toString() + "(" + a + ", " + b + ")";
            }
        };
    }

    private static RobotConditionNode buildCOND(Scanner s) {
        //Cuts down on repeated code
        String type = null;
        if (checkFor("and", s))
            type = "and";
        else if (checkFor("or", s))
            type = "or";

        if (type != null) {
            require(OPENPAREN, "Missing '('", s);

            RobotConditionNode argA = parseCOND(s);
            if (argA == null)
                return null;

            require(",", "Missing ','", s);

            RobotConditionNode argB = parseCOND(s);
            if (argB == null)
                return null;

            require(CLOSEPAREN, "Missing ')'", s);

            if (type.equals("and"))
                return new RobotConditionNode() {
                    public boolean execute(Robot r) {
                        return argA.execute(r) && argB.execute(r);
                    }

                    public String toString() {
                        return "(" + argA + " && " + argB + ")";
                    }
                };
            else
                return new RobotConditionNode() {
                    public boolean execute(Robot r) {
                        return argA.execute(r) || argB.execute(r);
                    }

                    public String toString() {
                        return "(" + argA + " || " + argB + ")";
                    }
                };
        }
        else if (checkFor("not", s)) {
            require(OPENPAREN, "Missing '('", s);

            RobotConditionNode cond = parseCOND(s);
            if (cond == null)
                return null;

            require(CLOSEPAREN, "Missing ')'", s);

            return new RobotConditionNode() {
                public boolean execute(Robot r) {
                    return !cond.execute(r);
                }

                public String toString() {
                    return "!(" + cond + ")";
                }
            };
        }

        return null;
    }

    private static RobotRelopNode parseRELOP(Scanner s) {
        if (checkFor("lt", s))
            return RobotRelopNode.LT;
        if (checkFor("gt", s))
            return RobotRelopNode.GT;
        if (checkFor("eq", s))
            return RobotRelopNode.EQ;

        return null;
    }

    private static RobotNumberNode parseSEN(Scanner s) {
        if (checkFor("barrelLR", s)) {
            //Optional argument
            if (checkFor(OPENPAREN, s)) {
                RobotNumberNode exp = parseEXP(s);
                require(CLOSEPAREN, "Missing ')'", s);

                return new RobotNumberNode() {
                    public int execute(Robot robot) {
                        return robot.getBarrelLR(exp.execute(robot));
                    }

                    public String toString() {
                        return "barrelLR(" + exp + ");\n";
                    }
                };
            }

            return RobotNumberNode.BARREL_LR;
        }
        if (checkFor("barrelFB", s)) {
            //Optional argument
            if (checkFor(OPENPAREN, s)) {
                RobotNumberNode exp = parseEXP(s);
                require(CLOSEPAREN, "Missing ')'", s);

                return new RobotNumberNode() {
                    public int execute(Robot robot) {
                        return robot.getBarrelFB(exp.execute(robot));
                    }

                    public String toString() {
                        return "barrelFB(" + exp + ");\n";
                    }
                };
            }

            return RobotNumberNode.BARREL_FB;
        }
        if (checkFor("fuelLeft", s))
            return RobotNumberNode.FUEL_LEFT;
        if (checkFor("oppLR", s))
            return RobotNumberNode.OPP_LR;
        if (checkFor("oppFB", s))
            return RobotNumberNode.OPP_FB;
        if (checkFor("numBarrels", s))
            return RobotNumberNode.NUM_BARRELS;
        if (checkFor("wallDist", s))
            return RobotNumberNode.WALL_DIST;

        return null;
    }

    private static RobotNumberNode parseVAR(Scanner s) {
        //If it looks like a variable
        if (s.hasNext(VARPAT)) {
            String name = s.next().substring(1);

            return new RobotNumberNode() {
                public int execute(Robot robot) {
                    Integer value = robot.getVariable(name);
                    //Kill the robot if tries to access a variable that doesn't exist
                    //Note: For me it was easier to implement existence checking at runtime than at parsing
                    if (value == null) {
                        robot.cancel();
                        throw new RuntimeException("$" + name + " has not yet been initialised or is out of scope");
                    }

                    return value;
                }

                public String toString() {
                    return "$" + name;
                }
            };
        }

        return null;
    }

    private static RobotNumberNode parseNUM(Scanner s) {
        int i = requireInt(NUMPAT, "Malformed number or variable", s);
        return new RobotNumberNode() {
            public int execute(Robot robot) {
                return i;
            }

            public String toString() {
                return Integer.toString(i);
            }
        };
    }

}

// You could add the node classes here, as long as they are not declared public (or private)
