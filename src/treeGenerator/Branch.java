package treeGenerator;

import processing.core.PApplet;
import processing.core.PVector;

/**
 * The branch class
 * 
 * @author jagracar
 */
public class Branch {
	// Maximum number of recursion levels
	static final int MAXLEVELS = 16;

	// Branch properties
	PApplet applet;
	PVector position;
	float length;
	float diameter;
	float angle;
	float accumulatedAngle;
	int color;
	int level;
	Branch middleBranch;
	Branch extremeBranch;

	/**
	 * Constructor
	 * 
	 * @param applet the sketch applet
	 * @param position the branch bottom position
	 * @param length the branch length
	 * @param diameter the branch diameter
	 * @param angle the branch relative inclination angle
	 * @param accumulatedAngle the accumulated inclination angle from previous recursion levels
	 * @param color the branch color
	 * @param level the branch recursive level
	 */
	public Branch(PApplet applet, PVector position, float length, float diameter, float angle, float accumulatedAngle,
			int color, int level) {
		this.applet = applet;
		this.position = position;
		this.length = length;
		this.diameter = diameter;
		this.angle = angle;
		this.accumulatedAngle = accumulatedAngle;
		this.color = color;
		this.level = level;

		// Create the sub branches
		middleBranch = createSubBranch(true);
		extremeBranch = createSubBranch(false);
	}

	/**
	 * Paints the branch and sub branches on the screen
	 */
	public void paint() {
		// Calculate the diameter at the branch top
		float topDiameter = (extremeBranch != null) ? extremeBranch.diameter : 0.65f * diameter;

		// Apply the coordinate transformations
		applet.pushMatrix();
		applet.translate(position.x, position.y);
		applet.rotate(angle);

		// Paint the middle branch
		if (middleBranch != null) {
			middleBranch.paint();
		}

		// Paint the extreme branch
		if (extremeBranch != null) {
			extremeBranch.paint();
		}

		// Paint the branch
		applet.pushStyle();
		applet.noStroke();
		applet.fill(color);
		applet.beginShape();
		applet.vertex(-diameter / 2, 0);
		applet.vertex(-topDiameter / 2, -1.04f * length);
		applet.vertex(topDiameter / 2, -1.04f * length);
		applet.vertex(diameter / 2, 0);
		applet.endShape();
		applet.popStyle();

		applet.popMatrix();
	}

	/**
	 * Creates a sub branch
	 * 
	 * @param isMiddleBranch indicates is the sub branch should be a middle branch
	 * @return the sub branch
	 */
	private Branch createSubBranch(boolean isMiddleBranch) {
		// Decide if the branch should be created
		boolean createBranch = false;

		if (isMiddleBranch) {
			if (level < 4 && applet.random(1) < 0.7f) {
				createBranch = true;
			} else if (level >= 4 && level < 10 && applet.random(1) < 0.8f) {
				createBranch = true;
			} else if (level >= 10 && level < MAXLEVELS && applet.random(1) < 0.85f) {
				createBranch = true;
			}
		} else {
			if (level == 1) {
				createBranch = true;
			} else if (level < MAXLEVELS && applet.random(1) < 0.85f) {
				createBranch = true;
			}
		}

		if (createBranch) {
			// Calculate the sub branch relative position
			PVector newPosition = new PVector(0, isMiddleBranch ? -applet.random(0.5f, 0.9f) * length : -length);

			// Calculate the length
			float newLength = applet.random(0.8f, 0.9f) * length;

			// Calculate the diameter
			float newDiameter = (level < 5) ? applet.random(0.8f, 0.9f) * diameter
					: applet.random(0.65f, 0.75f) * diameter;

			// Calculate the relative inclination angle
			float newAngle;

			if (isMiddleBranch) {
				float sign = (applet.random(1) < 0.5) ? -1 : 1;
				newAngle = sign * PApplet.radians(applet.random(20, 40));
			} else {
				newAngle = PApplet.radians(applet.random(-15, 15));
			}

			// Don't let the branches fall too much
			if (level < 8 && (PApplet.abs(accumulatedAngle + angle + newAngle) > 0.9 * PApplet.HALF_PI)) {
				newAngle *= -1;
			}

			// Calculate the color of the new branch
			int newColor;

			if (newDiameter > 1) {
				float deltaColor = applet.random(0, 10);
				newColor = applet.color(applet.red(color) + deltaColor, applet.green(color) + deltaColor,
						applet.blue(color));
			} else {
				newColor = applet.color(0.75f * applet.red(color), applet.green(color), 0.85f * applet.blue(color));
			}

			// Calculate the new branch level
			int newLevel = level + 1;

			if (level < 6 && applet.random(1) < 0.3) {
				newLevel++;
			}

			// Return the new branch
			return new Branch(applet, newPosition, newLength, newDiameter, newAngle, accumulatedAngle + angle, newColor,
					newLevel);
		} else {
			// Return null
			return null;
		}
	}
}
