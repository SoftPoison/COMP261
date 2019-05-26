import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a template GUI that you can use for your mapping program. It is an
 * *abstract class*, which means you'll need to extend it in your own program.
 * For a simple example of how to do this, have a look at the SquaresExample
 * class.
 * <p>
 * This GUI uses Swing, not the first-year UI library. Swing is not the focus of
 * this course, but it would be to your benefit if you took some time to
 * understand how this class works.
 *
 * @author tony
 */
public abstract class GUI {
    /**
     * defines the different types of movement the user can perform, the
     * appropriate one is passed to your code when the move(Move) method is
     * called.
     */
    public enum Move {
        NORTH, SOUTH, EAST, WEST, ZOOM_IN, ZOOM_OUT
    }

    // these are the methods you need to implement.

    /**
     * Is called when the drawing area is redrawn and performs all the logic for
     * the actual drawing, which is done with the passed Graphics object.
     */
    protected abstract void redraw(Graphics g);

    /**
     * Is called when the mouse is clicked (actually, when the mouse is
     * released), and is passed the MouseEvent object for that click.
     */
    protected abstract void onClick(MouseEvent e);

    protected abstract void onDrag(MouseEvent e);

    /**
     * Is called whenever the search box is updated. Use getSearchBox to get the
     * JTextField object that is the search box itself.
     */
    protected abstract List<String> onSearch(String text);

    /**
     * Is called whenever a navigation button is pressed. An instance of the
     * Move enum is passed, representing the button clicked by the user.
     */
    protected abstract void onMove(Move m);

    protected abstract void onArtPtsButton();

    protected abstract void onAStarModeChange();

    protected abstract void onScroll(MouseWheelEvent e);

    /**
     * Is called when the user has successfully selected a directory to load the
     * data files from. File objects representing the four files of interested
     * are passed to the method. The fourth File, polygons, might be null if it
     * isn't present in the directory.
     *
     * @param nodes         a File for nodeID-lat-lon.tab
     * @param roads         a File for roadID-roadInfo.tab
     * @param segments      a File for roadSeg-roadID-length-nodeID-nodeID-coords.tab
     * @param polygons      a File for polygon-shapes.mp
     * @param restrictions  a File for restrictions.tab
     * @param trafficLights a File for traffic-lights.tab
     */
    protected abstract void onLoad(File nodes, File roads, File segments, File polygons, File restrictions, File trafficLights);

    // here are some useful methods you'll need.

    /**
     * @return the JTextArea at the bottom of the screen for output.
     */
    public JTextArea getTextOutputArea() {
        return textOutputArea;
    }

    /**
     * @return the dimensions of the drawing area.
     */
    public Dimension getDrawingAreaDimension() {
        return drawing.getSize();
    }

    /**
     * Redraws the window (including drawing pane). This is already done
     * whenever a button is pressed or the search box is updated, so you
     * probably won't need to call this.
     */
    public void redraw() {
        frame.repaint();
    }

    // --------------------------------------------------------------------
    // Everything below here is Swing-related and, while it's worth
    // understanding, you don't need to look any further to finish the
    // assignment up to and including completion.
    // --------------------------------------------------------------------

    private static final int DEFAULT_DRAWING_HEIGHT = 400;
    private static final int DEFAULT_DRAWING_WIDTH = 400;
    private static final int TEXT_OUTPUT_ROWS = 5;

    private static final String NODES_FILENAME = "nodeID-lat-lon.tab";
    private static final String ROADS_FILENAME = "roadID-roadInfo.tab";
    private static final String SEGS_FILENAME = "roadSeg-roadID-length-nodeID-nodeID-coords.tab";
    private static final String POLYS_FILENAME = "polygon-shapes.mp";
    private static final String RESTRICTIONS_FILENAME = "restrictions.tab";
    private static final String TRAFFIC_FILENAME = "traffic-lights.tab";

    /*
     * In Swing, everything is a component; buttons, graphics panes, tool tips,
     * and the window frame are all components. This is implemented by
     * JComponent, which sits at the top of the component inheritance hierarchy.
     * A JFrame is a component that represents the outer window frame (with the
     * minimise, maximise, and close buttons) of your program. Every swing
     * program has to have one somewhere. JFrames can, of course, have other
     * components inside them. JPanels are your bog-standard container component
     * (can have other components inside them), that are used for laying out
     * your UI.
     */

    private JFrame frame;

    private JPanel controls;
    private JComponent drawing; // we customise this to make it a drawing pane.
    private JTextArea textOutputArea;

    private JTextPane search;
    private StyledDocument document;
    private JFileChooser fileChooser;

    protected boolean isSpeedHeuristic = false;
    protected boolean useTrafficLights = false;

    private final AttributeSet defaultAset = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLACK);
    private final AttributeSet suggestionAset = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.LIGHT_GRAY);

    private LinkedList<Character> searchText = new LinkedList<>();
    private int suggestionIndex = 0;
    private List<String> suggestions = new ArrayList<>();

    public GUI() {
        initialise();
    }

    /**
     * Converts the character list (searchText) into a String
     */
    private String searchTextToString() {
        StringBuilder sb = new StringBuilder();
        searchText.forEach(sb::append);
        return sb.toString();
    }

    /**
     * Sets text in the search box to the current buffer, and writes the given suggestion after it
     *
     * @param suggestion the suggestion to display
     * @param caretPos   where to put the caret. Negative or overflow values place caret at end of suggestion
     */
    private void setSearchBoxText(String suggestion, int caretPos) {
        if (caretPos < 0)
            caretPos = searchText.size();
        else if (caretPos > searchText.size())
            caretPos = searchText.size();

        search.setText("");
        try {
            if (suggestion != null) //If there's no suggestion, don't display any
                document.insertString(0, suggestion.substring(searchText.size()), suggestionAset);

            document.insertString(0, searchTextToString(), defaultAset);
            search.setCaretPosition(caretPos);

        }
        catch (BadLocationException ignored) { }
    }

    @SuppressWarnings("serial")
    private void initialise() {

        /*
         * first, we make the buttons etc. that go along the top bar.
         */

        // action listeners give you a hook to perform when the button is
        // pressed. the horrible thing being passed to addActionListener is an
        // anonymous class, covered in SWEN221. these are useful when working
        // with swing. the quit button isn't really necessary, as you can just
        // press the frame's close button, but it serves as a nice example.
        JButton quit = new JButton("Quit");
        quit.addActionListener(ev -> {
            System.exit(0); // cleanly end the program.
        });

        fileChooser = new JFileChooser();
        JButton load = new JButton("Load");
        load.addActionListener(ev -> {
            File nodes = null, roads = null, segments = null, polygons = null, restrictions = null, trafficLights = null;

            // set up the file chooser
            fileChooser.setCurrentDirectory(new File("."));
            fileChooser.setDialogTitle("Select input directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            // run the file chooser and check the user didn't hit cancel
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                // get the files in the selected directory and match them to
                // the files we need.
                File directory = fileChooser.getSelectedFile();
                File[] files = directory.listFiles();

                for (File f : files) {
                    if (f.getName().equals(NODES_FILENAME)) {
                        nodes = f;
                    }
                    else if (f.getName().equals(ROADS_FILENAME)) {
                        roads = f;
                    }
                    else if (f.getName().equals(SEGS_FILENAME)) {
                        segments = f;
                    }
                    else if (f.getName().equals(POLYS_FILENAME)) {
                        polygons = f;
                    }
                    else if (f.getName().equals(RESTRICTIONS_FILENAME)) {
                        restrictions = f;
                    }
                    else if (f.getName().equals(TRAFFIC_FILENAME)) {
                        trafficLights = f;
                    }
                }

                // check none of the files are missing, and call the load
                // method in your code.
                if (nodes == null || roads == null || segments == null) {
                    JOptionPane.showMessageDialog(frame,
                            "Directory does not contain correct files",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    onLoad(nodes, roads, segments, polygons, restrictions, trafficLights);
                    redraw();
                }
            }
        });

        JButton west = new JButton("\u2190");
        west.addActionListener(ev -> {
            onMove(Move.WEST);
            redraw();
        });

        JButton east = new JButton("\u2192");
        east.addActionListener(ev -> {
            onMove(Move.EAST);
            redraw();
        });

        JButton north = new JButton("\u2191");
        north.addActionListener(ev -> {
            onMove(Move.NORTH);
            redraw();
        });

        JButton south = new JButton("\u2193");
        south.addActionListener(ev -> {
            onMove(Move.SOUTH);
            redraw();
        });

        JButton in = new JButton("+");
        in.addActionListener(ev -> {
            onMove(Move.ZOOM_IN);
            redraw();
        });

        JButton out = new JButton("\u2012");
        out.addActionListener(ev -> {
            onMove(Move.ZOOM_OUT);
            redraw();
        });

        //Custom buttons
        JButton artPts = new JButton("Find Articulation Points");
        artPts.addActionListener(ev -> {
            onArtPtsButton();
            redraw();
        });

        JButton heuristic = new JButton("Distance Mode");
        heuristic.addActionListener(ev -> {
            isSpeedHeuristic = !isSpeedHeuristic;
            heuristic.setText(isSpeedHeuristic ? "Speed Mode" : "Distance Mode");
            onAStarModeChange();
            redraw();
        });

        JButton trafficLights = new JButton("Ignore Traffic Lights");
        trafficLights.addActionListener(ev -> {
            useTrafficLights = !useTrafficLights;
            trafficLights.setText(useTrafficLights ? "Avoid Traffic Lights" : "Ignore Traffic Lights");
            onAStarModeChange();
            redraw();
        });

        //Changed the search box to use JTextPane as it has multicolour text support
        //The search box is also modified to have custom input handling
        search = new JTextPane() {
            //https://stackoverflow.com/questions/24931965/how-to-make-jtextpane-scroll-horizontally
            //This fixes the pane from dropping down a linea
            public boolean getScrollableTracksViewportWidth() {
                return getUI().getPreferredSize(this).width <= getParent().getSize().width;
            }
        };
        document = search.getStyledDocument();
        search.addKeyListener(new KeyAdapter() {
            //"Control" keys
            public void keyPressed(KeyEvent e) {
                e.consume(); //"Consume" the keypress to prevent default actions

                int caretPos;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        caretPos = search.getCaretPosition();
                        if (caretPos > 0)
                            search.setCaretPosition(caretPos - 1); //Move the caret left
                        return;
                    case KeyEvent.VK_RIGHT:
                        caretPos = search.getCaretPosition();
                        if (caretPos < searchText.size())
                            search.setCaretPosition(caretPos + 1); //Move the caret right
                        return;
                    case KeyEvent.VK_UP:
                        if (--suggestionIndex < 0) //Cycle backwards through the suggestions
                            suggestionIndex = suggestions.size() - 1; //Loop back around to the end of the list
                        break;
                    case KeyEvent.VK_DOWN:
                        if (++suggestionIndex >= suggestions.size()) //Cycle forwards through the suggestions
                            suggestionIndex = 0; //Loop back around to the start of the list
                        break;
                    default:
                        return;
                }

                if (suggestions.size() == 0)
                    return;

                //The search box is only re-rendered if there are
                setSearchBoxText(suggestions.get(suggestionIndex), -1);
            }

            //Letters/numbers/"characters"
            public void keyTyped(KeyEvent e) {
                e.consume(); //"Consume" the keypress to prevent default actions

                //We only want to handle displayable (and certain special) characters
                if ((e.getKeyChar() < 8 || e.getKeyChar() >= 10) && e.getKeyChar() < 32)
                    return;

                int caretPos = search.getCaretPosition();
                if (caretPos > searchText.size())
                    caretPos = searchText.size();

                switch (e.getKeyChar()) {
                    case KeyEvent.VK_BACK_SPACE:
                        if (caretPos > 0) {
                            if (e.isControlDown()) { //Want to delete backwards up to space
                                do {
                                    searchText.remove(--caretPos);
                                }
                                while (caretPos > 0 && searchText.get(caretPos - 1) != ' ');
                            }
                            else { //Just want to remove one char to the left
                                searchText.remove(--caretPos);
                            }
                            search.setCaretPosition(caretPos);
                        }
                        break;
                    case KeyEvent.VK_DELETE:
                        if (caretPos < searchText.size() - 1) {
                            if (e.isControlDown()) { //Want to delete forwards up to space
                                do {
                                    searchText.remove(caretPos);
                                }
                                while (searchText.size() > caretPos && searchText.get(caretPos) != ' ');
                            }
                            else { //Just want to remove one char to the right
                                searchText.remove(caretPos);
                            }
                        }
                        break;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_TAB:
                        //Insert the currently showing suggestion into the search box

                        if (suggestions.size() == 0)
                            break;

                        searchText.clear();
                        for (char c : suggestions.get(suggestionIndex).toCharArray())
                            searchText.add(c); //Can't use addAll because it's a char array, so have to do it manually

                        caretPos = -1;

                        break;
                    default:
                        searchText.add(caretPos++, e.getKeyChar());
                        break;
                }

                suggestions = onSearch(searchTextToString());
                suggestionIndex = 0;
                setSearchBoxText(suggestions.size() > 0 ? suggestions.get(0) : null, caretPos);
                redraw();
            }
        });

        /*
         * next, make the top bar itself and arrange everything inside of it.
         */

        // almost any component (JPanel, JFrame, etc.) that contains other
        // components inside it needs a LayoutManager to be useful, these do
        // exactly what you expect. three common LayoutManagers are the BoxLayout,
        // GridLayout, and BorderLayout. BoxLayout, contrary to its name, places
        // components in either a row (LINE_AXIS) or a column (PAGE_AXIS).
        // GridLayout is self-describing. BorderLayout puts a single component
        // on the north, south, east, and west sides of the outer component, as
        // well as one in the centre. google for more information.
        controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));

        // make an empty border so the components aren't right up against the
        // frame edge.
        Border edge = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        controls.setBorder(edge);

        JPanel loadquit = new JPanel();
        loadquit.setLayout(new GridLayout(2, 1));
        // manually set a fixed size for the panel containing the load and quit
        // buttons (doesn't change with window resize).
        loadquit.setMaximumSize(new Dimension(50, 100));
        loadquit.add(load);
        loadquit.add(quit);
        controls.add(loadquit);
        // rigid areas are invisible components that can be used to space
        // components out.
        controls.add(Box.createRigidArea(new Dimension(15, 0)));

        JPanel navigation = new JPanel();
        navigation.setMaximumSize(new Dimension(150, 60));
        navigation.setLayout(new GridLayout(2, 3));
        navigation.add(out);
        navigation.add(north);
        navigation.add(in);
        navigation.add(west);
        navigation.add(south);
        navigation.add(east);
        controls.add(navigation);
        controls.add(Box.createRigidArea(new Dimension(15, 0)));

        JPanel buttons = new JPanel();
        buttons.setMaximumSize(new Dimension(300, 60));
        buttons.setLayout(new GridLayout(2, 2));
        buttons.add(artPts);
        buttons.add(heuristic);
        buttons.add(trafficLights);
        controls.add(buttons);
        controls.add(Box.createRigidArea(new Dimension(15, 0)));

        // glue is another invisible component that grows to take up all the
        // space it can on resize.
        controls.add(Box.createHorizontalGlue());

        //Wrap the search box in a scroll pane so that it behaves properly
        JScrollPane searchScroll = new JScrollPane(search);
        searchScroll.setPreferredSize(new Dimension(200, 25));
        searchScroll.setMaximumSize(new Dimension(0, 25)); //Necessary so it stays the right height
        //Make sure the scroll bars never show
        searchScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        searchScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        controls.add(new JLabel("Search"));
        controls.add(Box.createRigidArea(new Dimension(5, 0)));
        controls.add(searchScroll);

        /*
         * then make the drawing canvas, which is really just a boring old
         * JComponent with the paintComponent method overridden to paint
         * whatever we like. this is the easiest way to do drawing.
         */

        drawing = new JComponent() {
            protected void paintComponent(Graphics g) {
                redraw(g);
            }
        };
        drawing.setPreferredSize(new Dimension(DEFAULT_DRAWING_WIDTH,
                DEFAULT_DRAWING_HEIGHT));
        // this prevents a bug where the component won't be
        // drawn until it is resized.
        drawing.setVisible(true);
        drawing.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                onDrag(e);
                redraw();
            }
        });

        drawing.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                onClick(e);
                redraw();
            }
        });

        drawing.addMouseWheelListener(new MouseAdapter() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                onScroll(e);
                redraw();
            }
        });

        /*
         * then make the JTextArea that goes down the bottom. we put this in a
         * JScrollPane to get scroll bars when necessary.
         */

        textOutputArea = new JTextArea(TEXT_OUTPUT_ROWS, 0);
        textOutputArea.setLineWrap(true);
        textOutputArea.setWrapStyleWord(true); // pretty line wrap.
        textOutputArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(textOutputArea);
        // these two lines make the JScrollPane always scroll to the bottom when
        // text is appended to the JTextArea.
        DefaultCaret caret = (DefaultCaret) textOutputArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        /*
         * finally, make the outer JFrame and put it all together. this is more
         * complicated than it could be, as we put the drawing and text output
         * components inside a JSplitPane so they can be resized by the user.
         * the JScrollPane and the top bar are then added to the frame.
         */

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setDividerSize(5); // make the selectable area smaller
        split.setContinuousLayout(true); // make the panes resize nicely
        split.setResizeWeight(1); // always give extra space to drawings
        // JSplitPanes have a default border that makes an ugly row of pixels at
        // the top, remove it.
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setTopComponent(drawing);
        split.setBottomComponent(scroll);

        frame = new JFrame("Mapper");
        // this makes the program actually quit when the frame's close button is
        // pressed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(controls, BorderLayout.NORTH);
        frame.add(split, BorderLayout.CENTER);

        // always do these two things last, in this order.
        frame.pack();
        frame.setVisible(true);
    }
}

// code for COMP261 assignments
