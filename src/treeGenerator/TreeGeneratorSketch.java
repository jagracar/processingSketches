package treeGenerator;

import processing.core.PApplet;
import processing.core.PVector;

/**
 * Tree generator processing sketch
 * 
 * @author jagracar
 */
public class TreeGeneratorSketch extends PApplet {
	Branch tree;

	/**
	 * Sets the window size
	 */
	public void settings() {
		size(600, 450);
	}

	/**
	 * Initial sketch setup
	 */
	public void setup() {
		// Create the tree
		tree = createTree();

		// We only need to paint the tree once
		noLoop();
	}

	/**
	 * Draw method
	 */
	public void draw() {
		background(245);
		tree.paint();
	}

	/**
	 * Creates a new tree recursively
	 */
	Branch createTree() {
		PVector position = new PVector(0.5f * width, 0.95f * height);
		float length = height / 7.0f;
		float diameter = length / 4.5f;
		float angle = radians(random(-5, 5));
		float accumulatedAngle = 0;
		int color = color(130, 80, 20);
		int level = 1;
		return new Branch(this, position, length, diameter, angle, accumulatedAngle, color, level);
	}

	/**
	 * Paints a new tree each time the mouse is pressed
	 */
	public void mousePressed() {
		// Create a new tree
		tree = createTree();

		// Paint the tree
		redraw();
	}

	/**
	 * Executes the Processing sketch
	 * 
	 * @param args arguments to be passed to the sketch
	 */
	static public void main(String[] args) {
		String[] sketchName = new String[] { TreeGeneratorSketch.class.getName() };

		if (args != null) {
			PApplet.main(concat(sketchName, args));
		} else {
			PApplet.main(sketchName);
		}
	}
}
