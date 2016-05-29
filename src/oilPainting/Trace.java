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
	 * The minimum fraction of pixels in the trace trajectory that should fall inside the canvas
	 */
	private static final float MIN_INSIDE_FRACTION_IN_TRAJECTORY = 0.4f;

	/**
	 * The maximum allowed value of the colors standard deviation along the trace trajectory
	 */
	private static final float MAX_COLOR_STDEV_IN_TRAJECTORY = 45f;

	/**
	 * The maximum allowed fraction of pixels in the trace with colors similar to the original image
	 */
	private static final float MAX_SIMILAR_COLOR_FRACTION = 0.5f; // 0.8 - 0.85 - 0.5

	/**
	 * The minimum fraction of pixels in the trace that should fall inside the canvas
	 */
	private static final float MIN_INSIDE_FRACTION = 0.7f;

	/**
	 * The minimum fraction of trace that needs to be painted already to consider it painted
	 */
	private static final float MIN_PAINTED_FRACTION = 0.65f;

	/**
	 * The minimum color improvement factor of the already painted pixels required to paint the trace on the canvas
	 */
	private static final float MIN_COLOR_IMPROVEMENT_FACTOR = 0.6f;

	/**
	 * The minimum improvement fraction in the number of well painted pixels to consider to paint the trace even if
	 * there is not a significant color improvement
	 */
	private static final float BIG_WELL_PAINTED_IMPROVEMENT_FRACTION = 0.4f; // 0.3 - 0.35 - 0.4

	/**
	 * The minimum reduction fraction in the number of bad painted pixels required to paint the trace on the canvas
	 */
	private static final float MIN_BAD_PAINTED_REDUCTION_FRACTION = 0.45f; // 0.45 - 0.3 - 0.45

	/**
	 * The maximum allowed fraction of pixels in the trace that were previously well painted and will be now bad painted
	 */
	private static final float MAX_WELL_PAINTED_DESTRUCTION_FRACTION = 0.4f; // 0.4 - 0.55 - 0.4

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
	private static final float MIX_STRENGTH = 0.012f;

	/**
	 * The minimum alpha value to be considered for the trace average color calculation
	 */
	private static final int MIN_ALPHA = 20;

	private PApplet applet;
	private int nSteps;
	private PVector[] positions;
	private int[][] colors;
	private int[] alphas;
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
		this.colors = null;
		this.alphas = null;
		this.brush = null;
		this.nBristles = 0;

		// Fill the positions array
		PVector pos = position.copy();
		this.positions[0] = pos;
		float initAng = this.applet.random(PApplet.TWO_PI);
		float noiseSeed = this.applet.random(1000);

		for (int step = 1; step < this.nSteps; step++) {
			float ang = initAng + PApplet.TWO_PI * (this.applet.noise(noiseSeed + NOISE_FACTOR * step) - 0.5f);
			pos = new PVector(pos.x + speed * PApplet.cos(ang), pos.y + speed * PApplet.sin(ang));
			this.positions[step] = pos;
		}
	}

	/**
	 * Checks if the trace trajectory is valid. To be valid it should fall on a region that was not painted correctly
	 * before, the fraction of visited pixels in the trace trajectory should be small, it should not fall most of the
	 * time outside the canvas, and the color changes should not be too high.
	 * 
	 * @param originalImg the original image
	 * @param similarColor the similar color array
	 * @param visitedPixels the visited pixels array
	 * @return true if the trace has a valid trajectory
	 */
	public boolean hasValidTrajectory(boolean[] similarColor, boolean[] visitedPixels, PImage originalImg) {
		// Obtain some pixel statistics along the trajectory
		int insideCounter = 0;
		int similarColorCounter = 0;
		int visitedPixelsCounter = 0;
		int redSum = 0;
		int redSqSum = 0;
		int greenSum = 0;
		int greenSqSum = 0;
		int blueSum = 0;
		int blueSqSum = 0;
		int width = originalImg.width;
		int height = originalImg.height;

		for (PVector pos : positions) {
			int x = (int) pos.x;
			int y = (int) pos.y;

			// Check that it's inside the picture
			if (x >= 0 && x < width && y >= 0 && y < height) {
				// Increase the counters
				insideCounter++;
				int pixel = x + y * width;

				if (similarColor[pixel]) {
					similarColorCounter++;
				}

				if (visitedPixels[pixel]) {
					visitedPixelsCounter++;
				}

				// Extract the pixel color properties
				int col = originalImg.pixels[pixel];
				int red = (col >> 16) & 0xff;
				int green = (col >> 8) & 0xff;
				int blue = col & 0xff;
				redSum += red;
				redSqSum += red * red;
				greenSum += green;
				greenSqSum += green * green;
				blueSum += blue;
				blueSqSum += blue * blue;
			}
		}

		// Obtain the colors standard deviation along the trajectory
		float redStDev = 0;
		float greenStDev = 0;
		float blueStDev = 0;

		if (insideCounter > 1) {
			redStDev = PApplet.sqrt((redSqSum - redSum * redSum / insideCounter) / (insideCounter - 1));
			greenStDev = PApplet.sqrt((greenSqSum - greenSum * greenSum / insideCounter) / (insideCounter - 1));
			blueStDev = PApplet.sqrt((blueSqSum - blueSum * blueSum / insideCounter) / (insideCounter - 1));
		}

		// Check if it's a valid trajectory
		boolean insideCanvas = insideCounter >= MIN_INSIDE_FRACTION_IN_TRAJECTORY * nSteps;
		boolean badPainted = similarColorCounter <= MAX_SIMILAR_COLOR_FRACTION_IN_TRAJECTORY * insideCounter;
		boolean notVisited = visitedPixelsCounter <= MAX_VISITS_FRACTION_IN_TRAJECTORY * insideCounter;
		boolean smallColorChange = redStDev < MAX_COLOR_STDEV_IN_TRAJECTORY
				&& greenStDev < MAX_COLOR_STDEV_IN_TRAJECTORY && blueStDev < MAX_COLOR_STDEV_IN_TRAJECTORY;

		return insideCanvas && badPainted && notVisited && smallColorChange;
	}

	/**
	 * Defines the brush size that should be used to paint the trace
	 * 
	 * @param brushSize the brush size
	 */
	public void setBrushSize(float brushSize) {
		brush = new Brush(applet, brushSize);
		brush.init(positions[0]);
		nBristles = brush.getNBristles();
	}

	/**
	 * Calculates the trace colors
	 * 
	 * @param maxColorDiff the maximum color difference between the original image and the already painted color
	 * @param similarColor the similar color array
	 * @param originalImg the original image
	 * @param canvas the canvas buffer. If it's null, the sketch applet will be used instead
	 * @param bgColor the canvas background color
	 * @return false if the region covered by the trace was already painted with similar colors, most of the trace is
	 *         outside the canvas, or drawing the trace will not improve considerably the painting
	 */
	public boolean calculateColors(int[] maxColorDiff, boolean[] similarColor, PImage originalImg, PGraphics canvas,
			int bgColor) {
		// Create the colors and alphas arrays
		colors = new int[nSteps][nBristles];
		alphas = new int[nSteps];

		// Get the already painted pixels from the canvas buffer or the screen
		int[] paintedPixels = null;
		int paintedPixelsWidth = 0;

		if (canvas != null) {
			canvas.loadPixels();
			paintedPixels = canvas.pixels;
			paintedPixelsWidth = canvas.width;
		} else {
			applet.loadPixels();
			paintedPixels = applet.pixels;
			paintedPixelsWidth = applet.width;
		}

		// Calculate the trace average color and obtain some trace statistics
		int redAverage = 0;
		int greenAverage = 0;
		int blueAverage = 0;
		int insideCounter = 0;
		int outsideCounter = 0;
		int similarColorCounter = 0;
		int[][] originalColors = new int[nSteps][nBristles];
		boolean[][] similarColorBool = new boolean[nSteps][nBristles];
		int width = originalImg.width;
		int height = originalImg.height;
		float alphaDecrement = Math.min(255f / nSteps, 25f);
		float alpha = 255 + alphaDecrement;

		for (int step = 0; step < nSteps; step++) {
			// Move the brush and get the bristles positions
			brush.update(positions[step], false);
			PVector[] bristlesPositions = brush.getBristlesPositions();

			// Calculate the alpha value and check if it's high enough for the average color calculation
			alpha -= alphaDecrement;
			alphas[step] = PApplet.constrain((int) alpha, 0, 255);

			if (alpha >= MIN_ALPHA && bristlesPositions != null) {
				for (int bristle = 0; bristle < nBristles; bristle++) {
					// Check that the bristle is inside the canvas
					int x = (int) bristlesPositions[bristle].x;
					int y = (int) bristlesPositions[bristle].y;

					if (x >= 0 && x < width && y >= 0 && y < height) {
						// Save the already painted color if it's not the background color
						int paintedColor = paintedPixels[x + y * paintedPixelsWidth];

						if (paintedColor != bgColor) {
							colors[step][bristle] = paintedColor;
						}

						// Save the original image color
						int pixel = x + y * width;
						int originalImgColor = originalImg.pixels[pixel];
						originalColors[step][bristle] = originalImgColor;

						// Add the original image color to the average
						redAverage += (originalImgColor >> 16) & 0xff;
						greenAverage += (originalImgColor >> 8) & 0xff;
						blueAverage += originalImgColor & 0xff;

						// Increment the counters
						insideCounter++;

						if (similarColor[pixel]) {
							similarColorBool[step][bristle] = true;
							similarColorCounter++;
						}
					} else {
						outsideCounter++;
					}
				}
			}
		}

		if (insideCounter > 0) {
			redAverage /= insideCounter;
			greenAverage /= insideCounter;
			blueAverage /= insideCounter;
		}

		// Update the canvas or screen pixels
		paintedPixels = null;

		if (canvas != null) {
			canvas.updatePixels();
		} else {
			applet.updatePixels();
		}

		// Reset the brush to the initial position
		brush.init(positions[0]);

		// Check if the trace region was painted before with similar colors or falls outside the image
		boolean wellPainted = similarColorCounter >= MAX_SIMILAR_COLOR_FRACTION * insideCounter;
		boolean outsideCanvas = insideCounter < MIN_INSIDE_FRACTION * (insideCounter + outsideCounter);

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
						int redOriginal = (originalColor >> 16) & 0xff;
						int greenOriginal = (originalColor >> 8) & 0xff;
						int blueOriginal = originalColor & 0xff;
						int redDiff = Math.abs(redOriginal - redAverage);
						int greenDiff = Math.abs(greenOriginal - greenAverage);
						int blueDiff = Math.abs(blueOriginal - blueAverage);

						if ((redDiff < maxColorDiff[0]) && (greenDiff < maxColorDiff[1])
								&& (blueDiff < maxColorDiff[2])) {
							wellPaintedCounter++;
						} else if (similarColorBool[step][bristle]) {
							destroyedWellPaintedCounter++;
						}

						// Count previously painted pixels and calculate their color improvement
						int paintedColor = colors[step][bristle];

						if (paintedColor != 0) {
							alreadyPaintedCounter++;

							// Calculate the color improvement
							colorImprovement += Math.abs(redOriginal - ((paintedColor >> 16) & 0xff)) - redDiff
									+ Math.abs(greenOriginal - ((paintedColor >> 8) & 0xff)) - greenDiff
									+ Math.abs(blueOriginal - (paintedColor & 0xff)) - blueDiff;
						}
					}
				}
			}
		}

		int wellPaintedImprovement = wellPaintedCounter - similarColorCounter;
		int previousBadPainted = insideCounter - similarColorCounter;
		float averageMaxColorDiff = (maxColorDiff[0] + maxColorDiff[1] + maxColorDiff[2]) / 3.0f;

		boolean alreadyPainted = alreadyPaintedCounter >= MIN_PAINTED_FRACTION * insideCounter;
		boolean colorImproves = colorImprovement >= MIN_COLOR_IMPROVEMENT_FACTOR * averageMaxColorDiff
				* alreadyPaintedCounter;
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
		int averageColor = (redAverage << 16) | (greenAverage << 8) | blueAverage | 0xff000000;
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
					PApplet.constrain(brightnessAverage + deltaBrightness, 0, 255), 255);
		}

		applet.colorMode(PApplet.RGB, 255);

		// Extend the colors to the step where the mixing starts
		int mixStartingStep = PApplet.constrain(TYPICAL_MIX_STARTING_STEP, 1, nSteps);

		for (int step = 1; step < mixStartingStep; step++) {
			System.arraycopy(colors[0], 0, colors[step], 0, nBristles);
		}

		// Mix the previous step colors with the already painted colors
		float[] redPrevious = new float[nBristles];
		float[] greenPrevious = new float[nBristles];
		float[] bluePrevious = new float[nBristles];

		for (int bristle = 0; bristle < nBristles; bristle++) {
			redPrevious[bristle] = (colors[0][bristle] >> 16) & 0xff;
			greenPrevious[bristle] = (colors[0][bristle] >> 8) & 0xff;
			bluePrevious[bristle] = colors[0][bristle] & 0xff;
		}

		float f = 1 - MIX_STRENGTH;

		for (int step = mixStartingStep; step < nSteps; step++) {
			// Check if the alpha value is high enough for mixing
			if (alphas[step] >= MIN_ALPHA) {
				for (int bristle = 0; bristle < nBristles; bristle++) {
					int paintedColor = colors[step][bristle];

					if (paintedColor != 0) {
						float redMix = f * redPrevious[bristle] + MIX_STRENGTH * ((paintedColor >> 16) & 0xff);
						float greenMix = f * greenPrevious[bristle] + MIX_STRENGTH * ((paintedColor >> 8) & 0xff);
						float blueMix = f * bluePrevious[bristle] + MIX_STRENGTH * (paintedColor & 0xff);
						redPrevious[bristle] = redMix;
						greenPrevious[bristle] = greenMix;
						bluePrevious[bristle] = blueMix;
						colors[step][bristle] = (((int) redMix) << 16) | (((int) greenMix) << 8) | ((int) blueMix)
								| 0xff000000;
					} else {
						colors[step][bristle] = colors[step - 1][bristle];
					}
				}
			} else {
				// Copy the previous step colors
				System.arraycopy(colors[step - 1], 0, colors[step], 0, nBristles);
			}
		}

		// The trace is ready for painting
		return true;
	}

	/**
	 * Paints the trace on the canvas buffer and the screen
	 * 
	 * @param visitedPixels the visited pixels array
	 * @param width the image width
	 * @param height the image height
	 * @param canvas the canvas buffer
	 * @param paintOnlyCanvas if true only the canvas will be painted and not the screen
	 */
	public void paint(boolean[] visitedPixels, int width, int height, PGraphics canvas, boolean paintOnlyCanvas) {
		// Check that the trace colors have been initialized
		if (colors != null) {
			// Prepare the canvas buffer for drawing
			if (canvas != null) {
				canvas.beginDraw();
			}

			// Paint the brush step by step
			for (int step = 0; step < nSteps; step++) {
				// Check if the alpha value is high enough to paint it on the canvas
				int alpha = alphas[step];
				boolean highAlpha = alpha > MIN_ALPHA;

				// Move the brush
				brush.update(positions[step], true);

				// Paint the brush
				if (paintOnlyCanvas) {
					brush.paintOnCanvas(colors[step], alpha, canvas);
				} else if (canvas == null || !highAlpha) {
					brush.paintOnScreen(colors[step], alpha);
				} else {
					brush.paintOnCanvas(colors[step], 255, canvas);
					brush.paintOnScreen(colors[step], alpha);
				}

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

			// Close the canvas buffer
			if (canvas != null) {
				canvas.endDraw();
			}

			// Reset the brush to the initial position
			brush.init(positions[0]);
		}
	}

	/**
	 * Paints the trace on the canvas buffer and the screen for a given trace step
	 * 
	 * @param step the step to paint
	 * @param visitedPixels the visited pixels array
	 * @param width the image width
	 * @param height the image height
	 * @param canvas the canvas buffer
	 * @param paintOnlyCanvas if true only the canvas will be painted and not the screen
	 */
	public void paintStep(int step, boolean[] visitedPixels, int width, int height, PGraphics canvas,
			boolean paintOnlyCanvas) {
		// Check that the trace colors have been initialized
		if (colors != null && step < nSteps) {
			// Prepare the canvas buffer for drawing
			if (canvas != null) {
				canvas.beginDraw();
			}

			// Check if the alpha value is high enough to paint it on the canvas
			int alpha = alphas[step];
			boolean highAlpha = alpha > MIN_ALPHA;

			// Move the brush
			brush.update(positions[step], true);

			// Paint the brush
			if (paintOnlyCanvas) {
				brush.paintOnCanvas(colors[step], alpha, canvas);
			} else if (canvas == null || !highAlpha) {
				brush.paintOnScreen(colors[step], alpha);
			} else {
				brush.paintOnCanvas(colors[step], 255, canvas);
				brush.paintOnScreen(colors[step], alpha);
			}

			// Close the canvas buffer
			if (canvas != null) {
				canvas.endDraw();
			}

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

			// Check if we are at the last step
			if (step == nSteps - 1) {
				// Reset the brush to the initial position
				brush.init(positions[0]);
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
