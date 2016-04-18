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
	private String movieFile = "src/oilPainting/vtest.avi";
	// The directory where the animation frames should be saved
	private String frameDir = "src/oilPainting/frames/";
	// The maximum RGB color difference to consider the pixel correctly painted
	private float[] maxColorDiff = new float[] { 40, 40, 40 };
	// Compare the animation with the original movie
	private boolean comparisonMode = false;
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
	// The screen background color
	private int bgColor = color(255);

	// Sketch variables
	private Movie movie;
	private PImage frameImg;
	private int imgWidth;
	private int imgHeight;
	private PGraphics canvas;
	private boolean[] similarColor;
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
		// Make the window resizable
		surface.setResizable(true);

		// Load the movie
		movie = new Movie(this, movieFile);

		// Read the first frame to set the frame image dimensions
		movie.play();
		movie.pause();
		movie.read();
		imgWidth = movie.width / 2;
		imgHeight = movie.height / 2;

		// Create the canvas buffer
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
		} else {
			surface.setSize(imgWidth, imgHeight);
		}

		// Sketch setup
		background(bgColor);

		// Canvas buffer setup
		canvas.beginDraw();
		canvas.background(bgColor);
		canvas.strokeCap(SQUARE);
		canvas.endDraw();

		// Move the video to the frame counter starting position
		frameImg = null;
		frameCounter = 0;
		movie.play();
		movie.jump(frameCounter / movie.frameRate);
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Wait until there is a new frame image available
		if (frameImg != null) {
			// Pause the movie
			movie.pause();

			// Create a new oil paint of that frame image
			createOilPaint();

			// Draw the result on the screen
			image(canvas, 0, 0);

			if (comparisonMode) {
				image(frameImg, imgWidth, 0);
			}

			// Move to the next movie frame
			frameImg = null;
			frameCounter++;
			movie.play();
			movie.jump(frameCounter / movie.frameRate);
		}
	}

	/**
	 * Executes each time there is a movie event
	 * 
	 * @param mov the movie that produced the event
	 */
	public void movieEvent(Movie mov) {
		if (frameImg == null && abs(frameCounter - mov.time() * mov.frameRate) < 0.1) {
			// Copy the current frame into the frame image
			mov.read();
			frameImg = mov.get();
		}
	}

	/**
	 * Creates an oil paint from the current frame image
	 */
	public void createOilPaint() {
		// Resize the image to have the canvas dimensions
		frameImg.resize(imgWidth, imgHeight);

		// Load the frame image pixels. This way they will be all the time available
		frameImg.loadPixels();

		// Update the similar color and bad painted pixel arrays
		updatePixelArrays();

		// Reset the visited pixels array
		Arrays.fill(visitedPixels, false);

		// Loop until the painting is finished
		float averageBrushSize = max(smallerBrushSize, sqrt(imgWidth * imgHeight) / 6);
		Trace trace = null;
		int nTraces = 0;
		int invalidTracesCounter = 0;
		boolean newTrace = true;
		int startTime = millis();
		boolean continuePainting = true;

		while (continuePainting) {
			// Create a new trace or paint the current one
			if (newTrace) {
				// Check if we should stop painting
				if (averageBrushSize == smallerBrushSize && invalidTracesCounter > maxInvalidTracesForSmallerSize) {
					println("Frame = " + frameCounter + ", traces = " + nTraces + ", processing time = "
							+ (millis() - startTime) / 1000.0f + " seconds");
					continuePainting = false;
				} else {
					// Change the average brush size if there were too many invalid traces
					if (averageBrushSize > smallerBrushSize && invalidTracesCounter > maxInvalidTraces) {
						averageBrushSize = max(smallerBrushSize,
								min(averageBrushSize / brushSizeDecrement, averageBrushSize - 2));

						// Reset some of the sketch variables
						invalidTracesCounter = 0;
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
					if (trace.calculateColors(maxColorDiff, similarColor, frameImg, canvas, bgColor)) {
						// Test passed, the trace is good enough to be painted
						newTrace = false;
						nTraces++;
						invalidTracesCounter = 0;
					} else {
						// The trace is not good enough, try again in the next loop
						invalidTracesCounter++;
					}
				}
			} else {
				// Paint the trace
				trace.paint(visitedPixels, imgWidth, imgHeight, canvas, true);
				newTrace = true;

				// Update the pixel arrays
				updatePixelArrays();
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
				int originalCol = frameImg.pixels[pixel];
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
