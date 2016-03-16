package drawingBalls;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * Drawing balls processing sketch
 * 
 * @author jagracar
 */
public class DrawingBallsSketch extends PApplet {
	PImage img;
	int maxBalls;
	ArrayList<Ball> balls = new ArrayList<Ball>();

	/**
	 * Sets the default window size
	 */
	public void settings() {
		size(100, 100);
	}

	/**
	 * Initial sketch setup
	 */
	public void setup() {
		// Make the window resizable
		surface.setResizable(true);

		// Load the picture by Sukanto Debnath
		// https://www.flickr.com/photos/sukanto_debnath/3081836966/
		// img = loadImage("http://farm4.staticflickr.com/3137/3081836966_7945315150.jpg");
		img = loadImage("src/drawingBalls/picture.jpg");

		// Load the image pixels to be able to use them later
		img.loadPixels();

		// Resize the window to the image size
		surface.setSize(img.width, img.height);

		// Calculate the maximum number of balls
		maxBalls = (int) (2.5 * Math.sqrt(img.width * img.height));

		// Drawing options
		noStroke();
		ellipseMode(CENTER);
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Clean the window
		background(0);

		// Add a new ball in each frame up to certain limit
		if (balls.size() < maxBalls) {
			float r = random(30);
			float alpha = random(TWO_PI);
			PVector pos = new PVector(0.55f * width + r * cos(alpha), 0.4f * height + r * sin(alpha));
			PVector vel = new PVector(0, 0);
			balls.add(new Ball(this, pos, vel));
		}

		// Update the balls positions
		for (Ball ball : balls) {
			ball.update(img);
		}

		// Check if the balls are in contact and move them in that case
		for (int i = 0; i < balls.size(); i++) {
			for (int j = 0; j < balls.size(); j++) {
				if (j != i) {
					balls.get(i).checkContact(balls.get(j));
				}
			}
		}

		// Paint the balls in the canvas
		for (Ball ball : balls) {
			ball.paint();
		}
	}

	/**
	 * Applies a force to the balls each time the mouse is pressed
	 */
	public void mousePressed() {
		for (Ball ball : balls) {
			ball.applyForce(mouseX, mouseY);
		}
	}

	/**
	 * Executes the Processing sketch
	 * 
	 * @param args arguments to be passed to the sketch
	 */
	static public void main(String[] args) {
		String[] sketchName = new String[] { DrawingBallsSketch.class.getName() };

		if (args != null) {
			PApplet.main(concat(sketchName, args));
		} else {
			PApplet.main(sketchName);
		}
	}
}
