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
	 * @param nElements the total number of bristle elements
	 * @param thickness the thickness of the first bristle element
	 */
	public Bristle(int nElements, float thickness) {
		nPositions = nElements + 1;
		positions = new PVector[nPositions];
		lengths = new float[nPositions];
		thicknesses = new float[nPositions];

		// Fill the arrays
		float thicknessDecrement = thickness / nElements;

		for (int i = 0; i < nPositions; i++) {
			positions[i] = new PVector(0, 0);
			lengths[i] = nPositions - i;
			thicknesses[i] = thickness - (i - 1) * thicknessDecrement;
		}
	}

	/**
	 * Moves all the bristle elements to a new position
	 * 
	 * @param newPosition the new bristle elements position
	 */
	public void setPosition(PVector newPosition) {
		for (PVector pos : positions) {
			pos.set(newPosition.x, newPosition.y);
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
			float length = lengths[i];
			float ang = PApplet.atan2(previousPos.y - pos.y, previousPos.x - pos.x);
			previousPos = pos.set(previousPos.x - length * PApplet.cos(ang), previousPos.y - length * PApplet.sin(ang));
		}
	}

	/**
	 * Paints the bristle on the canvas buffer. Note that the canvas.beginDraw() method should have been called before
	 * 
	 * @param col the color to use
	 * @param canvas the canvas buffer
	 */
	public void paintOnCanvas(int col, PGraphics canvas) {
		// Set the stroke color
		canvas.stroke(col);

		// Paint the bristle elements
		PVector previousPos = positions[0];

		for (int i = 1; i < nPositions; i++) {
			PVector pos = positions[i];

			canvas.strokeWeight(thicknesses[i]);
			canvas.line(previousPos.x, previousPos.y, pos.x, pos.y);

			previousPos = pos;
		}
	}

	/**
	 * Paints the bristle on the screen
	 * 
	 * @param col the color to use
	 * @param applet the sketch applet
	 */
	public void paintOnScreen(int col, PApplet applet) {
		// Set the stroke color
		applet.stroke(col);

		// Paint the bristle elements
		PVector previousPos = positions[0];

		for (int i = 1; i < nPositions; i++) {
			PVector pos = positions[i];

			applet.strokeWeight(thicknesses[i]);
			applet.line(previousPos.x, previousPos.y, pos.x, pos.y);

			previousPos = pos;
		}
	}
}
