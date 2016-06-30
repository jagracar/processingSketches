package oilPainting;

import java.util.Arrays;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.video.Capture;

/**
 * This sketch takes pictures using the webcam and simulates an oil paint. It has many optional parameters, but only
 * some combinations produce optimal results.
 * 
 * Inspired on some of the works by Sergio Albiac.
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class WebcamOilPaintSketch extends PApplet {
	// The webcam frame width to use
	private int webcamWidth = 640;
	// The webcam frame height to use
	private int webcamHeight = 480;
	// The webcam frame rate to use
	private int webcamFrameRate = 30;
	// The directory where the output files should be saved
	private String outputDir = "src/oilPainting/webcamOut/";
	// The maximum RGB color difference to consider the pixel correctly painted
	private int[] maxColorDiff = new int[] { 40, 40, 40 };
	// Paint each picture with a clean canvas
	private boolean startWithCleanCanvas = true;
	// Save a picture of each oil paint
	private boolean savePicture = true;
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
	// The canvas background color
	private int backgroundColor = color(255);

	// Sketch variables
	private Capture webcam;
	private PImage webcamImg;
	private boolean displayWebcamOutput;
	private int imgWidth;
	private int imgHeight;
	private PGraphics canvas;
	private boolean[] similarColorPixels;
	private boolean[] visitedPixels;
	private int[] badPaintedPixels;
	private int nBadPaintedPixels;

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
		// Start the webcam
		webcam = new Capture(this, webcamWidth, webcamHeight, webcamFrameRate);
		webcam.start();
		webcamImg = null;
		displayWebcamOutput = true;

		// Set the frame image dimensions
		imgWidth = webcamWidth;
		imgHeight = webcamHeight;

		// Resize the sketch window
		surface.setResizable(true);
		surface.setSize(imgWidth, imgHeight);

		// Wait until the screen window has the correct size
		while (height != imgHeight) {
			// do nothing, just wait
		}

		// Sketch setup
		strokeCap(SQUARE);
		background(backgroundColor);
		frameRate(webcamFrameRate);

		// Create the canvas buffer
		canvas = createGraphics(imgWidth, imgHeight);

		// Canvas buffer setup
		// canvas.noSmooth();
		canvas.beginDraw();
		canvas.strokeCap(SQUARE);
		canvas.background(backgroundColor);
		canvas.endDraw();

		// Initialize the pixel arrays
		int nPixels = imgWidth * imgHeight;
		similarColorPixels = new boolean[nPixels];
		visitedPixels = new boolean[nPixels];
		badPaintedPixels = new int[nPixels];
		nBadPaintedPixels = nPixels;
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Display either the webcam output or the latest oil paint
		if (displayWebcamOutput) {
			webcamImg = getWebcamImage();
			image(webcamImg, 0, 0);
		} else {
			image(canvas, 0, 0);
		}
	}

	/**
	 * After the mouse is clicked, paint the latest webcam image or show the webcam output
	 */
	public void mouseClicked() {
		// Check if we were showing the webcam output
		if (displayWebcamOutput) {
			// Clean the canvas if necessary
			if (startWithCleanCanvas) {
				canvas.beginDraw();
				canvas.background(backgroundColor);
				canvas.endDraw();
			}

			// Create an oil paint of the webcam image
			createOilPaint();

			// Save the oil paint picture if necessary
			if (savePicture) {
				// Draw the canvas on the screen
				image(canvas, 0, 0);

				// Save the oil paint
				save(outputDir + "oilPaint-" + millis() + ".png");
			}
		}

		displayWebcamOutput = !displayWebcamOutput;
	}

	/**
	 * Obtains an image using the webcam
	 * 
	 * @return a webcam image
	 */
	private PImage getWebcamImage() {
		if (webcam.available() == true) {
			webcam.read();
		}

		return webcam.get();
	}

	/**
	 * Creates an oil paint from the current frame image
	 */
	private void createOilPaint() {
		// Load the frame image pixels. This way they will be available all the time
		webcamImg.loadPixels();

		// Reset the visited pixels array
		Arrays.fill(visitedPixels, false);

		// Loop until the painting is finished
		float averageBrushSize = max(smallerBrushSize, max(imgWidth, imgHeight) / 6.0f);
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
					println("Traces = " + nTraces + ", processing time = " + (millis() - startTime) / 1000.0f
							+ " seconds");

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
						// Create the trace starting from a bad painted pixel
						int pixel = badPaintedPixels[floor(random(nBadPaintedPixels))];
						startingPosition.set(pixel % imgWidth, pixel / imgWidth);
						trace = new Trace(this, startingPosition, nSteps, traceSpeed);

						// Check if it has a valid trajectory
						validTrajectory = trace.hasValidTrajectory(similarColorPixels, visitedPixels, webcamImg);

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
						if (trace.calculateColors(maxColorDiff, similarColorPixels, webcamImg, canvas,
								backgroundColor)) {
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

			// Check if we should stop painting because there are no more valid traces
			if (trace == null) {
				continuePainting = false;
			} else {
				// Paint the trace
				trace.paint(visitedPixels, imgWidth, imgHeight, canvas, true);
			}
		}
	}

	/**
	 * Updates the similar color and bad painted pixel arrays
	 */
	private void updatePixelArrays() {
		// Load the canvas buffer pixels
		canvas.loadPixels();

		// Update the arrays
		nBadPaintedPixels = 0;

		for (int pixel = 0, nPixels = canvas.pixels.length; pixel < nPixels; pixel++) {
			// Check if the pixel is well painted
			boolean wellPainted = false;
			int paintedCol = canvas.pixels[pixel];
			int originalCol = webcamImg.pixels[pixel];

			if (paintedCol != backgroundColor) {
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
	 * Executes the Processing sketch
	 * 
	 * @param args arguments to be passed to the sketch
	 */
	static public void main(String[] args) {
		String[] sketchName = new String[] { WebcamOilPaintSketch.class.getName() };

		if (args != null) {
			PApplet.main(concat(sketchName, args));
		} else {
			PApplet.main(sketchName);
		}
	}
}
