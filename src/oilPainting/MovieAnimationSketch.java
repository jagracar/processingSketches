package oilPainting;

import java.util.Arrays;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.video.Movie;

/**
 * This sketch takes a movie as input and creates an animation with an oil paint appearance. It has many optional
 * parameters, but only some combinations produce optimal results.
 * 
 * Inspired on some of the works by Sergio Albiac.
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class MovieAnimationSketch extends PApplet {
	// The path to the movie that we want to animate
	private final String movieFile = "src/oilPainting/vtest.avi";
	// The directory where the animation frames should be saved
	private final String frameDir = "src/oilPainting/frames/";
	// The maximum RGB color difference to consider the pixel correctly painted
	private final int[] maxColorDiff = new int[] { 40, 40, 40 };
	// The size reduction factor between the original movie and the final animation
	private final int sizeReductionFactor = 2;
	// Compare the animation with the original movie
	private final boolean comparisonMode = false;
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
	// The canvas background color
	private final int bgColor = color(255);

	// Sketch variables
	private Movie movie;
	private PImage frameImg;
	private int imgWidth;
	private int imgHeight;
	private float timeOffset;
	private PGraphics canvas;
	private boolean[] similarColorPixels;
	private boolean[] visitedPixels;
	private int[] badPaintedPixels;
	private int nBadPaintedPixels;
	private int frameCounter;

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
		// Load the movie that we want to animate
		movie = new Movie(this, movieFile);

		// Read the first frame to set the animation frame image dimensions
		frameImg = getFrameImage(0);
		imgWidth = frameImg.width / sizeReductionFactor;
		imgHeight = frameImg.height / sizeReductionFactor;

		// Get the time offset corresponding to the initial frame
		timeOffset = movie.time();

		// Resize the sketch window
		surface.setResizable(true);

		if (comparisonMode) {
			surface.setSize(2 * imgWidth, imgHeight);
		} else {
			surface.setSize(imgWidth, imgHeight);
		}

		// Sketch setup
		strokeCap(SQUARE);
		background(bgColor);
		frameRate(30);

		// Create the canvas buffer
		canvas = createGraphics(imgWidth, imgHeight);

		// Canvas buffer setup
		//canvas.noSmooth();
		canvas.beginDraw();
		canvas.strokeCap(SQUARE);
		canvas.background(bgColor);
		canvas.endDraw();

		// Initialize the pixel arrays
		int nPixels = imgWidth * imgHeight;
		similarColorPixels = new boolean[nPixels];
		visitedPixels = new boolean[nPixels];
		badPaintedPixels = new int[nPixels];
		nBadPaintedPixels = nPixels;

		// Set the starting frame
		frameCounter = 500;
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Get the new frame image
		frameImg = getFrameImage(frameCounter);

		// Check that we obtained the correct frame
		if (round((movie.time() - timeOffset) * movie.frameRate) == frameCounter) {
			// Resize the image to the canvas dimensions
			frameImg.resize(imgWidth, imgHeight);

			// Create an oil paint of the frame image
			createOilPaint();

			// Draw the result on the screen
			image(canvas, 0, 0);

			if (comparisonMode) {
				image(frameImg, imgWidth, 0);
			}

			// Advance to the next movie frame
			frameCounter++;
		}
	}

	/**
	 * Obtains an image of the movie at the frame position
	 * 
	 * @param frame the frame position
	 * @return an image of the movie at the frame position
	 */
	public PImage getFrameImage(int frame) {
		// Move the movie to the given frame position
		movie.play();
		movie.jump((frame + 0.5f) / movie.frameRate);
		movie.pause();

		// Wait until the frame is available
		while (!movie.available()) {
			// do nothing
		}

		// Read the frame
		movie.read();

		// Return the frame image
		return movie.get();
	}

	/**
	 * Creates an oil paint from the current frame image
	 */
	public void createOilPaint() {
		// Load the frame image pixels. This way they will be available all the time
		frameImg.loadPixels();

		// Reset the visited pixels array
		Arrays.fill(visitedPixels, false);

		// Loop until the painting is finished
		float averageBrushSize = max(smallerBrushSize, sqrt(imgWidth * imgHeight) / 6);
		boolean continuePainting = true;
		int nTraces = 0;
		int startTime = millis();

		while (continuePainting) {
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
					println("Frame = " + frameCounter + ", traces = " + nTraces + ", processing time = "
							+ (millis() - startTime) / 1000.0f + " seconds");

					// Stop the inner while loop
					trace = null;
					traceNotFound = false;
				} else {
					// Change the average brush size if there were too many invalid traces
					if (averageBrushSize > smallerBrushSize && (invalidTrajectoriesCounter > maxInvalidTrajectories
							|| invalidTracesCounter > maxInvalidTraces)) {
						averageBrushSize = max(smallerBrushSize,
								min(averageBrushSize / brushSizeDecrement, averageBrushSize - 2));

						// Reset some of the variables
						invalidTrajectoriesCounter = 0;
						invalidTracesCounter = 0;

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
						validTrajectory = trace.hasValidTrajectory(similarColorPixels, visitedPixels, frameImg);

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
						if (trace.calculateColors(maxColorDiff, similarColorPixels, frameImg, canvas, bgColor)) {
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

			// Check if we should stop painting because there are no more valid traces
			if (trace == null) {
				continuePainting = false;
			} else {
				// Paint the trace step by step or in one go
				trace.paint(visitedPixels, imgWidth, imgHeight, canvas, true);
			}
		}
	}

	/**
	 * Updates the similar color and bad painted pixel arrays
	 */
	public void updatePixelArrays() {
		// Load the canvas buffer pixels
		canvas.loadPixels();
		int nPixels = canvas.pixels.length;

		// Update the arrays
		nBadPaintedPixels = 0;

		for (int pixel = 0; pixel < nPixels; pixel++) {
			// Check if the pixel is well painted
			boolean wellPainted = false;
			int paintedCol = canvas.pixels[pixel];

			if (paintedCol != bgColor) {
				int originalCol = frameImg.pixels[pixel];
				wellPainted = abs(((originalCol >> 16) & 0xff) - ((paintedCol >> 16) & 0xff)) < maxColorDiff[0]
						&& abs(((originalCol >> 8) & 0xff) - ((paintedCol >> 8) & 0xff)) < maxColorDiff[1]
						&& abs((originalCol & 0xff) - (paintedCol & 0xff)) < maxColorDiff[2];
			}

			similarColorPixels[pixel] = wellPainted;

			if (!wellPainted) {
				badPaintedPixels[nBadPaintedPixels] = pixel;
				nBadPaintedPixels++;
			}
		}

		// Update the canvas buffer pixels
		canvas.updatePixels();
	}

	/**
	 * Saves the movie frames with a format that can be processed with the movie maker tool
	 */
	public void saveMovieFrame() {
		String fileRootName = frameDir;

		if (frameCounter < 10) {
			fileRootName += "000000";
		} else if (frameCounter < 100) {
			fileRootName += "00000";
		} else if (frameCounter < 1000) {
			fileRootName += "0000";
		} else if (frameCounter < 10000) {
			fileRootName += "000";
		} else if (frameCounter < 100000) {
			fileRootName += "00";
		} else if (frameCounter < 1000000) {
			fileRootName += "0";
		}

		saveFrame(fileRootName + frameCounter + ".png");
	}

	/**
	 * Executes the Processing sketch
	 * 
	 * @param args arguments to be passed to the sketch
	 */
	static public void main(String[] args) {
		String[] sketchName = new String[] { MovieAnimationSketch.class.getName() };

		if (args != null) {
			PApplet.main(concat(sketchName, args));
		} else {
			PApplet.main(sketchName);
		}
	}
}
