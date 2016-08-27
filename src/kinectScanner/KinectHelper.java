package kinectScanner;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;

class KinectHelper {

	private KinectHelper() {

	}

	/*
	 * This function calculates the initial limits of the scene
	 */

	public static PVector[] calculateLimits(KinectPoints kp) {
		float xmin = Float.MAX_VALUE;
		float ymin = Float.MAX_VALUE;
		float zmin = Float.MAX_VALUE;
		float xmax = -Float.MAX_VALUE;
		float ymax = -Float.MAX_VALUE;
		float zmax = -Float.MAX_VALUE;

		for (int i = 0; i < kp.nPoints; i++) {
			if (kp.visibilityMask[i]) {
				PVector p = kp.points[i].copy();
				if (p.x < xmin)
					xmin = p.x;
				if (p.x > xmax)
					xmax = p.x;
				if (p.y < ymin)
					ymin = p.y;
				if (p.y > ymax)
					ymax = p.y;
				if (p.z < zmin)
					zmin = p.z;
				if (p.z > zmax)
					zmax = p.z;
			}
		}

		// Extend the range a bit
		float deltaX = 0.1f * (xmax - xmin);
		float deltaY = 0.1f * (ymax - ymin);
		float deltaZ = 0.1f * (zmax - zmin);
		xmin -= deltaX;
		xmax += deltaX;
		ymin -= deltaY;
		ymax += deltaY;
		zmin -= deltaZ;
		zmax += deltaZ;

		return new PVector[] { new PVector(xmin, ymin, zmin), new PVector(xmax, ymax, zmax) };
	}

	/*
	 * This function produces an average scan from a list of scans.
	 *
	 * Useful to reduce the noise in the data. It produces also some funny/blurry effects if you move while the scans
	 * are taken.
	 */

	public static Scan averageScans(PApplet p, ArrayList<Scan> scans) {
		// Get the first scan to calculate the dimensions of the arrays.
		// Assumes that all scans have the same dimensions.
		Scan s0 = (Scan) scans.get(0);
		int xSize = s0.width;
		int ySize = s0.height;
		int nPoints = s0.nPoints;

		PVector[] points = new PVector[nPoints];
		int[] colors = new int[nPoints];
		boolean[] constrains = new boolean[nPoints];
		PVector scanCenter = new PVector();
		float[] n = new float[nPoints]; // number of points per pixel to average
		float[] r = new float[nPoints]; // red color
		float[] g = new float[nPoints]; // green color
		float[] b = new float[nPoints]; // blue color

		// Initialize the arrays
		for (int i = 0; i < nPoints; i++) {
			points[i] = new PVector();
			constrains[i] = false;
			n[i] = r[i] = g[i] = b[i] = 0;
		}

		// Loop over the scans and add the points and the colors
		for (int i = 0; i < scans.size(); i++) {
			Scan s = (Scan) scans.get(i);
			scanCenter.add(s.scanCenter);

			for (int j = 0; j < nPoints; j++) {
				if (s.visibilityMask[j]) {
					points[j].add(s.points[j]);
					constrains[j] = true;
					n[j]++;
					r[j] += p.red(s.colors[j]);
					g[j] += p.green(s.colors[j]);
					b[j] += p.blue(s.colors[j]);
				}
			}
		}

		// Divide by the number of scans that contributed to the points
		scanCenter.div(scans.size());

		for (int i = 0; i < nPoints; i++) {
			if (constrains[i]) {
				points[i].div(n[i]);
				colors[i] = p.color(r[i] / n[i], g[i] / n[i], b[i] / n[i]);
			}
		}

		return new Scan(xSize, ySize, points, colors, constrains, scanCenter);
	}

	/*
	 * This function combines a list of slits to create a single scan.
	 */

	public static Scan combineSlits(ArrayList<Slit> slitList, boolean circular, boolean commonCenter) {
		// Get the last slit to calculate the dimensions of the arrays.
		// Assumes that all slits have the same dimensions.
		Slit sLast = (Slit) slitList.get(slitList.size() - 1);
		boolean vertical = sLast.vertical;
		PVector scanCenter = sLast.slitCenter.copy();

		int width, height;
		if (vertical) {
			width = slitList.size();
			height = sLast.height;
		} else {
			width = sLast.width;
			height = slitList.size();
		}
		int nPoints = width * height;
		PVector[] points = new PVector[nPoints];
		int[] colors = new int[nPoints];
		boolean[] visibilityMask = new boolean[nPoints];

		// Populate the arrays
		if (vertical) {
			for (int x = 0; x < width; x++) {
				Slit s = (Slit) slitList.get(x);

				for (int y = 0; y < height; y++) {
					int index = x + y * width;
					points[index] = s.points[y].copy();
					colors[index] = s.colors[y];
					visibilityMask[index] = s.visibilityMask[y];
					if (visibilityMask[index]) {
						// Rotate along the y axis in case is requested
						if (circular) {
							float rot = -4 * (width - x) * PApplet.TWO_PI / 360;
							points[index].sub(s.slitCenter);
							points[index].set(PApplet.cos(rot) * points[index].x + PApplet.sin(rot) * points[index].z,
									points[index].y,
									-PApplet.sin(rot) * points[index].x + PApplet.cos(rot) * points[index].z);
							points[index].add(s.slitCenter);
						}
						// Or just shift it in x direction
						else {
							points[index].add(new PVector((width - x) * 5, 0, 0));
						}
						// Reffer all slits to the same center
						if (commonCenter) {
							points[index].sub(s.slitCenter);
							points[index].add(scanCenter);
						}
					}
				}
			}
		} else {
			for (int y = 0; y < height; y++) {
				Slit s = (Slit) slitList.get(y);

				for (int x = 0; x < width; x++) {
					int index = x + y * width;
					points[index] = s.points[x].copy();
					colors[index] = s.colors[x];
					visibilityMask[index] = s.visibilityMask[x];
					if (visibilityMask[index]) {
						// Rotate along the x axis in case is requested
						if (circular) {
							float rot = 4 * (height - y) * PApplet.TWO_PI / 360;
							points[index].sub(s.slitCenter);
							points[index].set(points[index].x,
									PApplet.cos(rot) * points[index].y - PApplet.sin(rot) * points[index].z,
									PApplet.sin(rot) * points[index].y + PApplet.cos(rot) * points[index].z);
							points[index].add(s.slitCenter);
						}
						// Or just shift it in y direction
						else {
							points[index].add(new PVector(0, (height - y) * 5, 0));
						}
						// Reffer all slits to the same center
						if (commonCenter) {
							points[index].sub(s.slitCenter);
							points[index].add(scanCenter);
						}
					}
				}
			}
		}

		return new Scan(width, height, points, colors, visibilityMask, scanCenter);
	}
}