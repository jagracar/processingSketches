package oilPainting;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

/**
 * This class simulates a brush composed of several bristles
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class Brush {

	/**
	 * The number of positions to use to calculate the average position
	 */
	private static final int POSITIONS_FOR_AVERAGE = 4;

	/**
	 * The noise range to add to the bristles vertical position on the brush
	 */
	private static final float BRISTLE_VERTICAL_NOISE = 8;

	/**
	 * The maximum noise range to add in each update to the bristles horizontal position on the brush
	 */
	private static final float MAX_BRISTLE_HORIZONTAL_NOISE = 4;

	/**
	 * Controls the bristles horizontal noise speed
	 */
	private static final float NOISE_SPEED_FACTOR = 0.04f;

	private PApplet applet;
	private PVector position;
	private int nBristles;
	private Bristle[] bristles;
	private PVector[] bOffsets;
	private PVector[] bPositions;
	private ArrayList<PVector> positionsHistory;
	private PVector averagePosition;
	private float noiseSeed;
	private int updatesCounter;
	private float bristleHorizontalNoise;

	/**
	 * Creates a new brush object
	 * 
	 * @param applet the sketch applet
	 * @param position the brush initial position
	 * @param size the brush size
	 * @param nBristles the total number of bristles in the brush
	 * @param bristleLength the typical bristle length
	 * @param bristleThickness the typical bristle thickness
	 */
	public Brush(PApplet applet, PVector position, float size, int nBristles, float bristleLength,
			float bristleThickness) {
		this.applet = applet;
		this.position = position;
		this.nBristles = nBristles;
		this.bristles = new Bristle[this.nBristles];
		this.bOffsets = new PVector[this.nBristles];
		this.bPositions = new PVector[this.nBristles];
		this.positionsHistory = new ArrayList<PVector>(POSITIONS_FOR_AVERAGE);
		this.averagePosition = this.position.copy();
		this.noiseSeed = this.applet.random(1000);
		this.updatesCounter = 0;
		this.bristleHorizontalNoise = Math.min(0.3f * size, MAX_BRISTLE_HORIZONTAL_NOISE);

		// Populate the bristles arrays
		int nElements = Math.max(3, PApplet.round(PApplet.sqrt(2 * bristleLength)));
		float initLength = nElements;
		float lengthDecrement = 1;
		float initThickness = bristleThickness;
		float thicknessDecrement = initThickness / nElements;

		for (int bristle = 0; bristle < this.nBristles; bristle++) {
			this.bristles[bristle] = new Bristle(nElements, initLength, lengthDecrement, initThickness,
					thicknessDecrement);
			this.bOffsets[bristle] = new PVector(size * this.applet.random(-0.5f, 0.5f),
					BRISTLE_VERTICAL_NOISE * this.applet.random(-0.5f, 0.5f));
			this.bPositions[bristle] = new PVector(0, 0);
		}
	}

	/**
	 * Moves the brush to a new position and resets some internal counters
	 * 
	 * @param newPosition the new position
	 */
	public void reset(PVector newPosition) {
		position.set(newPosition.x, newPosition.y);
		positionsHistory.clear();
		averagePosition.set(position.x, position.y);
		updatesCounter = 0;
	}

	/**
	 * Updates the brush properties (position, direction, etc)
	 * 
	 * @param newPosition the new position
	 * @param updateBristleElements true if the bristles element positions should be updated
	 */
	public void update(PVector newPosition, boolean updateBristleElements) {
		// Update the position
		position.set(newPosition.x, newPosition.y);

		// Add the new position to the positions history
		if (positionsHistory.size() < POSITIONS_FOR_AVERAGE) {
			positionsHistory.add(newPosition);
		} else {
			positionsHistory.set(updatesCounter % POSITIONS_FOR_AVERAGE, newPosition);
		}

		// Calculate the new average position
		float xAverage = 0;
		float yAverage = 0;

		for (PVector pos : positionsHistory) {
			xAverage += pos.x;
			yAverage += pos.y;
		}

		xAverage /= positionsHistory.size();
		yAverage /= positionsHistory.size();

		// Calculate the direction angle
		float directionAngle = PApplet.HALF_PI
				+ PApplet.atan2(yAverage - averagePosition.y, xAverage - averagePosition.x);

		// Update the average position
		averagePosition.set(xAverage, yAverage);

		// Update the bristles positions array
		updateBristlePositions(directionAngle);

		// Update the bristles elements to their new positions
		if (updateBristleElements) {
			if (positionsHistory.size() < POSITIONS_FOR_AVERAGE) {
				for (int bristle = 0; bristle < nBristles; bristle++) {
					bristles[bristle].setPosition(bPositions[bristle]);
				}
			} else {
				for (int bristle = 0; bristle < nBristles; bristle++) {
					bristles[bristle].updatePosition(bPositions[bristle]);
				}
			}
		}

		// Increment the update counter
		updatesCounter++;
	}

	/**
	 * Updates the bristle positions array
	 * 
	 * @param directionAngle the brush movement direction angle
	 */
	private void updateBristlePositions(float directionAngle) {
		// This saves some calculations
		float cos = PApplet.cos(directionAngle);
		float sin = PApplet.sin(directionAngle);
		float noisePos = noiseSeed + NOISE_SPEED_FACTOR * updatesCounter;

		for (int bristle = 0; bristle < nBristles; bristle++) {
			// Add some horizontal noise to make it look more realistic
			PVector offset = bOffsets[bristle];
			float x = offset.x + bristleHorizontalNoise * (applet.noise(noisePos + 0.1f * bristle) - 0.5f);
			float y = offset.y;

			// Rotate the offset vector and add it to the position
			bPositions[bristle].set(position.x + (x * cos - y * sin), position.y + (x * sin + y * cos));
		}
	}

	/**
	 * Paints the brush on the screen and the canvas using the provided bristle colors
	 * 
	 * @param colors the bristle colors
	 * @param alpha colors alpha value
	 * @param canvas the canvas buffer
	 * @param paintCanvas true if the brush should be painted on the canvas buffer
	 */
	public void paintOnCanvasAndScreen(int[] colors, int alpha, PGraphics canvas, boolean paintCanvas) {
		if (positionsHistory.size() == POSITIONS_FOR_AVERAGE && alpha > 0) {
			// Shift the alpha value
			alpha = alpha << 24;

			// Paint the bristles
			for (int bristle = 0; bristle < nBristles; bristle++) {
				bristles[bristle].paintOnCanvasAndScreen((colors[bristle] & 0x00ffffff) | alpha, applet, canvas,
						paintCanvas);
			}
		}
	}

	/**
	 * Paints the brush on the canvas buffer using the provided bristle colors
	 * 
	 * @param colors the bristle colors
	 * @param alpha colors alpha value
	 * @param canvas the canvas buffer
	 */
	public void paintOnCanvas(int[] colors, int alpha, PGraphics canvas) {
		if (positionsHistory.size() == POSITIONS_FOR_AVERAGE && alpha > 0) {
			// Shift the alpha value
			alpha = alpha << 24;

			// Paint the bristles
			for (int bristle = 0; bristle < nBristles; bristle++) {
				bristles[bristle].paintOnCanvas((colors[bristle] & 0x00ffffff) | alpha, canvas);
			}
		}
	}

	/**
	 * Paints the brush on the screen using the provided bristle colors
	 * 
	 * @param colors the bristle colors
	 * @param alpha colors alpha value
	 */
	public void paintOnScreen(int[] colors, int alpha) {
		if (positionsHistory.size() == POSITIONS_FOR_AVERAGE && alpha > 0) {
			// Shift the alpha value
			alpha = alpha << 24;

			// Paint the bristles
			for (int bristle = 0; bristle < nBristles; bristle++) {
				bristles[bristle].paintOnScreen((colors[bristle] & 0x00ffffff) | alpha, applet);
			}
		}
	}

	/**
	 * Returns the total number of bristles in the brush
	 * 
	 * @return the total number of bristles in the brush
	 */
	public int getNBristles() {
		return nBristles;
	}

	/**
	 * Returns the current bristles positions
	 * 
	 * @return the current bristles positions
	 */
	public PVector[] getBristlesPositions() {
		return positionsHistory.size() == POSITIONS_FOR_AVERAGE ? bPositions : null;
	}
}
