package oilPainting;

import java.util.Arrays;

import gifAnimation.GifMaker;
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
	private String movieFile = "src/oilPainting/test1.avi";
	// The path to the picture that should be used as initial background
	private String backgroundPictureFile = null;
	// The directory where the output files should be saved
	private String outputDir = "src/oilPainting/out/";
	// The maximum RGB color difference to consider the pixel correctly painted
	private int[] maxColorDiff = new int[] { 55, 55, 55 };
	// The size reduction factor between the original movie and the final animation
	private float sizeReductionFactor = 1.0f;
	// The movie frame where the animation should start
	private int startingFrame = 508;
	// The movie frame step between two animation frames
	private int animationFrameStep = 2;
	// Compare the animation with the original movie
	private boolean comparisonMode = true;
	// Make a movie showing the animation
	private boolean makeMovie = false;
	// Make a gif showing the animation
	private boolean makeGif = true;
	// Avoid painting on areas with the same color as the canvas background
	private boolean avoidBackgroundRegions = false;
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
	private Movie movie;
	private int movieFrame;
	private PImage frameImg;
	private int imgWidth;
	private int imgHeight;
	private PGraphics canvas;
	private boolean[] similarColorPixels;
	private boolean[] visitedPixels;
	private int[] badPaintedPixels;
	private int nBadPaintedPixels;
	private GifMaker gifMaker;

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
		movieFrame = startingFrame;
		frameImg = getFrameImage(movieFrame);
		imgWidth = round(frameImg.width / sizeReductionFactor);
		imgHeight = round(frameImg.height / sizeReductionFactor);

		// Resize the sketch window
		surface.setResizable(true);

		if (comparisonMode) {
			surface.setSize(imgWidth, 2 * imgHeight);
		} else {
			surface.setSize(imgWidth, imgHeight);
		}

		// Wait until the screen window has the correct size
		while (width != imgWidth) {
			// do nothing, just wait
		}

		// Sketch setup
		strokeCap(SQUARE);
		background(backgroundColor);
		frameRate(60);

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

		// Create the gif maker object if we are making a gif
		if (makeGif) {
			gifMaker = new GifMaker(this, outputDir + "oilPaint.gif");
			gifMaker.setRepeat(0);
		}
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Check if we should start the animation adding an initial background picture
		if (frameCount == 1 && backgroundPictureFile != null) {
			paintBackgroundPicture();
		}

		// Get the new frame image
		frameImg = getFrameImage(movieFrame);

		// Check that we obtained the correct frame, otherwise we will try again in the next draw step
		if (round(movie.time() * movie.frameRate) == movieFrame) {
			// Resize the image to the animation dimensions
			frameImg.resize(imgWidth, imgHeight);

			// Create an oil paint of the frame image
			createOilPaint();

			// Draw the result on the screen
			image(canvas, 0, 0);

			// Draw the original frame image if necessary
			if (comparisonMode) {
				image(frameImg, 0, imgHeight);
			}

			// Save the movie frame
			if (makeMovie) {
				saveMovieFrame();
			}

			// Save the gif frame
			if (makeGif) {

				if (movieFrame > 582 && gifMaker != null) {
					gifMaker.finish();
					gifMaker = null;
				} else {
					saveGifFrame();
				}
			}

			// Advance to the next animation frame
			movieFrame += animationFrameStep;
		}
	}

	/**
	 * Obtains an image of the movie at the frame position
	 * 
	 * @param frame the frame position
	 * @return an image of the movie at the frame position
	 */
	private PImage getFrameImage(int frame) {
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
	 * Paints the background picture on the canvas
	 */
	private void paintBackgroundPicture() {
		// Load and resize the background img
		PImage backgroundImg = loadImage(backgroundPictureFile);
		backgroundImg.resize(imgWidth, imgHeight);

		// Paint it on the canvas
		canvas.beginDraw();
		canvas.image(backgroundImg, 0, 0);
		canvas.endDraw();
	}

	/**
	 * Creates an oil paint from the current frame image
	 */
	private void createOilPaint() {
		// Load the frame image pixels. This way they will be available all the time
		frameImg.loadPixels();

		// Reset the visited pixels array
		Arrays.fill(visitedPixels, false);

		// Mask background regions if necessary
		if (avoidBackgroundRegions) {
			maskBackgroundRegions();
		}

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
					println("Frame = " + movieFrame + ", traces = " + nTraces + ", processing time = "
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
						if (trace.calculateColors(maxColorDiff, similarColorPixels, frameImg, canvas,
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
			int originalCol = frameImg.pixels[pixel];

			if (paintedCol != backgroundColor) {
				wellPainted = abs(((originalCol >> 16) & 0xff) - ((paintedCol >> 16) & 0xff)) < maxColorDiff[0]
						&& abs(((originalCol >> 8) & 0xff) - ((paintedCol >> 8) & 0xff)) < maxColorDiff[1]
						&& abs((originalCol & 0xff) - (paintedCol & 0xff)) < maxColorDiff[2];
			} else if (originalCol == backgroundColor) {
				wellPainted = avoidBackgroundRegions;
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
	 * Masks all pixels with a color that is equal to the canvas background color. The mask is applied to the visited
	 * pixels array.
	 */
	private void maskBackgroundRegions() {
		// Load the canvas buffer pixels
		canvas.loadPixels();

		// Mask all pixels in the canvas and the original image with a color equal to the background color
		for (int pixel = 0, nPixels = canvas.pixels.length; pixel < nPixels; pixel++) {
			if (canvas.pixels[pixel] == backgroundColor && frameImg.pixels[pixel] == backgroundColor) {
				visitedPixels[pixel] = true;
			}
		}

		// Update the canvas buffer pixels
		canvas.updatePixels();
	}

	/**
	 * Saves the movie frames with a format that can be processed with the movie maker tool
	 */
	private void saveMovieFrame() {
		String fileRootName = outputDir;

		if (movieFrame < 10) {
			fileRootName += "000000";
		} else if (movieFrame < 100) {
			fileRootName += "00000";
		} else if (movieFrame < 1000) {
			fileRootName += "0000";
		} else if (movieFrame < 10000) {
			fileRootName += "000";
		} else if (movieFrame < 100000) {
			fileRootName += "00";
		} else if (movieFrame < 1000000) {
			fileRootName += "0";
		}

		saveFrame(fileRootName + movieFrame + ".png");
	}

	/**
	 * Saves the current gif frame
	 */
	private void saveGifFrame() {
		if (gifMaker != null) {
			gifMaker.setDelay(1);
			gifMaker.addFrame();
		}
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
