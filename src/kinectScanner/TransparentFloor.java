package kinectScanner;

import processing.core.PApplet;
import processing.core.PImage;

/*
 * TransparentFloor class
 *
 * Just a simple class for the floor. 
 */

public class TransparentFloor {
	public float xMin, xMax, yMin, yMax, zMin, zMax;
	public PImage img;

	//
	// Constructor
	//

	public TransparentFloor(PApplet p, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax,
			int col) {
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
		this.zMin = zMin;
		this.zMax = zMax;

		// Create the image for the texture
		this.img = p.createImage(10, 50, PApplet.ARGB);
		this.img.loadPixels();
		for (int y = 0; y < img.height; y++) {
			for (int x = 0; x < img.width; x++) {
				float gradientAlpha = PApplet.map(y, 0, img.height, 150, 0);
				img.pixels[x + y * img.width] = p.color(p.red(col), p.green(col), p.blue(col), gradientAlpha);
			}
		}
		img.updatePixels();
	}

	//
	// Class Methods
	//

	// paint
	public void paint(PApplet p) {
		p.noStroke();
		p.beginShape();
		p.texture(img);
		p.vertex(xMin, yMin, zMin - 0.25f * (zMax - zMin), 0, 0);
		p.vertex(xMax, yMin, zMin - 0.25f * (zMax - zMin), img.width, 0);
		p.vertex(xMax, yMin, zMax + 0.5f * (zMax - zMin), img.width, img.height);
		p.vertex(xMin, yMin, zMax + 0.5f * (zMax - zMin), 0, img.height);
		p.endShape();
	}
}