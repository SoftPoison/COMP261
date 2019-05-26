package renderer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * EdgeList should store the data for the edge list of a single polygon in your
 * scene. A few method stubs have been provided so that it can be tested, but
 * you'll need to fill in all the details.
 *
 * You'll probably want to add some setters as well as getters or, for example,
 * an addRow(y, xLeft, xRight, zLeft, zRight) method.
 */
public class EdgeList {
	private final int startY;
	private final int endY;

	private Map<Integer, Float> leftXMap = new HashMap<>();
	private Map<Integer, Float> rightXMap = new HashMap<>();
	private Map<Integer, Float> leftZMap = new HashMap<>();
	private Map<Integer, Float> rightZMap = new HashMap<>();
	private Map<Integer, Color> leftColorMap = new HashMap<>();
	private Map<Integer, Color> rightColorMap = new HashMap<>();

	public EdgeList(int startY, int endY) {
		this.startY = startY;
		this.endY = endY;
	}

	public int getStartY() {
		return startY;
	}

	public int getEndY() {
		return endY;
	}

	public void add(int y, float x, float z, Color color, boolean left) {
		if (left) {
			leftXMap.put(y, x);
			leftZMap.put(y, z);
			leftColorMap.put(y, color);
		}
		else {
			rightXMap.put(y, x);
			rightZMap.put(y, z);
			rightColorMap.put(y, color);
		}
	}

	public float getLeftX(int y) {
		return leftXMap.get(y);
	}

	public float getRightX(int y) {
		return rightXMap.get(y);
	}

	public float getLeftZ(int y) {
		return leftZMap.get(y);
	}

	public float getRightZ(int y) {
		return rightZMap.get(y);
	}

	public Color getLeftColor(int y) {
		return leftColorMap.get(y);
	}

	public Color getRightColor(int y) {
		return rightColorMap.get(y);
	}
}

// code for comp261 assignments
