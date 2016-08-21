package kinectScanner;

import java.awt.Rectangle;

import gab.opencv.OpenCV;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * ScanBox class
 *
 * Scans are taken inside this box
 *
 * Uses OpenCV 2 to center the box in the first detected face. If you don't have OpenCV installed in your computer, you
 * should comment the lines inside the centerInFace() function, but centerInFace() still needs to be defined.
 */
public class ScanBox {

	/**
	 * The box center
	 */
	public PVector center;

	/**
	 * The box size
	 */
	public float size;

	/**
	 * Constructor
	 * 
	 * @param center the box center
	 * @param size the box size
	 */
	public ScanBox(PVector center, float size) {
		this.center = center.copy();
		this.size = size;
	}

	/**
	 * Paints the box on the screen
	 * 
	 * @param p the Processing applet
	 */
	public void paint(PApplet p, int col) {
		p.pushStyle();
		p.noFill();
		p.stroke(col);
		p.strokeWeight(1);

		p.pushMatrix();
		p.translate(center.x, center.y, center.z);
		p.line(-size, 0, 0, size, 0, 0);
		p.line(0, -size, 0, 0, size, 0);
		p.line(0, 0, -size, 0, 0, size);
		p.box(size, size, size);
		p.popMatrix();

		p.popStyle();
	}

	/**
	 * Detects a face inside the provided Kinect points and centers the box on the face position
	 * 
	 * @param p the Processing applet
	 * @param kp the Kinect points used to detect the face
	 */
	public void centerInFace(PApplet p, KinectPoints kp) {
		// Create an image with only the visible points color information
		PImage img = p.createImage(kp.width, kp.height, PApplet.RGB);
		img.loadPixels();

		for (int i = 0; i < kp.nPoints; i++) {
			if (kp.visibilityMask[i]) {
				img.pixels[i] = kp.colors[i];
			}
		}

		img.updatePixels();

		// Initialize OpenCV
		OpenCV opencv = new OpenCV(p, img);
		opencv.loadCascade(OpenCV.CASCADE_FRONTALFACE);

		// Detect faces in the image
		Rectangle[] faces = opencv.detect();
		System.out.println("Center in face: " + faces.length + " face detected");

		// Check if a face was detected
		if (faces.length > 0) {
			// Get the first face 3D position
			Rectangle face = faces[0];
			int x = face.x + (int) (face.width / 2);
			int y = face.y + (int) (face.height / 2);
			int index = x + y * kp.width;
			
			if (kp.visibilityMask[index]) {
				center = kp.points[index].copy();
				center.add(0, 0, 100); // add some offset
				System.out.println("Center in face: Done (centered in the first face)");
			} else {
				System.out.println("Center in face: Invalid point. Try again");
			}
		} else {
			System.out.println("Center in face: Nothing done. Try again");
		}
	}
}