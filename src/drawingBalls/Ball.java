package drawingBalls;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * The ball class
 * 
 * @author jagracar
 */
public class Ball {
	PApplet applet;
	PVector pos;
	PVector vel;
	int col;
	float rad;

	/**
	 * Constructor
	 * 
	 * @param applet the sketch applet
	 * @param initPos the ball initial position
	 * @param initVel the ball initial velocity
	 */
	Ball(PApplet applet, PVector initPos, PVector initVel) {
		this.applet = applet;
		this.pos = initPos.copy();
		this.vel = initVel.copy();
		this.col = applet.color(0);
		this.rad = 3;
	}

	/**
	 * Updates the ball position, color and radius
	 * 
	 * @param img the background image (assumes that the image pixels are already loaded)
	 */
	public void update(PImage img) {
		// Calculate the new ball position and velocity
		pos.add(vel);
		vel.mult(0.999f);

		// Check that the ball is not leaving the screen
		if (pos.x > img.width - rad) {
			pos.x = img.width - rad;
			vel.x = -vel.x;
		} else if (pos.x < rad) {
			pos.x = rad;
			vel.x = -vel.x;
		}

		if (pos.y > img.height - rad) {
			pos.y = img.height - rad;
			vel.y = -vel.y;
		} else if (pos.y < rad) {
			pos.y = rad;
			vel.y = -vel.y;
		}

		// Calculate the new ball color and radius
		col = img.pixels[Math.round(pos.x) + Math.round(pos.y) * img.width];
		rad = PApplet.map(applet.brightness(col), 0, 255, 3, 7);
	}

	/**
	 * Paints the ball on the screen
	 */
	public void paint() {
		applet.fill(col);
		applet.ellipse(pos.x, pos.y, (2 * rad) - 1, (2 * rad) - 1);
	}

	/**
	 * Checks if the ball is in contact with another ball, and, if that is the case, changes their positions and
	 * velocities
	 * 
	 * @param b the ball to check
	 */
	public void checkContact(Ball b) {
		// Obtain the positions difference vector
		float xDiff = b.pos.x - pos.x;
		float yDiff = b.pos.y - pos.y;
		float diffSq = PApplet.sq(xDiff) + PApplet.sq(yDiff);

		// Check if the balls are in contact
		if (diffSq < PApplet.sq(rad + b.rad)) {
			// Calculate the center of gravity weighted by the ball radius
			float weight = rad / (rad + b.rad);
			float xCenter = pos.x + xDiff * weight;
			float yCenter = pos.y + yDiff * weight;

			// Normalize the positions difference vector
			float diff = PApplet.sqrt(diffSq);
			xDiff /= diff;
			yDiff /= diff;

			// Update the balls positions
			pos.set(xCenter - rad * xDiff, yCenter - rad * yDiff);
			b.pos.set(xCenter + b.rad * xDiff, yCenter + b.rad * yDiff);

			// Set the velocities to zero
			vel.set(0, 0);
			b.vel.set(0, 0);
		}
	}

	/**
	 * Applies a force to the ball
	 * 
	 * @param x the force x center
	 * @param y the force y center
	 */
	public void applyForce(float x, float y) {
		// Obtain the positions difference vector
		float xDiff = pos.x - x;
		float yDiff = pos.y - y;
		float diffSq = PApplet.sq(xDiff) + PApplet.sq(yDiff);

		if (diffSq < PApplet.sq(rad)) {
			// Make sure we don't divide by zero
			if (diffSq > 0) {
				float diff = PApplet.sqrt(diffSq);
				vel.add(15 * xDiff / diff, 15 * yDiff / diff);
			}
		} else if (diffSq < 1000) {
			float diff = PApplet.sqrt(diffSq);
			vel.add(15 * PApplet.sq(rad / diff) * xDiff / diff, 15 * PApplet.sq(rad / diff) * yDiff / diff);
		}
	}
}
