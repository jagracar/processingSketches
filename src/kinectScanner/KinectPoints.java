package kinectScanner;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * Simple class to manipulate and paint Kinect output data
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class KinectPoints {

	/**
	 * Maximum separation allowed between two consecutive points to consider them connected
	 */
	public static final float MAX_SEP_SQ = 90 * 90;

	/**
	 * Kinect arrays horizontal dimension
	 */
	public int width;

	/**
	 * Kinect arrays vertical dimension
	 */
	public int height;

	/**
	 * Total number of points in the arrays
	 */
	public int nPoints;

	/**
	 * Array containing the points coordinates
	 */
	public PVector[] points;

	/**
	 * Array containing the points colors
	 */
	public int[] colors;

	/**
	 * Array containing the points visibility mask
	 */
	public boolean[] visibilityMask;

	public KinectPoints() {
		// Empty constructor
	}

	/**
	 * Constructor
	 * 
	 * @param points Kinect 3D points
	 * @param rgbImg Kinect color image
	 * @param depthMap Kinect depth map
	 * @param reductionFactor scale reduction factor
	 */
	public KinectPoints(PVector[] points, PImage rgbImg, int[] depthMap, int reductionFactor) {
		reductionFactor = Math.max(1, reductionFactor);
		this.width = rgbImg.width / reductionFactor;
		this.height = rgbImg.height / reductionFactor;
		this.nPoints = this.width * this.height;
		this.points = new PVector[this.nPoints];
		this.colors = new int[this.nPoints];
		this.visibilityMask = new boolean[this.nPoints];

		// Populate the arrays
		rgbImg.loadPixels();

		for (int row = 0; row < this.height; row++) {
			for (int col = 0; col < this.width; col++) {
				int index = col + row * this.width;
				int indexOriginal = col * reductionFactor + row * reductionFactor * rgbImg.width;
				this.points[index] = points[indexOriginal].copy();
				this.colors[index] = rgbImg.pixels[indexOriginal];
				this.visibilityMask[index] = depthMap[indexOriginal] > 0;
			}
		}
	}

	/**
	 * Constructor
	 * 
	 * @param width arrays horizontal dimension
	 * @param height arrays vertical dimension
	 * @param points Kinect 3D points
	 * @param colors Kinect point colors
	 * @param visibilityMask Kinect points visibility mask
	 */
	public KinectPoints(int width, int height, PVector[] points, int[] colors, boolean[] visibilityMask) {
		this.width = width;
		this.height = height;
		this.nPoints = this.width * this.height;
		this.points = new PVector[this.nPoints];
		this.colors = new int[this.nPoints];
		this.visibilityMask = new boolean[this.nPoints];

		// Populate the arrays
		for (int i = 0; i < this.nPoints; i++) {
			this.points[i] = points[i].copy();
			this.colors[i] = colors[i];
			this.visibilityMask[i] = visibilityMask[i];
		}
	}

	/**
	 * Reduces the Kinect points resolution by a given factor
	 * 
	 * @param reductionFactor scale reduction factor
	 */
	public void reduceResolution(int reductionFactor) {
		if (reductionFactor > 1) {
			// Obtain the dimensions of the reduced arrays
			int widthRed = width / reductionFactor;
			int heightRed = height / reductionFactor;
			int nPointsRed = widthRed * heightRed;
			PVector[] pointsRed = new PVector[nPointsRed];
			int[] colorsRed = new int[nPointsRed];
			boolean[] visibilityMaskRed = new boolean[nPointsRed];

			// Populate the reduced arrays
			for (int row = 0; row < heightRed; row++) {
				for (int col = 0; col < widthRed; col++) {
					int indexRed = col + row * widthRed;
					int index = col * reductionFactor + row * reductionFactor * width;
					pointsRed[indexRed] = points[index].copy();
					colorsRed[indexRed] = colors[index];
					visibilityMaskRed[indexRed] = visibilityMask[index];
				}
			}

			// Update the arrays to the new resolution
			width = widthRed;
			height = heightRed;
			nPoints = nPointsRed;
			points = pointsRed;
			colors = colorsRed;
			visibilityMask = visibilityMaskRed;
		}
	}

	/**
	 * Constrains the points visibilities to a cube delimited by some lower and upper corner coordinates
	 * 
	 * @param xMin lower corner x coordinate
	 * @param xMax upper corner x coordinate
	 * @param yMin lower corner y coordinate
	 * @param yMax upper corner y coordinate
	 * @param zMin lower corner z coordinate
	 * @param zMax upper corner z coordinate
	 */
	public void constrainPoints(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
		for (int i = 0; i < nPoints; i++) {
			PVector point = points[i];
			visibilityMask[i] = visibilityMask[i] && (point.x > xMin) && (point.x < xMax) && (point.y > yMin)
					&& (point.y < yMax) && (point.z > zMin) && (point.z < zMax);
		}
	}

	/**
	 * Constrains the points visibilities to a cube delimited by some lower and upper corner coordinates
	 * 
	 * @param corners an array with the lower and upper corners
	 */
	public void constrainPoints(PVector[] corners) {
		constrainPoints(corners[0].x, corners[1].x, corners[0].y, corners[1].y, corners[0].z, corners[1].z);
	}

	/**
	 * Calculates the square of the distance between two points
	 * 
	 * @param point1 first point
	 * @param point2 second point
	 * @return the square of the distance between the two points
	 */
	private static float distanceSq(PVector point1, PVector point2) {
		float dx = point1.x - point2.x;
		float dy = point1.y - point2.y;
		float dz = point1.z - point2.z;

		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Returns true if the two points are close enough to be considered connected
	 * 
	 * @param point1 first point
	 * @param point2 second point
	 * @return true if the points can be considered connected
	 */
	private static boolean connected(PVector point1, PVector point2) {
		return KinectPoints.distanceSq(point1, point2) < MAX_SEP_SQ;
	}

	/**
	 * Draws the Kinect points as pixels on the screen
	 * 
	 * @param p the Processing applet
	 * @param pixelSize the pixel size
	 */
	public void drawAsPixels(PApplet p, int pixelSize) {
		p.pushStyle();
		p.strokeWeight(pixelSize);

		for (int i = 0; i < nPoints; i++) {
			if (visibilityMask[i]) {
				PVector point = points[i];
				p.stroke(colors[i]);
				p.point(point.x, point.y, point.z);
			}
		}

		p.popStyle();
	}

	/**
	 * Draws the Kinect points as pixels on the screen
	 * 
	 * @param p the Processing applet
	 * @param pixelSize the pixel size
	 * @param pixelColor the pixel color
	 */
	public void drawAsPixels(PApplet p, int pixelSize, int pixelColor) {
		p.pushStyle();
		p.strokeWeight(pixelSize);
		p.stroke(pixelColor);

		for (int i = 0; i < nPoints; i++) {
			if (visibilityMask[i]) {
				PVector point = points[i];
				p.point(point.x, point.y, point.z);
			}
		}

		p.popStyle();
	}

	/**
	 * Draws the Kinect points as horizontal bands on the screen
	 * 
	 * @param p the Processing applet
	 */
	public void drawAsBands(PApplet p) {
		p.pushStyle();
		p.noStroke();
		boolean bandStarted = false;

		for (int row = 0; row < height - 1; row++) {
			for (int col = 0; col < width; col++) {
				int index = col + row * width;

				// Check if the point is valid
				if (visibilityMask[index]) {
					PVector point = points[index];

					if (!bandStarted) {
						// Start a new band
						p.beginShape(PApplet.TRIANGLE_STRIP);
						p.fill(colors[index]);
						p.vertex(point.x, point.y, point.z);
						bandStarted = true;
					} else if (KinectPoints.connected(point, points[index - 1])) {
						p.fill(colors[index]);
						p.vertex(point.x, point.y, point.z);
					} else {
						p.endShape();
						bandStarted = false;

						// It's a good point, use it in the next loop as starting point for a new band
						col--;
						continue;
					}

					// Check if the lower point is valid
					int lowerIndex = index + width;

					if (visibilityMask[lowerIndex]) {
						PVector lowerPoint = points[lowerIndex];

						if (KinectPoints.connected(point, lowerPoint)) {
							p.fill(colors[lowerIndex]);
							p.vertex(lowerPoint.x, lowerPoint.y, lowerPoint.z);
						} else {
							p.fill(colors[index]);
							p.vertex(point.x, point.y, point.z);
						}
					} else {
						p.fill(colors[index]);
						p.vertex(point.x, point.y, point.z);
					}

					// Finish the band if it is the last point in the row
					if (col == width - 1) {
						p.endShape();
						bandStarted = false;
					}
				} else if (bandStarted) {
					// The point is not valid, let's see if we can use the lower point for the last point in the band
					int lowerIndex = index + width;

					if (visibilityMask[lowerIndex]) {
						PVector lowerPoint = points[lowerIndex];

						if (KinectPoints.connected(lowerPoint, points[index - 1])) {
							p.fill(colors[lowerIndex]);
							p.vertex(lowerPoint.x, lowerPoint.y, lowerPoint.z);
						}
					}

					// Finish the band
					p.endShape();
					bandStarted = false;
				}
			}
		}

		p.popStyle();
	}

	/**
	 * Draws the Kinect points as horizontal bands on the screen
	 * 
	 * @param p the Processing applet
	 * @param bandsColor the bands color
	 */
	public void drawAsBands(PApplet p, int bandsColor) {
		p.pushStyle();
		p.noStroke();
		p.fill(bandsColor);
		boolean bandStarted = false;

		for (int row = 0; row < height - 1; row++) {
			for (int col = 0; col < width; col++) {
				int index = col + row * width;

				// Check if the point is valid
				if (visibilityMask[index]) {
					PVector point = points[index];

					if (!bandStarted) {
						// Start a new band
						p.beginShape(PApplet.TRIANGLE_STRIP);
						p.vertex(point.x, point.y, point.z);
						bandStarted = true;
					} else if (KinectPoints.connected(point, points[index - 1])) {
						p.vertex(point.x, point.y, point.z);
					} else {
						p.endShape();
						bandStarted = false;

						// It's a good point, use it in the next loop as starting point for a new band
						col--;
						continue;
					}

					// Check if the lower point is valid
					int lowerIndex = index + width;

					if (visibilityMask[lowerIndex]) {
						PVector lowerPoint = points[lowerIndex];

						if (KinectPoints.connected(point, lowerPoint)) {
							p.vertex(lowerPoint.x, lowerPoint.y, lowerPoint.z);
						} else {
							p.vertex(point.x, point.y, point.z);
						}
					} else {
						p.vertex(point.x, point.y, point.z);
					}

					// Finish the band if it is the last point in the row
					if (col == width - 1) {
						p.endShape();
						bandStarted = false;
					}
				} else if (bandStarted) {
					// The point is not valid, let's see if we can use the lower point for the last point in the band
					int lowerIndex = index + width;

					if (visibilityMask[lowerIndex]) {
						PVector lowerPoint = points[lowerIndex];

						if (KinectPoints.connected(lowerPoint, points[index - 1])) {
							p.vertex(lowerPoint.x, lowerPoint.y, lowerPoint.z);
						}
					}

					// Finish the band
					p.endShape();
					bandStarted = false;
				}
			}
		}

		p.popStyle();
	}

	/**
	 * Draws a line between two Kinect points if they are connected
	 * 
	 * @param p the Processing applet
	 * @param point1 the first point
	 * @param point2 the second point
	 * @param lineColor the line color
	 */
	private void drawLine(PApplet p, PVector point1, PVector point2, int lineColor) {
		if (KinectPoints.connected(point1, point2)) {
			p.stroke(lineColor);
			p.line(point1.x, point1.y, point1.z, point2.x, point2.y, point2.z);
		}
	}

	/**
	 * Draws a triangle between three Kinect points if they are connected
	 * 
	 * @param p the Processing applet
	 * @param point1 the first point
	 * @param point2 the second point
	 * @param point3 the third point
	 */
	private void drawTriangle(PApplet p, PVector point1, PVector point2, PVector point3) {
		if (KinectPoints.connected(point1, point2) && KinectPoints.connected(point1, point3)
				&& KinectPoints.connected(point2, point3)) {
			p.beginShape(PApplet.TRIANGLES);
			p.vertex(point1.x, point1.y, point1.z);
			p.vertex(point2.x, point2.y, point2.z);
			p.vertex(point3.x, point3.y, point3.z);
			p.endShape();
		}
	}

	/**
	 * Draws a triangle between three Kinect points if they are connected
	 * 
	 * @param p the Processing applet
	 * @param point1 the first point
	 * @param point2 the second point
	 * @param point3 the third point
	 * @param color1 the first point color
	 * @param color2 the second point color
	 * @param color3 the third point color
	 */
	private void drawTriangle(PApplet p, PVector point1, PVector point2, PVector point3, int color1, int color2,
			int color3) {
		if (KinectPoints.connected(point1, point2) && KinectPoints.connected(point1, point3)
				&& KinectPoints.connected(point2, point3)) {
			p.beginShape(PApplet.TRIANGLES);
			p.fill(color1);
			p.vertex(point1.x, point1.y, point1.z);
			p.fill(color2);
			p.vertex(point2.x, point2.y, point2.z);
			p.fill(color3);
			p.vertex(point3.x, point3.y, point3.z);
			p.endShape();
		}
	}

	/**
	 * Draws the Kinect points as lines on the screen
	 * 
	 * @param p the Processing applet
	 */
	public void drawAsLines(PApplet p) {
		p.pushStyle();
		p.strokeWeight(1);

		for (int row = 0; row < height - 1; row++) {
			for (int col = 0; col < width - 1; col++) {
				int index = col + row * width;

				if (visibilityMask[index]) {
					PVector point = points[index];
					int lineColor = colors[index];

					if (visibilityMask[index + 1]) {
						drawLine(p, point, points[index + 1], lineColor);
					}

					if (visibilityMask[index + width]) {
						drawLine(p, point, points[index + width], lineColor);
					}

					if (visibilityMask[index + 1 + width]) {
						drawLine(p, point, points[index + 1 + width], lineColor);
					}
				}
			}
		}

		p.popStyle();
	}

	/**
	 * Draws the Kinect points as triangles on the screen
	 * 
	 * @param p the Processing applet
	 */
	public void drawAsTriangles(PApplet p) {
		p.pushStyle();
		p.noStroke();

		for (int row = 0; row < height - 1; row++) {
			for (int col = 0; col < width - 1; col++) {
				int index = col + row * width;

				// First triangle
				if (visibilityMask[index] && visibilityMask[index + width]) {
					if (visibilityMask[index + 1]) {
						drawTriangle(p, points[index], points[index + 1], points[index + width], colors[index],
								colors[index + 1], colors[index + width]);
					} else if (visibilityMask[index + 1 + width]) {
						drawTriangle(p, points[index], points[index + 1 + width], points[index + width], colors[index],
								colors[index + 1 + width], colors[index + width]);
					}
				}

				// Second triangle
				if (visibilityMask[index + 1] && visibilityMask[index + 1 + width]) {
					if (visibilityMask[index + width]) {
						drawTriangle(p, points[index + 1], points[index + 1 + width], points[index + width],
								colors[index + 1], colors[index + 1 + width], colors[index + width]);
					} else if (visibilityMask[index]) {
						drawTriangle(p, points[index], points[index + 1], points[index + 1 + width], colors[index],
								colors[index + 1], colors[index + 1 + width]);
					}
				}
			}
		}

		p.popStyle();
	}

	/**
	 * Draws the Kinect points as triangles on the screen
	 * 
	 * @param p the Processing applet
	 * @param trianglesColor the triangles color
	 */
	public void drawAsTriangles(PApplet p, int trianglesColor) {
		p.pushStyle();
		p.noStroke();
		p.fill(trianglesColor);

		for (int y = 0; y < height - 1; y++) {
			for (int x = 0; x < width - 1; x++) {
				int index = x + y * width;

				// First triangle
				if (visibilityMask[index] && visibilityMask[index + width]) {
					if (visibilityMask[index + 1]) {
						drawTriangle(p, points[index], points[index + 1], points[index + width]);
					} else if (visibilityMask[index + 1 + width]) {
						drawTriangle(p, points[index], points[index + 1 + width], points[index + width]);
					}
				}

				// Second triangle
				if (visibilityMask[index + 1] && visibilityMask[index + 1 + width]) {
					if (visibilityMask[index + width]) {
						drawTriangle(p, points[index + 1], points[index + 1 + width], points[index + width]);
					} else if (visibilityMask[index]) {
						drawTriangle(p, points[index], points[index + 1], points[index + 1 + width]);
					}
				}
			}
		}

		p.popStyle();
	}
}