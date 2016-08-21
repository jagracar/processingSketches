package kinectScanner;

import processing.core.PVector;

/*
 * Slit class
 * Subclass of the KinectPoints class
 *
 * Contains the data points from a vertical or horizontal slit.
 */

public class Slit extends KinectPoints {
	public PVector slitCenter;
	public boolean vertical; // false = horizontal

	//
	// Constructor
	//

	public Slit(KinectPoints kp, ScanBox sb, boolean tempVertical) {
		super(); // Necessary for the variable definitions
		slitCenter = sb.center.copy();
		vertical = tempVertical;

		// Define the dimensions of the arrays
		if (vertical) {
			width = 1;
			height = kp.height;
		} else {
			width = kp.width;
			height = 1;
		}
		nPoints = width * height;
		points = new PVector[nPoints];
		colors = new int[nPoints];
		visibilityMask = new boolean[nPoints];

		// Populate the arrays
		if (vertical) {
			// Find the x coordinate of the vertical slit
			int slitPos = -1;
			float minDistance = 1000;
			float delta = 0.5f * sb.size;
			for (int y = 0; y < kp.height; y++) {
				for (int x = 0; x < kp.width; x++) {
					int index = x + y * kp.width;
					boolean cond = kp.visibilityMask[index] && (kp.points[index].x > (slitCenter.x - delta))
							&& (kp.points[index].x < (slitCenter.x + delta))
							&& (kp.points[index].y > (slitCenter.y - delta))
							&& (kp.points[index].y < (slitCenter.y + delta))
							&& (kp.points[index].z > (slitCenter.z - delta))
							&& (kp.points[index].z < (slitCenter.z + delta));
					if (cond) {
						float distance = Math.abs(kp.points[index].x - slitCenter.x);
						if ((distance < minDistance) && (distance < 5)) {
							slitPos = x;
							minDistance = distance;
						}
					}
				}
			}
			// Populate the vertical slit
			for (int y = 0; y < height; y++) {
				if (slitPos >= 0) {
					int index = slitPos + y * kp.width;
					boolean cond = kp.visibilityMask[index] && (kp.points[index].x > (slitCenter.x - delta))
							&& (kp.points[index].x < (slitCenter.x + delta))
							&& (kp.points[index].y > (slitCenter.y - delta))
							&& (kp.points[index].y < (slitCenter.y + delta))
							&& (kp.points[index].z > (slitCenter.z - delta))
							&& (kp.points[index].z < (slitCenter.z + delta));
					if (cond) {
						points[y] = kp.points[index].copy();
						colors[y] = kp.colors[index];
						visibilityMask[y] = kp.visibilityMask[index];
					} else {
						points[y] = new PVector();
						colors[y] = 0;
						visibilityMask[y] = false;
					}
				} else {
					points[y] = new PVector();
					colors[y] = 0;
					visibilityMask[y] = false;
				}
			}
		} else {
			// Find the y coordinate of the horizontal slit
			int slitPos = -1;
			float minDistance = 1000;
			float delta = 0.5f * sb.size;
			for (int y = 0; y < kp.height; y++) {
				for (int x = 0; x < kp.width; x++) {
					int index = x + y * kp.width;
					boolean cond = kp.visibilityMask[index] && (kp.points[index].x > (slitCenter.x - delta))
							&& (kp.points[index].x < (slitCenter.x + delta))
							&& (kp.points[index].y > (slitCenter.y - delta))
							&& (kp.points[index].y < (slitCenter.y + delta))
							&& (kp.points[index].z > (slitCenter.z - delta))
							&& (kp.points[index].z < (slitCenter.z + delta));
					if (cond) {
						float distance = Math.abs(kp.points[index].y - slitCenter.y);
						if ((distance < minDistance) && (distance < 5)) {
							slitPos = y;
							minDistance = distance;
						}
					}
				}
			}
			// Populate the horizontal slit
			for (int x = 0; x < width; x++) {
				if (slitPos >= 0) {
					int index = x + slitPos * kp.width;
					boolean cond = kp.visibilityMask[index] && (kp.points[index].x > (slitCenter.x - delta))
							&& (kp.points[index].x < (slitCenter.x + delta))
							&& (kp.points[index].y > (slitCenter.y - delta))
							&& (kp.points[index].y < (slitCenter.y + delta))
							&& (kp.points[index].z > (slitCenter.z - delta))
							&& (kp.points[index].z < (slitCenter.z + delta));
					if (cond) {
						points[x] = kp.points[index].copy();
						colors[x] = kp.colors[index];
						visibilityMask[x] = kp.visibilityMask[index];
					} else {
						points[x] = new PVector();
						colors[x] = 0;
						visibilityMask[x] = false;
					}
				} else {
					points[x] = new PVector();
					colors[x] = 0;
					visibilityMask[x] = false;
				}
			}
		}
	}
}