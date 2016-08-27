package kinectScanner;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * Class used to represent the sketch floor
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class Floor {

	/**
	 * The texture used to paint the floor
	 */
	private PImage floorTexture;

	/**
	 * Constructor
	 * 
	 * @param p the parent Processing applet
	 * @param color the floor color
	 */
	public Floor(PApplet p, int color) {
		this.floorTexture = p.createImage(10, 50, PApplet.ARGB);

		// Set the texture pixel colors
		float redColor = p.red(color);
		float greenColor = p.green(color);
		float blueColor = p.blue(color);

		this.floorTexture.loadPixels();

		for (int y = 0; y < floorTexture.height; y++) {
			float alpha = 150f * y / floorTexture.height;
			int rowColor = p.color(redColor, greenColor, blueColor, alpha);

			for (int x = 0; x < floorTexture.width; x++) {
				floorTexture.pixels[x + y * floorTexture.width] = rowColor;
			}
		}

		this.floorTexture.updatePixels();
	}

	/**
	 * 
	 * @param p the parent Processing sketch
	 * @param limits the sketch visibility limits
	 */
	public void paint(PApplet p, PVector[] limits) {
		float zStart = limits[0].z - 0.25f * (limits[1].z - limits[0].z);
		float zEnd = limits[1].z + 0.5f * (limits[1].z - limits[0].z);

		p.noStroke();
		p.beginShape();
		p.texture(floorTexture);
		p.vertex(limits[0].x, limits[0].y, zEnd, 0, 0);
		p.vertex(limits[1].x, limits[0].y, zEnd, floorTexture.width, 0);
		p.vertex(limits[1].x, limits[0].y, zStart, floorTexture.width, floorTexture.height);
		p.vertex(limits[0].x, limits[0].y, zStart, 0, floorTexture.height);
		p.endShape();
	}
}