package oilPainting;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

/**
 * This class simulates the movement of a brush on the canvas
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class Trace {
	/**
	 * Sets how random the trace movement is
	 */
	private static final float NOISE_FACTOR = 0.007f;

	/**
	 * The maximum allowed fraction of pixels in the trace trajectory with colors similar to the original image
	 */
	private static final float MAX_SIMILAR_COLOR_FRACTION_IN_TRAJECTORY = 0.6f;

	/**
	 * The maximum allowed fraction of pixels in the trace trajectory that have been visited before
	 */
	private static final float MAX_VISITS_FRACTION_IN_TRAJECTORY = 0.35f;

	/**
	 * The maximum allowed fraction of pixels in the trace trajectory that fall outside the canvas
	 */
	private static final float MAX_OUTSIDE_FRACTION_IN_TRAJECTORY = 0.6f;

	/**
	 * The maximum allowed fraction of pixels in the trace with colors similar to the original image
	 */
	private static final float MAX_SIMILAR_COLOR_FRACTION = 0.85f;

	/**
	 * The maximum allowed fraction of pixels in the trace that fall outside the canvas
	 */
	private static final float MAX_OUTSIDE_FRACTION = 0.3f;

	/**
	 * The minimum fraction of trace that needs to be painted already to consider it painted
	 */
	private static final float MIN_PAINTED_FRACTION = 0.65f;

	/**
	 * The trace minimum color improvement factor required to paint it on the canvas
	 */
	private static final float MIN_COLOR_IMPROVEMENT_FACTOR = 20;

	/**
	 * The minimum improvement fraction in the number of well painted pixels to consider to paint the trace even if
	 * there is not a significant color improvement
	 */
	private static final float BIG_WELL_PAINTED_IMPROVEMENT_FRACTION = 0.35f;

	/**
	 * The minimum reduction fraction in the number of bad painted pixels required to paint the trace on the canvas
	 */
	private static final float MIN_BAD_PAINTED_REDUCTION_FRACTION = 0.3f;

	/**
	 * The maximum allowed fraction of pixels in the trace that were previously well painted and will be now bad painted
	 */
	private static final float MAX_WELL_PAINTED_DESTRUCTION_FRACTION = 0.55f;

	/**
	 * The brightness relative change range between the bristles
	 */
	private static final float BRIGHTNESS_RELATIVE_CHANGE = 0.09f;

	/**
	 * The typical step when the color mixing starts
	 */
	private static final int TYPICAL_MIX_STARTING_STEP = 5;

	/**
	 * The color mixing strength
	 */
	private static final float MIX_STRENGTH = 0.015f;

	/**
	 * The minimum alpha value to be considered for the trace average color calculation
	 */
	private static final int MIN_ALPHA = 20;

	private PApplet applet;
	private int nSteps;
	private PVector[] positions;
	private int[] alphas;
	private int[][] colors;
	private Brush brush;
	private int nBristles;

	/**
	 * Creates a new trace object
	 * 
	 * @param applet the sketch applet
	 * @param position the trace initial position
	 * @param nSteps the total number of steps in the trace trajectory
	 * @param speed the trace moving speed
	 */
	public Trace(PApplet applet, PVector position, int nSteps, float speed) {
		this.applet = applet;
		this.nSteps = nSteps;
		this.positions = new PVector[this.nSteps];
		this.alphas = new int[this.nSteps];
		this.colors = null;
		this.brush = null;
		this.nBristles = 0;

		// Fill the arrays
		float initAng = this.applet.random(PApplet.TWO_PI);
		float noiseSeed = this.applet.random(1000);
		PVector previousPos = position;
		float alphaDecrement = PApplet.min(255f / this.nSteps, 25f);
		float previousAlpha = 255 + alphaDecrement;

		for (int step = 0; step < this.nSteps; step++) {
			float ang = initAng + PApplet.TWO_PI * (this.applet.noise(noiseSeed + NOISE_FACTOR * step) - 0.5f);
			previousPos = new PVector(previousPos.x + speed * PApplet.cos(ang),
					previousPos.y + speed * PApplet.sin(ang));
			previousAlpha -= alphaDecrement;
			this.positions[step] = previousPos;
			this.alphas[step] = (int) previousAlpha;
		}
	}

	/**
	 * Checks if the trace trajectory is valid. To be valid it should fall on a region that was not painted correctly
	 * before, the fraction of visited pixels in the trace trajectory should be small, and it should not fall most of
	 * the time outside the canvas.
	 * 
	 * @param similarColor the similar color array
	 * @param visitedPixels the visited pixels array
	 * @param width the image width
	 * @param height the image height
	 * @return true if the trace has a valid trajectory
	 */
	public boolean hasValidTrajectory(boolean[] similarColor, boolean[] visitedPixels, int width, int height) {
		int similarColorCounter = 0;
		int visitedPixelsCounter = 0;
		int outsideCounter = 0;

		for (PVector pos : positions) {
			int x = (int) pos.x;
			int y = (int) pos.y;

			// Check if the trace position falls on the canvas
			if (x >= 0 && x < width && y >= 0 && y < height) {
				int pixel = x + y * width;

				if (similarColor[pixel]) {
					similarColorCounter++;
				}

				if (visitedPixels[pixel]) {
					visitedPixelsCounter++;
				}
			} else {
				outsideCounter++;
			}
		}

		boolean badPainted = similarColorCounter <= MAX_SIMILAR_COLOR_FRACTION_IN_TRAJECTORY * nSteps;
		boolean notVisited = visitedPixelsCounter <= MAX_VISITS_FRACTION_IN_TRAJECTORY * nSteps;
		boolean insideCanvas = outsideCounter <= MAX_OUTSIDE_FRACTION_IN_TRAJECTORY * nSteps;

		return badPainted && notVisited && insideCanvas;
	}

	/**
	 * Associates a brush to the trace
	 * 
	 * @param newBrush the new brush
	 */
	public void setBrush(Brush newBrush) {
		brush = newBrush;
		nBristles = brush.getNBristles();
	}

	/**
	 * Calculates the trace colors
	 * 
	 * @param maxColorDiff the maximum color difference between the original image and the already painted color
	 * @param similarColor the similar color array
	 * @param originalImg the original image
	 * @param canvas the canvas buffer
	 * @param bgColor the canvas background color
	 * @return false if the region covered by the trace was already painted with similar colors, most of the trace is
	 *         outside the canvas, or drawing the trace will not improve considerably the painting
	 */
	public boolean calculateColors(float[] maxColorDiff, boolean[] similarColor, PImage originalImg, PGraphics canvas,
			int bgColor) {
		// Initialize the colors array
		colors = new int[nSteps][nBristles];

		// Load the canvas pixels
		canvas.loadPixels();
		int width = canvas.width;
		int height = canvas.height;

		// Calculate the trace average color and obtain some trace statistics
		int rAverage = 0;
		int gAverage = 0;
		int bAverage = 0;
		int insideCounter = 0;
		int outsideCounter = 0;
		int similarColorCounter = 0;
		int[][] originalColors = new int[nSteps][nBristles];
		boolean[][] similarColorBool = new boolean[nSteps][nBristles];

		for (int step = 0; step < nSteps; step++) {
			// Move the brush and get the bristles positions
			brush.update(positions[step], false);
			PVector[] bristlesPositions = brush.getBristlesPositions();

			if (bristlesPositions != null) {
				// Check if the alpha value is high enough for the average calculation
				boolean highAlpha = alphas[step] >= MIN_ALPHA;

				for (int bristle = 0; bristle < nBristles; bristle++) {
					// Check that the bristle position is inside the canvas
					int x = (int) bristlesPositions[bristle].x;
					int y = (int) bristlesPositions[bristle].y;

					if (x >= 0 && x < width && y >= 0 && y < height) {
						// Save the canvas color if it's not the background color
						int pixel = x + y * width;
						int canvasColor = canvas.pixels[pixel];

						if (canvasColor != bgColor) {
							colors[step][bristle] = canvasColor;
						}

						// Save the original image color
						int originalImgColor = originalImg.pixels[pixel];
						originalColors[step][bristle] = originalImgColor;

						// Add the original image color to the average if alpha is high
						if (highAlpha) {
							rAverage += (originalImgColor >> 16) & 0xff;
							gAverage += (originalImgColor >> 8) & 0xff;
							bAverage += originalImgColor & 0xff;

							// Increment the counters
							insideCounter++;

							if (similarColor[pixel]) {
								similarColorBool[step][bristle] = true;
								similarColorCounter++;
							}
						}
					} else if (highAlpha) {
						outsideCounter++;
					}
				}
			}
		}

		if (insideCounter > 0) {
			rAverage /= insideCounter;
			gAverage /= insideCounter;
			bAverage /= insideCounter;
		}

		// Update the canvas pixels
		canvas.updatePixels();

		// Reset the brush
		brush.reset(positions[0]);

		// Check if the trace region was painted before with similar colors or falls outside the image
		boolean wellPainted = similarColorCounter >= MAX_SIMILAR_COLOR_FRACTION * insideCounter;
		boolean outsideCanvas = outsideCounter >= MAX_OUTSIDE_FRACTION * (insideCounter + outsideCounter);

		if (wellPainted || outsideCanvas) {
			// The trace is not valid, don't paint it
			return false;
		}

		// Check that drawing the trace will improve the accuracy of the painting
		int wellPaintedCounter = 0;
		int destroyedWellPaintedCounter = 0;
		int alreadyPaintedCounter = 0;
		int colorImprovement = 0;

		for (int step = 0; step < nSteps; step++) {
			// Check if the alpha value is high enough
			if (alphas[step] >= MIN_ALPHA) {
				for (int bristle = 0; bristle < nBristles; bristle++) {
					// Check that the bristle position is inside the canvas
					int originalColor = originalColors[step][bristle];

					if (originalColor != 0) {
						// Count the number of well painted pixels, and how many are not well painted anymore
						int rOriginal = (originalColor >> 16) & 0xff;
						int gOriginal = (originalColor >> 8) & 0xff;
						int bOriginal = originalColor & 0xff;
						int rDiff = Math.abs(rOriginal - rAverage);
						int gDiff = Math.abs(gOriginal - gAverage);
						int bDiff = Math.abs(bOriginal - bAverage);

						if ((rDiff < maxColorDiff[0]) && (gDiff < maxColorDiff[1]) && (bDiff < maxColorDiff[2])) {
							wellPaintedCounter++;
						} else if (similarColorBool[step][bristle]) {
							destroyedWellPaintedCounter++;
						}

						// Count previously painted pixels and calculate their color improvement
						int canvasColor = colors[step][bristle];

						if (canvasColor != 0) {
							alreadyPaintedCounter++;
							colorImprovement += Math.abs(rOriginal - ((canvasColor >> 16) & 0xff)) - rDiff
									+ Math.abs(gOriginal - ((canvasColor >> 8) & 0xff)) - gDiff
									+ Math.abs(bOriginal - (canvasColor & 0xff)) - bDiff;
						}
					}
				}
			}
		}

		int wellPaintedImprovement = wellPaintedCounter - similarColorCounter;
		int previousBadPainted = insideCounter - similarColorCounter;

		boolean alreadyPainted = alreadyPaintedCounter >= MIN_PAINTED_FRACTION * insideCounter;
		boolean colorImproves = colorImprovement >= MIN_COLOR_IMPROVEMENT_FACTOR * alreadyPaintedCounter;
		boolean bigWellPaintedImprovement = wellPaintedImprovement >= BIG_WELL_PAINTED_IMPROVEMENT_FRACTION
				* insideCounter;
		boolean reducedBadPainted = wellPaintedImprovement >= MIN_BAD_PAINTED_REDUCTION_FRACTION * previousBadPainted;
		boolean lowWellPaintedDestruction = destroyedWellPaintedCounter <= MAX_WELL_PAINTED_DESTRUCTION_FRACTION
				* wellPaintedImprovement;
		boolean improves = (colorImproves || bigWellPaintedImprovement) && reducedBadPainted
				&& lowWellPaintedDestruction;

		if (alreadyPainted && !improves) {
			// Don't use this trace, we are not going to improve the painting
			return false;
		}

		// The trace is good enough for painting!
		// Set the first step bristle colors to the original image average color
		int averageColor = (rAverage << 16) | (gAverage << 8) | bAverage;
		float hueAverage = applet.hue(averageColor);
		float saturationAverage = applet.saturation(averageColor);
		float brightnessAverage = applet.brightness(averageColor);
		float noiseSeed = applet.random(1000);

		applet.colorMode(PApplet.HSB, 255);

		for (int bristle = 0; bristle < nBristles; bristle++) {
			// Add some brightness changes to make it more realistic
			float deltaBrightness = BRIGHTNESS_RELATIVE_CHANGE * brightnessAverage
					* (applet.noise(noiseSeed + 0.4f * bristle) - 0.5f);
			colors[0][bristle] = applet.color(hueAverage, saturationAverage,
					PApplet.constrain(brightnessAverage + deltaBrightness, 0, 255));
		}

		applet.colorMode(PApplet.RGB, 255);

		// Extend the colors to the step where the mixing starts
		int mixStartingStep = PApplet.constrain(TYPICAL_MIX_STARTING_STEP, 1, nSteps);

		for (int step = 1; step < mixStartingStep; step++) {
			System.arraycopy(colors[0], 0, colors[step], 0, nBristles);
		}

		// Mix the previous step colors with the canvas colors
		float[] rPrevious = new float[nBristles];
		float[] gPrevious = new float[nBristles];
		float[] bPrevious = new float[nBristles];

		for (int bristle = 0; bristle < nBristles; bristle++) {
			rPrevious[bristle] = (colors[mixStartingStep - 1][bristle] >> 16) & 0xff;
			gPrevious[bristle] = (colors[mixStartingStep - 1][bristle] >> 8) & 0xff;
			bPrevious[bristle] = colors[mixStartingStep - 1][bristle] & 0xff;
		}

		float f = 1 - MIX_STRENGTH;

		for (int step = mixStartingStep; step < nSteps; step++) {
			for (int bristle = 0; bristle < nBristles; bristle++) {
				int canvasColor = colors[step][bristle];

				if (canvasColor != 0) {
					rPrevious[bristle] = f * rPrevious[bristle] + MIX_STRENGTH * ((canvasColor >> 16) & 0xff);
					gPrevious[bristle] = f * gPrevious[bristle] + MIX_STRENGTH * ((canvasColor >> 8) & 0xff);
					bPrevious[bristle] = f * bPrevious[bristle] + MIX_STRENGTH * (canvasColor & 0xff);
					colors[step][bristle] = (((int) rPrevious[bristle]) << 16) | (((int) gPrevious[bristle]) << 8)
							| ((int) bPrevious[bristle]);
				} else {
					colors[step][bristle] = colors[step - 1][bristle];
				}
			}
		}

		// The trace is ready for painting
		return true;
	}

	/**
	 * Paints the trace on the screen and the canvas
	 * 
	 * @param visitedPixels the visited pixels array
	 * @param canvas the canvas buffer
	 */
	public void paint(boolean[] visitedPixels, PGraphics canvas) {
		// Check that the trace colors have been initialized
		if (colors != null) {
			// Prepare the canvas for drawing
			canvas.beginDraw();
			int width = canvas.width;
			int height = canvas.height;

			// Paint the brush step by step
			for (int step = 0; step < nSteps; step++) {
				// Check if the alpha value is high enough to paint it on the canvas
				boolean highAlpha = alphas[step] > MIN_ALPHA;

				// Move the brush and paint it on the canvas
				brush.update(positions[step], true);
				brush.paint(colors[step], alphas[step], canvas, highAlpha);

				// Fill the visited pixels array if alpha is high enough
				if (highAlpha) {
					PVector[] bristlesPositions = brush.getBristlesPositions();

					if (bristlesPositions != null) {
						for (PVector pos : bristlesPositions) {
							int x = (int) pos.x;
							int y = (int) pos.y;

							if (x >= 0 && x < width && y >= 0 && y < height) {
								visitedPixels[x + y * width] = true;
							}
						}
					}
				}
			}

			// Close the canvas
			canvas.endDraw();

			// Reset the brush
			brush.reset(positions[0]);
		}
	}

	/**
	 * Paints the trace on the screen and the canvas for a given trace step
	 * 
	 * @param step the step to paint
	 * @param visitedPixels the visited pixels array
	 * @param canvas the canvas buffer
	 */
	public void paint(int step, boolean[] visitedPixels, PGraphics canvas) {
		// Check that the trace colors have been initialized
		if (colors != null) {
			// Prepare the canvas for drawing
			canvas.beginDraw();
			int width = canvas.width;
			int height = canvas.height;

			// Check if the alpha value is high enough to paint it on the canvas
			boolean highAlpha = alphas[step] > MIN_ALPHA;

			// Move the brush and paint it on the canvas
			brush.update(positions[step], true);
			brush.paint(colors[step], alphas[step], canvas, highAlpha);

			// Close the canvas
			canvas.endDraw();

			// Fill the visited pixels array if alpha is high enough
			if (highAlpha) {
				PVector[] bristlesPositions = brush.getBristlesPositions();

				if (bristlesPositions != null) {
					for (PVector pos : bristlesPositions) {
						int x = (int) pos.x;
						int y = (int) pos.y;

						if (x >= 0 && x < width && y >= 0 && y < height) {
							visitedPixels[x + y * width] = true;
						}
					}
				}
			}
		}
	}

	/**
	 * Returns the number of steps in the trace trajectory
	 * 
	 * @return the total number of steps in the trace trajectory
	 */
	public int getNSteps() {
		return nSteps;
	}
}
