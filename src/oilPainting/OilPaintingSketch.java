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
	// The picture file name
	private String pictureFile = "src/oilPainting/picture.jpg";
	// The maximum RGB color difference to consider the pixel correctly painted
	private float[] maxColorDiff = new float[] { 40, 40, 40 };
	// Compare the oil paint with the original picture
	private boolean comparisonMode = false;
	// Show additional debug images
	private boolean debugMode = false;
	// Make a movie showing the painting in steps
	private boolean makeMovie = false;
	// Draw the traces step by step, or in one go
	private boolean drawStepByStep = false;
	// Saves a picture of the final frame
	private boolean saveFinalFramePicture = false;
	// The smaller brush size allowed
	private float smallerBrushSize = 4;
	// The brush size decrement ratio
	private float brushSizeDecrement = 1.3f;
	// The maximum bristle length
	private float maxBristleLength = 12;
	// The maximum bristle thickness
	private float maxBristleThickness = 5;
	// The maximum number of invalid traces allowed before the brush size is reduced
	private int maxInvalidTraces = 250;
	// The maximum number of invalid traces allowed for the smaller brush size before the painting is stopped
	private int maxInvalidTracesForSmallerSize = 350;
	// The trace speed
	private float traceSpeed = 2;
	// The typical trace length, relative to the brush size
	private float relativeTraceLength = 2.3f;
	// The minimum trace length allowed
	private float minTraceLength = 8;
	// The screen and canvas background color
	private int bgColor = color(255);
	// The movie frame rate
	private int movieFrameRate = 20;

	// Sketch variables
	private PImage originalImg;
	private int imgWidth;
	private int imgHeight;
	private PGraphics canvas;
	private boolean[] similarColor;
	private boolean[] visitedPixels;
	private int[] badPaintedPixels;
	private int nBadPaintedPixels;
	private float averageBrushSize;
	private Trace trace;
	private int nTraces;
	private int invalidTracesCounter;
	private boolean newTrace;
	private boolean paint;
	private int startTime;
	private int lastChangeTime;
	private int lastChangeFrameCount;
	private int frameCounter;
	private int step;

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

		// Load the image that we want to paint
		originalImg = loadImage(pictureFile);
		imgWidth = originalImg.width;
		imgHeight = originalImg.height;

		// Load the original image pixels. This way they will be all the time available
		originalImg.loadPixels();

		// Create canvas buffer
		canvas = createGraphics(imgWidth, imgHeight);

		// Create the pixel arrays
		int nPixels = imgWidth * imgHeight;
		similarColor = new boolean[nPixels];
		visitedPixels = new boolean[nPixels];
		badPaintedPixels = new int[nPixels];
		nBadPaintedPixels = nPixels;

		for (int i = 0; i < nPixels; i++) {
			badPaintedPixels[i] = i;
		}

		// Resize the sketch window
		if (comparisonMode) {
			surface.setSize(2 * imgWidth, imgHeight);
		} else if (debugMode) {
			surface.setSize(3 * imgWidth, imgHeight);
		} else {
			surface.setSize(imgWidth, imgHeight);
		}

		// Sketch setup
		background(bgColor);
		strokeCap(SQUARE);
		frameRate(2000);

		// Canvas buffer setup
		canvas.noSmooth();
		canvas.beginDraw();
		canvas.background(bgColor);
		canvas.strokeCap(SQUARE);
		canvas.endDraw();

		// Initialize the rest of the sketch variables
		averageBrushSize = max(smallerBrushSize, sqrt(imgWidth * imgHeight) / 6);
		trace = null;
		nTraces = 0;
		invalidTracesCounter = 0;
		newTrace = true;
		paint = true;
		startTime = millis();
		lastChangeTime = startTime;
		lastChangeFrameCount = 0;
		frameCounter = 1;
		step = 0;
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Create a new trace or paint the current one
		if (newTrace) {
			// Check if we should stop painting
			if (averageBrushSize == smallerBrushSize && invalidTracesCounter > maxInvalidTracesForSmallerSize) {
				float totalTime = (millis() - startTime) / 1000.0f;

				println("Total number of painted traces: " + nTraces);
				println("Average frame rate = " + round(frameCount / totalTime));
				println("Processing time = " + totalTime + " seconds");

				// Stop painting traces
				newTrace = false;
				paint = false;

				// Save a picture of the final frame
				if (saveFinalFramePicture) {
					save("src/oilPainting/oilPaint-" + frameCount + ".png");
				}

				// Stop the sketch if we are not making a movie
				if (!makeMovie) {
					noLoop();
				}
			} else {
				// Change the average brush size if there were too many invalid traces
				if (averageBrushSize > smallerBrushSize && invalidTracesCounter > maxInvalidTraces) {
					averageBrushSize = max(smallerBrushSize,
							min(averageBrushSize / brushSizeDecrement, averageBrushSize - 2));
					float ellapsedTime = (millis() - lastChangeTime) / 1000.0f;

					println("Frame = " + frameCount + ", traces = " + nTraces + ", new average brush size = "
							+ averageBrushSize);
					println("Average frame rate = " + round((frameCount - lastChangeFrameCount) / ellapsedTime));
					println();

					// Reset some of the sketch variables
					invalidTracesCounter = 0;
					lastChangeTime = millis();
					lastChangeFrameCount = frameCount;

					// Reset the visited pixels array
					Arrays.fill(visitedPixels, false);
				}

				// Create new traces until one of them has a valid trajectory
				boolean validTrajectory = false;
				PVector initPosition = new PVector(0, 0);
				float brushSize = max(smallerBrushSize, averageBrushSize * random(0.95f, 1.05f));
				int nSteps = (int) max(minTraceLength, relativeTraceLength * brushSize / traceSpeed);

				while (!validTrajectory) {
					// Create the trace
					int pixel = badPaintedPixels[floor(random(nBadPaintedPixels))];
					initPosition.set(pixel % imgWidth, pixel / imgWidth);
					trace = new Trace(this, initPosition, nSteps, traceSpeed);

					// Check if it has a valid trajectory
					validTrajectory = trace.hasValidTrajectory(similarColor, visitedPixels, imgWidth, imgHeight);
				}

				// Create the brush
				int nBristles = (int) (brushSize * random(1.6f, 1.9f));
				float bristleLength = min(2 * brushSize, maxBristleLength);
				float bristleThickness = min(0.8f * brushSize, maxBristleThickness);
				Brush brush = new Brush(this, initPosition, brushSize, nBristles, bristleLength, bristleThickness);

				// Add the brush to the trace
				trace.setBrush(brush);

				// Calculate the trace colors and check that painting the trace will improve the painting
				if (trace.calculateColors(maxColorDiff, similarColor, originalImg, canvas, bgColor)) {
					// Test passed, the trace is good enough to be painted
					newTrace = false;
					nTraces++;
					invalidTracesCounter = 0;
					step = 0;
				} else {
					// The trace is not good enough, try again in the next loop
					invalidTracesCounter++;
				}
			}
		} else if (paint) {
			// Paint the trace step by step or in one go
			if (drawStepByStep) {
				trace.paint(step, visitedPixels, canvas);
				step++;

				if (step == trace.getNSteps()) {
					newTrace = true;
				}
			} else {
				trace.paint(visitedPixels, canvas);
				newTrace = true;
			}

			// Update the pixel arrays if we finished to paint the trace
			if (newTrace) {
				updatePixelArrays();
			}

			// Draw the additional images if necessary
			if (comparisonMode) {
				image(originalImg, imgWidth, 0);
			} else if (debugMode) {
				drawDebugImages();
			}

			// Save the frame if we are making a movie
			if (makeMovie) {
				saveMovieFrame();
			}
		} else {
			// Save the frame if we are making a movie
			if (makeMovie) {
				// Trick to make the Movie Maker tool work
				fill(color(random(250, 255)));
				ellipse(0, 0, 2, 2);

				saveMovieFrame();
			}
		}
	}

	/**
	 * Updates the similar color and bad painted pixel arrays
	 */
	public void updatePixelArrays() {
		// Load the canvas pixels
		canvas.loadPixels();
		int nPixels = canvas.pixels.length;

		// Update the arrays
		nBadPaintedPixels = 0;

		for (int pixel = 0; pixel < nPixels; pixel++) {
			// Check if the pixel is well painted
			boolean wellPainted = false;
			int canvasCol = canvas.pixels[pixel];

			if (canvasCol != bgColor) {
				int originalCol = originalImg.pixels[pixel];
				int rDiff = abs(((originalCol >> 16) & 0xff) - ((canvasCol >> 16) & 0xff));
				int gDiff = abs(((originalCol >> 8) & 0xff) - ((canvasCol >> 8) & 0xff));
				int bDiff = abs((originalCol & 0xff) - (canvasCol & 0xff));
				wellPainted = (rDiff < maxColorDiff[0]) && (gDiff < maxColorDiff[1]) && (bDiff < maxColorDiff[2]);
			}

			similarColor[pixel] = wellPainted;

			if (!wellPainted) {
				badPaintedPixels[nBadPaintedPixels] = pixel;
				nBadPaintedPixels++;
			}
		}

		// Update the canvas pixels
		canvas.updatePixels();
	}

	/**
	 * Draws on the screen the visited pixels and similar color arrays
	 */
	public void drawDebugImages() {
		// Load the screen pixels
		loadPixels();

		// Draw the arrays
		for (int x = 0; x < imgWidth; x++) {
			for (int y = 0; y < imgHeight; y++) {
				int pixel = x + y * imgWidth;
				pixels[x + y * width + imgWidth] = visitedPixels[pixel] ? 0xff000000 : 0xffffffff;
				pixels[x + y * width + 2 * imgWidth] = similarColor[pixel] ? 0xff000000 : 0xffffffff;
			}
		}

		// Update the screen pixels
		updatePixels();
	}

	/**
	 * Saves the movie frames with a format that can be processed with the movie maker tool
	 */
	public void saveMovieFrame() {
		if (frameCounter % movieFrameRate == 0) {
			String frameDir = "src/oilPainting/frames/";

			if (frameCounter < 10) {
				frameDir += "000000";
			} else if (frameCounter < 100) {
				frameDir += "00000";
			} else if (frameCounter < 1000) {
				frameDir += "0000";
			} else if (frameCounter < 10000) {
				frameDir += "000";
			} else if (frameCounter < 100000) {
				frameDir += "00";
			} else if (frameCounter < 1000000) {
				frameDir += "0";
			}

			saveFrame(frameDir + frameCounter + ".png");
		}

		// Increase the frame counter
		frameCounter++;
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
