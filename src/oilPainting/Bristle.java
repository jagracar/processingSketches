package oilPainting;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

/**
 * This class simulates the movement of a bristle
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class Bristle {
	private int nPositions;
	private PVector[] positions;
	private float[] lengths;
	private float[] thicknesses;

	/**
	 * Creates a new bristle object
	 * 
	 * @param nElements number of bristle elements
	 * @param initLength length of the first bristle element
	 * @param lengthDecrement length decrement in each successive element
	 * @param initThickness thickness of the first bristle element
	 * @param thicknessDecrement thickness decrement in each successive element
	 */
	public Bristle(int nElements, float initLength, float lengthDecrement, float initThickness,
			float thicknessDecrement) {
		nPositions = nElements + 1;
		positions = new PVector[nPositions];
		lengths = new float[nPositions];
		thicknesses = new float[nPositions];

		// Fill the arrays
		for (int i = 0; i < nPositions; i++) {
			positions[i] = new PVector(0, 0);
			lengths[i] = Math.max(1.0f, initLength - i * lengthDecrement);
			thicknesses[i] = Math.max(0.1f, initThickness - i * thicknessDecrement);
		}
	}

	/**
	 * Moves all the bristle elements to a new position
	 * 
	 * @param newPosition the new bristle elements position
	 */
	public void setPosition(PVector newPosition) {
		for (PVector p : positions) {
			p.set(newPosition.x, newPosition.y);
		}
	}

	/**
	 * Updates the bristle position
	 * 
	 * @param newPosition the new bristle position
	 */
	public void updatePosition(PVector newPosition) {
		PVector previousPos = positions[0].set(newPosition.x, newPosition.y);

		for (int i = 1; i < nPositions; i++) {
			PVector pos = positions[i];
			float ang = PApplet.atan2(previousPos.y - pos.y, previousPos.x - pos.x);
			previousPos = pos.set(previousPos.x - lengths[i] * PApplet.cos(ang),
					previousPos.y - lengths[i] * PApplet.sin(ang));
		}
	}

	/**
	 * Paints the bristle on the screen and the canvas
	 * 
	 * @param col the color to use
	 * @param applet the sketch applet
	 * @param canvas the canvas buffer
	 * @param paintCanvas true if the bristle should be painted on the canvas buffer
	 */
	public void paint(int col, PApplet applet, PGraphics canvas, boolean paintCanvas) {
		// Set the stroke color
		applet.stroke(col);

		if (paintCanvas) {
			// Set the alpha value to 255.
			// This speeds up the drawing calculations
			canvas.stroke(col | 0xff000000);
		}

		// Paint the bristle elements
		PVector previousPos = positions[0];

		for (int i = 1; i < nPositions; i++) {
			float thickness = thicknesses[i];
			PVector pos = positions[i];

			applet.strokeWeight(thickness);
			applet.line(previousPos.x, previousPos.y, pos.x, pos.y);

			if (paintCanvas) {
				canvas.strokeWeight(thickness);
				canvas.line(previousPos.x, previousPos.y, pos.x, pos.y);
			}

			previousPos = pos;
		}
	}
}
