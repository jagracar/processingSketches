package kinectScanner;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * Simple class to work with moving images
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class MovingImg {

	private PImage img;
	public PVector position;
	public PVector velocity;
	public float angle;
	public boolean visible;

	/**
	 * Constructor
	 * 
	 * @param img the image
	 */
	public MovingImg(PImage img) {
		this.img = img;
		this.position = new PVector();
		this.velocity = new PVector();
		this.angle = 0;
		this.visible = true;
	}

	/**
	 * Updates the image position
	 */
	public void update() {
		position.add(velocity);
	}

	/**
	 * Checks if the image is close to a given position
	 * 
	 * @param pos the position to check
	 */
	public boolean closeToPosition(PVector pos) {
		return position.dist(pos) < 100;
	}

	/**
	 * Paints the image on the screen
	 * 
	 * @param p the parent Processing applet
	 */
	void paint(PApplet p) {
		if (visible) {
			p.pushStyle();
			p.imageMode(PApplet.CENTER);
			p.pushMatrix();
			p.translate(position.x, position.y, position.z);
			p.rotateZ(angle);
			p.image(img, 0, 0);
			p.popMatrix();
			p.popStyle();
		}
	}
}