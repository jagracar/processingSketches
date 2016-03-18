package photoSlices;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PImage;

/**
 * Photo slices processing sketch
 * 
 * @author jagracar
 */
public class PhotoSlicesSketch extends PApplet {
	PImage img;
	int sliceSize;
	ArrayList<Slice> slices;

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

		// Load the picture by Sukanto Debnath
		// https://www.flickr.com/photos/sukanto_debnath/2354607553
		// img = loadImage("http://farm3.staticflickr.com/2340/2354607553_9996a0c8fc.jpg");
		img = loadImage("src/photoSlices/picture.jpg");

		// Resize the window
		surface.setSize((int) (1.5 * img.width), img.height);

		// Obtain the image slices
		sliceSize = 8;
		slices = createSlices(sliceSize);
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Clean the window
		background(0);

		// Update the slices positions and paint them on the screen
		for (Slice slice : slices) {
			slice.update();
			slice.paint();
		}

		// Check if the user pushed the slices
		if (mouseX >= 0 && mouseX <= width && mouseY >= 0 && mouseY < height && abs(mouseX - pmouseX) > 2) {
			float pushVel = mouseX - pmouseX > 0 ? 5 : -5;

			for (Slice slice : slices) {
				slice.checkPush(mouseX, pushVel);
			}
		}
	}

	/**
	 * Creates the image slices
	 * 
	 * @param size the individual slice size
	 * @return the image slices array
	 */
	ArrayList<Slice> createSlices(int size) {
		int nSlices = floor(img.width / size);
		ArrayList<Slice> slicesArray = new ArrayList<Slice>(nSlices);

		for (int i = 0; i < nSlices; i++) {
			slicesArray.add(new Slice(this, i * size, size, img));
		}

		return slicesArray;
	}

	/**
	 * Changes the slice size and creates a new set of slices, each time the mouse is pressed
	 */
	public void mousePressed() {
		// Decrease the slice size by a factor of 2
		sliceSize /= 2;

		// If the size is too small, set it to a larger value
		if (sliceSize < 2) {
			sliceSize = 32;
		}

		// Create the new slices
		slices = createSlices(sliceSize);
	}

	/**
	 * Executes the Processing sketch
	 * 
	 * @param args arguments to be passed to the sketch
	 */
	static public void main(String[] args) {
		String[] sketchName = new String[] { PhotoSlicesSketch.class.getName() };

		if (args != null) {
			PApplet.main(concat(sketchName, args));
		} else {
			PApplet.main(sketchName);
		}
	}
}
