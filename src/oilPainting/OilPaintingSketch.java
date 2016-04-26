package oilPainting;

import java.util.Arrays;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

/**
 * This sketch takes a picture as input and simulates an oil paint. It has many optional parameters, but only some
 * combinations produce optimal results.
 * 
 * Inspired on some of the works by Sergio Albiac.
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class OilPaintingSketch extends PApplet {
	// The path to the picture that we want to paint
	private final String pictureFile = "src/oilPainting/picture.jpg";
	// The directory where the movie frames should be saved
	private final String frameDir = "src/oilPainting/frames/";
	// The maximum RGB color difference to consider the pixel correctly painted
	private final int[] maxColorDiff = new int[] { 40, 40, 40 };
	// Use a separate canvas buffer for color mixing (a bit slower)
	private final boolean useCanvas = true;
	// Compare the oil paint with the original picture
	private final boolean comparisonMode = false;
	// Show additional debug images
	private final boolean debugMode = false;
	// Make a movie showing the painting in steps
	private final boolean makeMovie = false;
	// Paint the traces step by step, or in one go
	private final boolean paintStepByStep = false;
	// Save a picture of the final frame
	private final boolean saveFinalFramePicture = false;
	// The smaller brush size allowed
	private final float smallerBrushSize = 4;
	// The brush size decrement ratio
	private final float brushSizeDecrement = 1.3f;
	// The maximum number of invalid trajectories allowed before the brush size is reduced
	private final int maxInvalidTrajectories = 5000;
	// The maximum number of invalid trajectories allowed for the smaller brush size before the painting is stopped
	private final int maxInvalidTrajectoriesForSmallerSize = 10000;
	// The maximum number of invalid traces allowed before the brush size is reduced
	private final int maxInvalidTraces = 250;
	// The maximum number of invalid traces allowed for the smaller brush size before the painting is stopped
	private final int maxInvalidTracesForSmallerSize = 350;
	// The trace speed
	private final float traceSpeed = 2;
	// The typical trace length, relative to the brush size
	private final float relativeTraceLength = 2.3f;
	// The minimum trace length allowed
	private final float minTraceLength = 16;
	// The screen and canvas background color
	private final int bgColor = color(255);
	// The movie frame rate
	private final int movieFrameRate = 20;

	// Sketch variables
	private PImage originalImg;
	private int imgWidth;
	private int imgHeight;
	private PGraphics canvas;
	private boolean[] similarColorPixels;
	private boolean[] visitedPixels;
	private int[] badPaintedPixels;
	private int nBadPaintedPixels;
	private float averageBrushSize;
	private boolean continuePainting;
	private Trace trace;
	private int traceStep;
	private int nTraces;
	private int startTime;
	private int lastChangeTime;
	private int lastChangeFrameCount;

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
		// Load the image that we want to paint
		originalImg = loadImage(pictureFile);
		imgWidth = originalImg.width;
		imgHeight = originalImg.height;

		// Load the original image pixels. This way they will be available all the time
		originalImg.loadPixels();

		// Resize the sketch window
		surface.setResizable(true);

		if (comparisonMode) {
			surface.setSize(2 * imgWidth, imgHeight);
		} else if (debugMode) {
			surface.setSize(3 * imgWidth, imgHeight);
		} else {
			surface.setSize(imgWidth, imgHeight);
		}

		// Sketch setup
		strokeCap(SQUARE);
		background(bgColor);
		frameRate(2000);

		// Create the canvas buffer
		canvas = null;

		if (useCanvas) {
			canvas = createGraphics(imgWidth, imgHeight);

			// Canvas buffer setup
			canvas.noSmooth();
			canvas.beginDraw();
			canvas.strokeCap(SQUARE);
			canvas.background(bgColor);
			canvas.endDraw();
		}

		// Initialize the pixel arrays
		int nPixels = imgWidth * imgHeight;
		similarColorPixels = new boolean[nPixels];
		visitedPixels = new boolean[nPixels];
		badPaintedPixels = new int[nPixels];
		nBadPaintedPixels = nPixels;

		// Initialize the rest of the sketch variables
		averageBrushSize = max(smallerBrushSize, sqrt(nPixels) / 6.0f);
		continuePainting = true;
		trace = null;
		traceStep = 0;
		nTraces = 0;
		startTime = millis();
		lastChangeTime = startTime;
		lastChangeFrameCount = 0;
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Check that the oil painting simulation didn't finish
		if (continuePainting) {
			// Get a new valid trace if we are not painting one already
			if (trace == null) {
				trace = getValidTrace();
				traceStep = 0;
			}

			// Check if we should stop painting because there are no more valid traces
			if (trace == null) {
				continuePainting = false;

				// Save the final frame picture
				if (saveFinalFramePicture) {
					save(frameDir + "oilPaint-" + frameCount + ".png");
				}

				// Stop the sketch if we are not making a movie
				if (!makeMovie) {
					noLoop();
				}
			} else {
				// Paint the trace step by step or in one go
				if (paintStepByStep) {
					trace.paintStep(traceStep, visitedPixels, imgWidth, imgHeight, canvas, false);
					traceStep++;

					// Check if we finished painting the trace
					if (traceStep == trace.getNSteps()) {
						trace = null;
					}
				} else {
					trace.paint(visitedPixels, imgWidth, imgHeight, canvas, false);
					trace = null;
				}

				// Draw the additional images if necessary
				if (comparisonMode) {
					image(originalImg, imgWidth, 0);
				} else if (debugMode) {
					drawDebugImages();
				}
			}
		}

		// Save the movie frame
		if (makeMovie) {
			saveMovieFrame();
		}
	}

	/**
	 * Obtains a valid trace, ready to be painted. It returns null if there are no more valid traces and the paint can
	 * be considered finish.
	 * 
	 * @return the valid trace
	 */
	public Trace getValidTrace() {
		// Update the similar color and bad painted pixel arrays
		updatePixelArrays();

		// Obtain a new valid trace
		Trace trace = null;
		boolean traceNotFound = true;
		int invalidTrajectoriesCounter = 0;
		int invalidTracesCounter = 0;
		PVector startingPosition = new PVector(0, 0);

		while (traceNotFound) {
			// Check if we should stop painting
			if (averageBrushSize == smallerBrushSize
					&& (invalidTrajectoriesCounter > maxInvalidTrajectoriesForSmallerSize
							|| invalidTracesCounter > maxInvalidTracesForSmallerSize)) {
				float totalTime = (millis() - startTime) / 1000.0f;

				println("Total number of painted traces: " + nTraces);
				println("Average frame rate = " + round(frameCount / totalTime));
				println("Processing time = " + totalTime + " seconds");

				// Stop the while loop
				trace = null;
				traceNotFound = false;
			} else {
				// Change the average brush size if there were too many invalid traces
				if (averageBrushSize > smallerBrushSize && (invalidTrajectoriesCounter > maxInvalidTrajectories
						|| invalidTracesCounter > maxInvalidTraces)) {
					averageBrushSize = max(smallerBrushSize,
							min(averageBrushSize / brushSizeDecrement, averageBrushSize - 2));
					float ellapsedTime = (millis() - lastChangeTime) / 1000.0f;

					println("Frame = " + frameCount + ", traces = " + nTraces + ", new average brush size = "
							+ averageBrushSize);
					println("Average frame rate = " + round((frameCount - lastChangeFrameCount) / ellapsedTime));
					println();

					// Reset some of the variables
					invalidTrajectoriesCounter = 0;
					invalidTracesCounter = 0;
					lastChangeTime = millis();
					lastChangeFrameCount = frameCount;

					// Reset the visited pixels array
					Arrays.fill(visitedPixels, false);
				}

				// Create new traces until one of them has a valid trajectory or we exceed a number of tries
				boolean validTrajectory = false;
				float brushSize = max(smallerBrushSize, averageBrushSize * random(0.95f, 1.05f));
				int nSteps = (int) (max(minTraceLength, relativeTraceLength * brushSize * random(0.9f, 1.1f))
						/ traceSpeed);

				while (!validTrajectory && invalidTrajectoriesCounter % 500 != 499) {
					// Create the trace
					int pixel = badPaintedPixels[floor(random(nBadPaintedPixels))];
					startingPosition.set(pixel % imgWidth, pixel / imgWidth);
					trace = new Trace(this, startingPosition, nSteps, traceSpeed);

					// Check if it has a valid trajectory
					validTrajectory = trace.hasValidTrajectory(similarColorPixels, visitedPixels, originalImg);

					// Increase the counter
					invalidTrajectoriesCounter++;
				}

				// Check if we have a valid trajectory
				if (validTrajectory) {
					// Reset the invalid trajectories counter
					invalidTrajectoriesCounter = 0;

					// Set the trace brush size
					trace.setBrushSize(brushSize);

					// Calculate the trace colors and check that painting the trace will improve the painting
					if (trace.calculateColors(maxColorDiff, similarColorPixels, originalImg, canvas, bgColor)) {
						// Test passed, the trace is good enough to be painted
						traceNotFound = false;
						nTraces++;
					} else {
						// The trace is not good enough, try again in the next loop
						invalidTracesCounter++;
					}
				} else {
					// The trace is not good enough, try again in the next loop
					invalidTrajectoriesCounter++;
					invalidTracesCounter++;
				}
			}
		}

		// Return the trace
		return trace;
	}

	/**
	 * Updates the similar color and bad painted pixel arrays
	 */
	public void updatePixelArrays() {
		// Load the screen pixels
		loadPixels();

		// Update the arrays
		nBadPaintedPixels = 0;

		for (int x = 0; x < imgWidth; x++) {
			for (int y = 0; y < imgHeight; y++) {
				// Check if the pixel is well painted
				boolean wellPainted = false;
				int imgPixel = x + y * imgWidth;
				int paintedCol = pixels[x + y * width];

				if (paintedCol != bgColor) {
					int originalCol = originalImg.pixels[imgPixel];
					wellPainted = abs(((originalCol >> 16) & 0xff) - ((paintedCol >> 16) & 0xff)) < maxColorDiff[0]
							&& abs(((originalCol >> 8) & 0xff) - ((paintedCol >> 8) & 0xff)) < maxColorDiff[1]
							&& abs((originalCol & 0xff) - (paintedCol & 0xff)) < maxColorDiff[2];
				}

				similarColorPixels[imgPixel] = wellPainted;

				if (!wellPainted) {
					badPaintedPixels[nBadPaintedPixels] = imgPixel;
					nBadPaintedPixels++;
				}
			}
		}

		// Update the screen pixels
		updatePixels();
	}

	/**
	 * Draws on the screen the visited pixels and similar color pixels arrays
	 */
	public void drawDebugImages() {
		// Load the screen pixels
		loadPixels();

		// Draw the arrays
		for (int x = 0; x < imgWidth; x++) {
			for (int y = 0; y < imgHeight; y++) {
				int imgPixel = x + y * imgWidth;
				int screenPixel = x + y * width + imgWidth;
				pixels[screenPixel] = visitedPixels[imgPixel] ? 0xff000000 : 0xffffffff;
				pixels[screenPixel + imgWidth] = similarColorPixels[imgPixel] ? 0xff000000 : 0xffffffff;
			}
		}

		// Update the screen pixels
		updatePixels();
	}

	/**
	 * Saves the movie frames with a format that can be processed with the movie maker tool
	 */
	public void saveMovieFrame() {
		if (!continuePainting) {
			// Trick to make the Movie Maker tool work when we don't paint anything
			fill(color(random(250, 255)));
			ellipse(0, 0, 2, 2);
		}

		if (frameCount % movieFrameRate == 0) {
			String fileRootName = frameDir;

			if (frameCount < 10) {
				fileRootName += "000000";
			} else if (frameCount < 100) {
				fileRootName += "00000";
			} else if (frameCount < 1000) {
				fileRootName += "0000";
			} else if (frameCount < 10000) {
				fileRootName += "000";
			} else if (frameCount < 100000) {
				fileRootName += "00";
			} else if (frameCount < 1000000) {
				fileRootName += "0";
			}

			saveFrame(fileRootName + frameCount + ".png");
		}
	}

	/**
	 * Executes the Processing sketch
	 * 
	 * @param args arguments to be passed to the sketch
	 */
	static public void main(String[] args) {
		String[] sketchName = new String[] { OilPaintingSketch.class.getName() };

		if (args != null) {
			PApplet.main(concat(sketchName, args));
		} else {
			PApplet.main(sketchName);
		}
	}
}
