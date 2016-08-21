package kinectScanner;

import processing.core.PApplet;
import processing.core.PVector;

/**
 * Subclass of the KinectPoints class
 * 
 * Implements some additional functions to manipulate and save Kinect output data
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class Scan extends KinectPoints {

	/**
	 * The scan central coordinates
	 */
	public PVector scanCenter;

	/**
	 * Constructor
	 * 
	 * @param width arrays horizontal dimension
	 * @param height arrays vertical dimension
	 * @param points Kinect 3D points
	 * @param colors Kinect point colors
	 * @param visibilityMask Kinect points visibility mask
	 * @param scanCenter the scan central coordinates
	 */
	public Scan(int width, int height, PVector[] points, int[] colors, boolean[] visibilityMask, PVector scanCenter) {
		super(width, height, points, colors, visibilityMask);
		this.scanCenter = scanCenter.copy();
	}

	/**
	 * Constructor
	 * 
	 * @param kp the KinectPoints object
	 * @param box the scan box from which the scan will be selected
	 */
	public Scan(KinectPoints kp, ScanBox box) {
		this(kp.width, kp.height, kp.points, kp.colors, kp.visibilityMask, box.center);

		// Update the visibility mask
		float xMin = this.scanCenter.x - 0.5f * box.size;
		float xMax = xMin + box.size;
		float yMin = this.scanCenter.y - 0.5f * box.size;
		float yMax = yMin + box.size;
		float zMin = this.scanCenter.z - 0.5f * box.size;
		float zMax = zMin + box.size;

		for (int i = 0; i < this.nPoints; i++) {
			PVector point = this.points[i];
			this.visibilityMask[i] = this.visibilityMask[i] && (point.x > xMin) && (point.x < xMax) && (point.y > yMin)
					&& (point.y < yMax) && (point.z > zMin) && (point.z < zMax);
		}
	}

	/**
	 * Rotates the scan around the vertical axis
	 * 
	 * @param rotationAngle the scan rotation angle in radians
	 */
	public void rotate(float rotationAngle) {
		float cos = (float) Math.cos(rotationAngle);
		float sin = (float) Math.sin(rotationAngle);

		for (int i = 0; i < nPoints; i++) {
			PVector point = points[i];
			point.sub(scanCenter);
			point.set(cos * point.x + sin * point.z, point.y, -sin * point.x + cos * point.z);
			point.add(scanCenter);
		}
	}

	/**
	 * Crops the scan to the region with visible points
	 */
	public void crop() {
		// Calculate the limits of the scan region with visible data
		int colIni = Integer.MAX_VALUE;
		int colEnd = Integer.MIN_VALUE;
		int rowIni = Integer.MAX_VALUE;
		int rowEnd = Integer.MIN_VALUE;

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (visibilityMask[col + row * width]) {
					if (col < colIni) {
						colIni = col;
					}

					if (col > colEnd) {
						colEnd = col;
					}

					if (row < rowIni) {
						rowIni = row;
					}

					if (row > rowEnd) {
						rowEnd = row;
					}
				}
			}
		}

		// Check that there was at least one visible data point
		if (colIni <= colEnd && rowIni <= rowEnd) {
			// Obtain the dimensions of the cropped arrays
			int widthCropped = colEnd - colIni + 1;
			int heightCropped = rowEnd - rowIni + 1;
			int nPointsCropped = widthCropped * heightCropped;
			PVector[] pointsCropped = new PVector[nPointsCropped];
			int[] colorsCropped = new int[nPointsCropped];
			boolean[] visibilityMaskCropped = new boolean[nPointsCropped];

			// Populate the cropped arrays
			for (int row = 0; row < heightCropped; row++) {
				for (int col = 0; col < widthCropped; col++) {
					int indexCropped = col + row * heightCropped;
					int index = (colIni + col) + (rowIni + row) * width;
					pointsCropped[indexCropped] = points[index];
					colorsCropped[indexCropped] = colors[index];
					visibilityMaskCropped[indexCropped] = visibilityMask[index];
				}
			}

			// Update the arrays to the new dimensions
			width = widthCropped;
			height = heightCropped;
			nPoints = nPointsCropped;
			points = pointsCropped;
			colors = colorsCropped;
			visibilityMask = visibilityMaskCropped;
		}
	}

	/**
	 * Save the Kinect scan points and colors on a file
	 * 
	 * @param p the Processing applet
	 * @param fileName the file name
	 */
	public void savePoints(PApplet p, String fileName) {
		// Crop the scan to avoid writing unnecessary empty data points
		crop();

		// Create the array that will contain the file lines
		String[] lines = new String[nPoints + 1];

		// The first line describes the scan dimensions
		lines[0] = width + " " + height;

		// Write each point coordinates and color on a separate line
		for (int i = 0; i < nPoints; i++) {
			if (visibilityMask[i]) {
				// Center the points coordinates
				PVector point = PVector.sub(points[i], scanCenter);
				int col = colors[i];
				lines[i + 1] = point.x + " " + point.y + " " + point.z + " " + p.red(col) + " " + p.green(col) + " "
						+ p.blue(col);
			} else {
				// Use a dummy line if the point should be masked
				lines[i + 1] = "-99" + " " + "-99" + " " + "-99" + " " + "-99" + " " + "-99" + " " + "-99";
			}
		}

		// Save the data on the file
		p.saveStrings(fileName, lines);
		System.out.println("3D points saved in " + fileName);
	}
}