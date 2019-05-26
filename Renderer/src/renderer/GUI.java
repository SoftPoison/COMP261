package renderer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;

/**
 * A simple GUI, similar to the one in assignments 1 and 2, that you can base
 * your renderer off. It is abstract, and there are three methods you need to
 * implement: onLoad, onKeyPress, and render. There is a method to get the
 * ambient light level set by the sliders. You are free to use this class as-is,
 * modify it, or ignore it completely.
 * 
 * @author tony
 */
public abstract class GUI {

	/**
	 * Is called when the user has successfully selected a model file to load,
	 * and is passed a File representing that file.
	 */
	protected abstract void onLoad(File file);

	/**
	 * Is called every time the user presses a key. This can be used for moving
	 * the camera around. It is passed a KeyEvent object, whose methods of
	 * interest are getKeyChar() and getKeyCode().
	 */
	protected abstract void onKeyPress(KeyEvent ev);

	/**
	 * Is called every time the drawing canvas is drawn. This should return a
	 * BufferedImage that is your render of the scene.
	 */
	protected abstract BufferedImage render();

	/**
	 * Forces a redraw of the drawing canvas. This is called for you, so you
	 * don't need to call this unless you modify this GUI.
	 */
	public void redraw() {
		frame.repaint();
	}

	/**
	 * Returns the values of the three sliders used for setting the ambient
	 * light of the scene. The returned array in the form [R, G, B] where each
	 * value is between 0 and 255.
	 */
	public int[] getAmbientLight() {
		return new int[] { red.getValue(), green.getValue(), blue.getValue() };
	}

	public Collection<Light> getLights() {
		return lights.values();
	}

	public static final int CANVAS_WIDTH = 600;
	public static final int CANVAS_HEIGHT = 600;

	private static final float DEG_TO_RAD = (float) (Math.PI / 180);
	private static final Vector3D DEFAULT_LIGHT_POS = new Vector3D(0, 0, -1);

	// --------------------------------------------------------------------
	// Everything below here is Swing-related and, while it's worth
	// understanding, you don't need to look any further to finish the
	// assignment up to and including completion.
	// --------------------------------------------------------------------

	private JFrame frame;
	JPanel controls = new JPanel();
	private final JSlider red = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
	private final JSlider green = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
	private final JSlider blue = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
	private final Component GLUE = Box.createVerticalGlue();

	private int lightID = 0;
	private Map<Integer, Light> lights = new HashMap<>();

	private static final Dimension DRAWING_SIZE = new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT);
	private static final Dimension CONTROLS_SIZE = new Dimension(150, 600);

	private static final Font FONT = new Font("Courier", Font.BOLD, 36);

	protected boolean showAxes = false;

	public GUI() {
		initialise();
	}

	@SuppressWarnings("serial")
	private void initialise() {
		// make the frame
		frame = new JFrame();
		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.LINE_AXIS));
		frame.setSize(new Dimension(DRAWING_SIZE.width + CONTROLS_SIZE.width, DRAWING_SIZE.height));
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// set up the drawing canvas, hook it into the render() method, and give
		// it a nice default if render() returns null.
		JComponent drawing = new JComponent() {
			protected void paintComponent(Graphics g) {
				BufferedImage image = render();
				if (image == null) {
					g.setColor(Color.WHITE);
					g.fillRect(0, 0, DRAWING_SIZE.width, DRAWING_SIZE.height);
					g.setColor(Color.BLACK);
					g.setFont(FONT);
					g.drawString("IMAGE IS NULL", 50, DRAWING_SIZE.height - 50);
				} else {
					g.drawImage(image, 0, 0, null);
				}
			}
		};
		// fix its size
		drawing.setPreferredSize(DRAWING_SIZE);
		drawing.setMinimumSize(DRAWING_SIZE);
		drawing.setMaximumSize(DRAWING_SIZE);
		drawing.setVisible(true);

		// set up the load button
		final JFileChooser fileChooser = new JFileChooser();
		JButton load = new JButton("Load");
		load.addActionListener(ev -> {
			// set up the file chooser
			fileChooser.setCurrentDirectory(new File("."));
			fileChooser.setDialogTitle("Select input file");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

			// run the file chooser and check the user didn't hit cancel
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				onLoad(file);
				redraw();
			}
		});
		// we have to put the button in its own panel to ensure it fills the
		// full width of the control bar.
		JPanel loadpanel = new JPanel(new BorderLayout());
		loadpanel.setMaximumSize(new Dimension(1000, 25));
		loadpanel.setPreferredSize(new Dimension(1000, 25));
		loadpanel.add(load, BorderLayout.CENTER);

		JButton axes = new JButton("Toggle Axes");
		axes.addActionListener(ev -> {
			showAxes = !showAxes;
			redraw();
		});
		JPanel axesPanel = new JPanel(new BorderLayout());
		axesPanel.setMaximumSize(new Dimension(1000, 25));
		axesPanel.setPreferredSize(new Dimension(1000, 25));
		axesPanel.add(axes, BorderLayout.CENTER);

		JButton addLight = new JButton("Add light");
		addLight.addActionListener(ev -> addLightPanel());
		JPanel lightPanel = new JPanel(new BorderLayout());
		lightPanel.setMaximumSize(new Dimension(1000, 25));
		lightPanel.setPreferredSize(new Dimension(1000, 25));
		lightPanel.add(addLight, BorderLayout.CENTER);

		// set up the sliders for ambient light. they were instantiated in
		// the field definition, as for some reason they need to be final to
		// pull the set background trick.
		red.setBackground(new Color(230, 50, 50));
		green.setBackground(new Color(50, 230, 50));
		blue.setBackground(new Color(50, 50, 230));

		red.addChangeListener(l -> redraw());
		green.addChangeListener(l -> redraw());
		blue.addChangeListener(l -> redraw());

		JPanel sliderparty = new JPanel();
		sliderparty.setLayout(new BoxLayout(sliderparty, BoxLayout.PAGE_AXIS));
		sliderparty.setBorder(BorderFactory.createTitledBorder("Ambient Light"));

		sliderparty.add(red);
		sliderparty.add(green);
		sliderparty.add(blue);

		// this is not a best-practices way of doing key listening; instead you
		// should use either a KeyListener or an InputMap/ActionMap combo. but
		// this method neatly avoids any focus issues (KeyListener) and requires
		// less effort on your part (ActionMap).
		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(ev -> {
			if (ev.getID() == KeyEvent.KEY_PRESSED) {
				onKeyPress(ev);
				redraw();
			}
			return true;
		});

		// make the panel on the right, fix its size, give it a border!
		controls.setPreferredSize(CONTROLS_SIZE);
		controls.setMinimumSize(CONTROLS_SIZE);
		controls.setMaximumSize(CONTROLS_SIZE);
		controls.setLayout(new BoxLayout(controls, BoxLayout.PAGE_AXIS));
		Border edge = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		controls.setBorder(edge);

		controls.add(loadpanel);
		controls.add(axesPanel);
		controls.add(lightPanel);
		controls.add(Box.createRigidArea(new Dimension(0, 15)));
		controls.add(sliderparty);
		controls.add(GLUE);

		// put it all together.
		frame.add(drawing);
		frame.add(controls);

		frame.pack();
		frame.setVisible(true);
	}

	private void angleChangeEvent(JSlider theta, JSlider phi, int id) {
		Light light = lights.get(id);
		Transform t = Transform.newXRotation(-phi.getValue() * DEG_TO_RAD).compose(Transform.newYRotation(-theta.getValue() * DEG_TO_RAD));

		lights.put(id, new Light(t.multiply(DEFAULT_LIGHT_POS), light.r, light.g, light.b));
		redraw();
	}

	private void addLightPanel() {
		final int id = lightID++;

		final JPanel lightPanel = new JPanel();
		lightPanel.setLayout(new BoxLayout(lightPanel, BoxLayout.PAGE_AXIS));
		lightPanel.setBorder(BorderFactory.createTitledBorder("Custom Light"));

		JButton remove = new JButton("Remove");
		remove.addActionListener(l -> {
			lights.remove(id);
			controls.remove(lightPanel);
			frame.validate();
			redraw();
		});

		JPanel removePanel = new JPanel(new BorderLayout());
		removePanel.setMaximumSize(new Dimension(1000, 25));
		removePanel.setPreferredSize(new Dimension(1000, 25));
		removePanel.add(remove, BorderLayout.CENTER);

		JSlider theta = new JSlider(JSlider.HORIZONTAL, -180, 180, 0);
		JSlider phi = new JSlider(JSlider.HORIZONTAL, -180, 180, 0);
		JSlider red = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
		JSlider green = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
		JSlider blue = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);

		red.setBackground(new Color(230, 50, 50));
		green.setBackground(new Color(50, 230, 50));
		blue.setBackground(new Color(50, 50, 230));
		theta.addChangeListener(l -> angleChangeEvent(theta, phi, id));
		phi.addChangeListener(l -> angleChangeEvent(theta, phi, id));
		red.addChangeListener(l -> {
			Light light = lights.get(id);
			lights.put(id, new Light(light.direction, red.getValue(), light.g, light.b));
			redraw();
		});
		green.addChangeListener(l -> {
			Light light = lights.get(id);
			lights.put(id, new Light(light.direction, light.r, green.getValue(), light.b));
			redraw();
		});
		blue.addChangeListener(l -> {
			Light light = lights.get(id);
			lights.put(id, new Light(light.direction, light.r, light.g, blue.getValue()));
			redraw();
		});

		lightPanel.add(removePanel);
		lightPanel.add(theta);
		lightPanel.add(phi);
		lightPanel.add(red);
		lightPanel.add(green);
		lightPanel.add(blue);

		controls.remove(GLUE);
		controls.add(lightPanel);
		controls.add(GLUE);

		lights.put(id, new Light(DEFAULT_LIGHT_POS, 128, 128, 128));

		frame.validate();
		redraw();
	}
}

// code for comp261 assignments
