package oilPainting;

import java.util.Arrays;

import gifAnimation.GifMaker;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

/**
 * This sketch takes a set of pictures as input and simulates a sequence of oil paints. It has many optional parameters,
 * but only some combinations produce optimal results.
 * 
 * Inspired on some of the works by Sergio Albiac.
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class OilPaintingSketch extends PApplet {
	// The paths to the pictures that we want to paint
	private String[] pictureFiles = { "src/oilPainting/picture.jpg" };
	// The path to the picture that should be used as initial background
	private String backgroundPictureFile = null;
	// The directory where the output files should be saved
	private String outputDir = "src/oilPainting/out/";
	// The maximum RGB color difference to consider the pixel correctly painted
	private int[] maxColorDiff = new int[] { 40, 40, 40 };
	// The size reduction factor between the original images and the final painting
	private float sizeReductionFactor = 1.0f;
	// Use a separate canvas buffer for color mixing (a bit slower)
	private boolean useCanvas = false;
	// Paint each picture with a clean canvas
	private boolean startWithCleanCanvas = false;
	// Compare the oil paint with the original picture
	private boolean comparisonMode = false;
	// Show additional debug images
	private boolean debugMode = false;
	// Make a movie showing the painting in steps
	private boolean makeMovie = false;
	// Make a gif showing the painting in steps
	private boolean makeGif = false;
	// Paint the traces step by step, or in one go
	private boolean paintStepByStep = true;
	// Avoid painting on areas with the same color as the canvas background
	private boolean avoidBackgroundRegions = true;
	// Save a picture of the final frame of each picture paint
	private boolean saveFinalFramePicture = false;
	// The smaller brush size allowed
	private float smallerBrushSize = 4;
	// The brush size decrement ratio
	private float brushSizeDecrement = 1.3f;
	// The maximum number of invalid trajectories allowed before the brush size is reduced
	private int maxInvalidTrajectories = 5000;
	// The maximum number of invalid trajectories allowed for the smaller brush size before the painting is stopped
	private int maxInvalidTrajectoriesForSmallerSize = 10000;
	// The maximum number of invalid traces allowed before the brush size is reduced
	private int maxInvalidTraces = 250;
	// The maximum number of invalid traces allowed for the smaller brush size before the painting is stopped
	private int maxInvalidTracesForSmallerSize = 350;
	// The trace speed
	private float traceSpeed = 2;
	// The typical trace length, relative to the brush size
	private float relativeTraceLength = 2.3f;
	// The minimum trace length allowed
	private float minTraceLength = 16;
	// The screen and canvas background color
	private int backgroundColor = color(255);
	// The animation frame step between two movie frames
	private int movieFrameStep = 20;
	// The animation frame step between two gif frames
	private int gifFrameStep = 200;

	// Sketch variables
	private PImage originalImg;
	private int imgWidth;
	private int imgHeight;
	private int imgCounter;
	private PGraphics canvas;
	private boolean[] similarColorPixels;
	private boolean[] visitedPixels;
	private int[] badPaintedPixels;
	private int nBadPaintedPixels;
	private GifMaker gifMaker;
	private float averageBrushSize;
	private boolean continuePainting;
	private Trace trace;
	private int traceStep;
	private int nTraces;
	private int startTime;
	private int waitCounter;

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
		// Load the first image that we want to paint and resize it by the specified amount
		originalImg = loadImage(pictureFiles[0]);
		imgWidth = round(originalImg.width / sizeReductionFactor);
		imgHeight = round(originalImg.height / sizeReductionFactor);
		originalImg.resize(imgWidth, imgHeight);
		imgCounter = 1;

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

		// Wait until the screen window has the correct size
		while (height != imgHeight) {
			// Do nothing, just wait
		}

		// Sketch setup
		strokeCap(SQUARE);
		background(backgroundColor);
		frameRate(2000);

		// Create the canvas buffer
		canvas = null;

		if (useCanvas) {
			canvas = createGraphics(imgWidth, imgHeight);

			// Canvas buffer setup
			canvas.noSmooth();
			canvas.beginDraw();
			canvas.strokeCap(SQUARE);
			canvas.background(backgroundColor);
			canvas.endDraw();
		}

		// Initialize the pixel arrays
		int nPixels = imgWidth * imgHeight;
		similarColorPixels = new boolean[nPixels];
		visitedPixels = new boolean[nPixels];
		badPaintedPixels = new int[nPixels];
		nBadPaintedPixels = nPixels;

		// Create the gif maker object if we are making a gif
		if (makeGif) {
			gifMaker = new GifMaker(this, outputDir + "oilPaint.gif");
			gifMaker.setRepeat(0);
		}

		// Initialize the rest of the sketch variables
		averageBrushSize = max(smallerBrushSize, max(imgWidth, imgHeight) / 6.0f);
		continuePainting = true;
		trace = null;
		traceStep = 0;
		nTraces = 0;
		startTime = millis();
		waitCounter = 0;
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Check if we should start the animation adding an initial background picture
		if (frameCount == 1 && backgroundPictureFile != null) {
			paintBackgroundPicture();
		}

		// Check that the oil painting simulation from the current image didn't finish
		if (continuePainting) {
			// Get a new valid trace if we are not painting one already
			if (trace == null) {
				trace = getValidTrace();
				traceStep = 0;
			}

			// Check if we should stop painting the current picture because there are no more valid traces
			if (trace == null) {
				continuePainting = false;

				// Save the final frame picture
				if (saveFinalFramePicture) {
					save(outputDir + "oilPaint-" + frameCount + ".png");
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
		} else if (imgCounter < pictureFiles.length) {
			// Wait some frames before starting to paint the next picture
			if (waitCounter < 60 * movieFrameStep) {
				waitCounter++;
			} else {
				// Load the next image that we want to paint and resize it by the specified amount
				originalImg = loadImage(pictureFiles[imgCounter]);
				originalImg.resize(imgWidth, imgHeight);
				imgCounter++;

				// Load the original image pixels. This way they will be available all the time
				originalImg.loadPixels();

				// Clean the screen and the canvas if necessary
				if (startWithCleanCanvas) {
					background(backgroundColor);

					if (useCanvas) {
						canvas.beginDraw();
						canvas.background(backgroundColor);
						canvas.endDraw();
					}
				}

				// Reset the visited pixels array
				Arrays.fill(visitedPixels, false);

				// Initialize the rest of the sketch variables
				averageBrushSize = max(smallerBrushSize, max(imgWidth, imgHeight) / 6.0f);
				continuePainting = true;
				trace = null;
				traceStep = 0;
				nTraces = 0;
				startTime = millis();
				waitCounter = 0;
			}
		} else {
			// Stop the sketch if we are not making a movie
			if (!makeMovie) {
				noLoop();
			}

			// Close the gif maker object if we are making a gif
			if (makeGif) {
				gifMaker.finish();
				gifMaker = null;
			}
		}

		// Save the movie frame
		if (makeMovie) {
			saveMovieFrame();
		}

		// Save the gif frame
		if (makeGif) {
			saveGifFrame();
		}
	}

	/**
	 * Paints the background picture on the screen and the canvas
	 */
	private void paintBackgroundPicture() {
		// Load and resize the background img
		PImage backgroundImg = loadImage(backgroundPictureFile);
		backgroundImg.resize(imgWidth, imgHeight);

		// Paint it on the screen and the canvas
		image(backgroundImg, 0, 0);

		if (useCanvas) {
			canvas.beginDraw();
			canvas.image(backgroundImg, 0, 0);
			canvas.endDraw();
		}
	}

	/**
	 * Obtains a valid trace, ready to be painted
	 * 
	 * @return the valid trace. null if there are no more valid traces and the paint can be considered finished.
	 */
	private Trace getValidTrace() {
		// Update the similar color and bad painted pixel arrays
		updatePixelArrays();

		// Mask background regions if necessary
		if (avoidBackgroundRegions && nTraces == 0) {
			maskBackgroundRegions();
		}

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
				println("Total number of painted traces: " + nTraces);
				println("Processing time = " + (millis() - startTime) / 1000.0f + " seconds");

				// Stop the while loop
				trace = null;
				traceNotFound = false;
			} else {
				// Change the average brush size if there were too many invalid traces
				if (averageBrushSize > smallerBrushSize && (invalidTrajectoriesCounter > maxInvalidTrajectories
						|| invalidTracesCounter > maxInvalidTraces)) {
					averageBrushSize = max(smallerBrushSize,
							min(averageBrushSize / brushSizeDecrement, averageBrushSize - 2));

					println("Frame = " + frameCount + ", traces = " + nTraces + ", new average brush size = "
							+ averageBrushSize);

					// Reset some of the variables
					invalidTrajectoriesCounter = 0;
					invalidTracesCounter = 0;

					// Reset the visited pixels array
					Arrays.fill(visitedPixels, false);

					// Mask background regions if necessary
					if (avoidBackgroundRegions) {
						maskBackgroundRegions();
					}
				}

				// Create new traces until one of them has a valid trajectory or we exceed a number of tries
				boolean validTrajectory = false;
				float brushSize = max(smallerBrushSize, averageBrushSize * random(0.95f, 1.05f));
				int nSteps = (int) (max(minTraceLength, relativeTraceLength * brushSize * random(0.9f, 1.1f))
						/ traceSpeed);

				while (!validTrajectory && invalidTrajectoriesCounter % 500 != 499) {
					// Create the trace starting from a bad painted pixel
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
					if (trace.calculateColors(maxColorDiff, similarColorPixels, originalImg, canvas, backgroundColor)) {
						// Test passed, the trace is good enough to be painted
						traceNotFound = false;
						nTraces++;
					} else {
						// The trace is not good enough, try again in the next loop step
						invalidTracesCounter++;
					}
				} else {
					// The trace is not good enough, try again in the next loop step
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
	private void updatePixelArrays() {
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
				int originalCol = originalImg.pixels[imgPixel];

				if (paintedCol != backgroundColor) {
					wellPainted = abs(((originalCol >> 16) & 0xff) - ((paintedCol >> 16) & 0xff)) < maxColorDiff[0]
							&& abs(((originalCol >> 8) & 0xff) - ((paintedCol >> 8) & 0xff)) < maxColorDiff[1]
							&& abs((originalCol & 0xff) - (paintedCol & 0xff)) < maxColorDiff[2];
				} else if (originalCol == backgroundColor) {
					wellPainted = avoidBackgroundRegions;
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
	 * Masks all pixels with a color that is equal to the canvas background color. The mask is applied to the visited
	 * pixels array.
	 */
	private void maskBackgroundRegions() {
		// Load the screen pixels
		loadPixels();

		// Mask all pixels in the canvas and the original image with a color equal to the background color
		for (int x = 0; x < imgWidth; x++) {
			for (int y = 0; y < imgHeight; y++) {
				int imgPixel = x + y * imgWidth;

				if (pixels[x + y * width] == backgroundColor && originalImg.pixels[imgPixel] == backgroundColor) {
					visitedPixels[imgPixel] = true;
				}
			}
		}

		// Update the screen pixels
		updatePixels();
	}

	/**
	 * Draws on the screen the visited pixels and similar color pixels arrays
	 */
	private void drawDebugImages() {
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
	private void saveMovieFrame() {
		// Trick to make the Movie Maker tool work when we don't paint anything
		if (!continuePainting) {
			fill(color(random(250, 255)));
			ellipse(0, 0, 2, 2);
		}

		// Make the frame counter start from zero
		int frame = frameCount - 1;

		// Save a picture after a given number of frames
		if (frame % movieFrameStep == 0) {
			String fileRootName = outputDir;

			if (frame < 10) {
				fileRootName += "000000";
			} else if (frame < 100) {
				fileRootName += "00000";
			} else if (frame < 1000) {
				fileRootName += "0000";
			} else if (frame < 10000) {
				fileRootName += "000";
			} else if (frame < 100000) {
				fileRootName += "00";
			} else if (frame < 1000000) {
				fileRootName += "0";
			}

			saveFrame(fileRootName + frame + ".png");
		}
	}

	/**
	 * Saves the current gif frame
	 */
	private void saveGifFrame() {
		if (gifMaker != null && (frameCount - 1) % gifFrameStep == 0) {
			gifMaker.setDelay(1);
			gifMaker.addFrame();
		}
	}

	/**
	 * Saves an screenshot of the current painting state each time the mouse is clicked
	 */
	public void mouseClicked() {
		saveFrame(outputDir + "screenshot" + millis() + ".png");
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
