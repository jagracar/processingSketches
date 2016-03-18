package photoSlices;

import processing.core.PApplet;
import processing.core.PImage;

/**
 * The slice class
 * 
 * @author jagracar
 */
public class Slice {
	PApplet applet;
	PImage imgSlice;
	float xWithoutNoise;
	float x;
	float vel;
	float noiseDelta;
	float noiseSmallDelta;

	/**
	 * Constructor
	 * 
	 * @param applet the sketch applet
	 * @param xImg the slice x position in the image
	 * @param size the slice size
	 * @param img the image to slice
	 */
	Slice(PApplet applet, int xImg, int size, PImage img) {
		this.applet = applet;

		// Obtain the slice image
		imgSlice = img.get(xImg, 0, size, img.height);

		// Calculate the slice position and velocity
		xWithoutNoise = (applet.width - img.width) / 2 + xImg;
		x = xWithoutNoise;
		vel = 0;

		// Define the noise properties
		noiseDelta = applet.random(100);
		noiseSmallDelta = applet.random(100);
	}

	/**
	 * Updates the slice position and velocity
	 */
	public void update() {
		// Move the global position
		xWithoutNoise += vel;

		// Add the noise
		x = xWithoutNoise + 400 * (applet.noise(noiseDelta) - 0.5f) + 200 * (applet.noise(noiseSmallDelta) - 0.5f);
		noiseDelta += 0.002f;
		noiseSmallDelta += 0.002f;

		// Slow down the velocity
		vel *= 0.95;
	}

	/**
	 * Paints the slice on the screen
	 */
	public void paint() {
		applet.image(imgSlice, x, 0);
	}

	/**
	 * Checks if the slice have being pushed
	 * 
	 * @param xPush the push x position
	 * @param velPush the push velocity
	 */
	public void checkPush(float xPush, float velPush) {
		if (xPush >= x && xPush <= (x + imgSlice.width)) {
			vel = velPush;
		}
	}
}
