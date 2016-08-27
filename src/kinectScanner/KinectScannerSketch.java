package kinectScanner;

import java.util.ArrayList;

import SimpleOpenNI.SimpleOpenNI;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * Simple 3D scanner using the Kinect (JGC, version 6).
 *
 * Select the scan area with the controls (or use the "center in face" option) and press the "take scan" button to
 * capture the 3D points inside the box. Press "save scan" to save them in the sketch directory. Press "take scan" again
 * to take more scans.
 *
 * Do the same for the slit scans.
 *
 * Use http://www.openprocessing.org/sketch/62533 to read and represent the scans.
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
public class KinectScannerSketch extends PApplet {

	// Sketch variables
	public boolean drawBands = true;
	public boolean drawPixels = false;
	public boolean monochrome = false;
	public int monochromeCol = color(220, 50, 50);
	public int resolution = 2;
	public PVector[] limits;
	public String fileName = "test";
	public String fileDir = "out/";
	public boolean drawBox = false;
	public int framesPerScan = 10;
	public boolean takeScan = false;
	public boolean drawScan = false;
	public boolean saveScan = false;
	public boolean verticalSlitScan = true;
	public boolean rotateSlitScan = false;
	public boolean centerSlitScan = false;
	public boolean takeSlitScan = false;
	public boolean drawSlitScan = true;
	public boolean saveSlitScan = false;
	public int sculptureSides = 30;
	public boolean takeSculpture = false;
	public boolean drawSculpture = true;
	public boolean saveSculpture = false;
	public boolean oktoberfest = false;
	public boolean handControl = false;
	public float initZoom = 0.35f;
	public float initRotX = PI;
	public float initRotY = 0;
	public float zoom = initZoom;
	public float rotX = initRotX;
	public float rotY = initRotY;

	public SimpleOpenNI context;
	public ScanBox sBox;
	private Floor transparentFloor;
	public KinectPoints kPoints;
	private Scan scan;
	public Scan slitScan;
	private int scanCounter = 0;
	private int slitScanCounter = 0;
	private int sculptureCounter = 0;
	private ArrayList<Scan> scansToAverage = new ArrayList<Scan>();
	public ArrayList<Slit> slits = new ArrayList<Slit>();
	private int frameIterator = 0;
	private PVector handPos;
	private PVector prevHandPos;
	private KinectControlPanel controlPanel;
	private MovingImg bier;
	private MovingImg[] brezeln;
	private PImage backgroundFig;
	public Sculpture sculpt;
	private boolean handIsEnabled = false;
	private int handGesture = 0;

	/**
	 * Sets the default window size
	 */
	public void settings() {
		size(1024, 768, P3D);
	}

	/**
	 * Initial sketch setup
	 */
	public void setup() {
		perspective(radians(45), ((float) width) / ((float) height), 10.0f, 150000.0f);

		// Initialize the Kinect
		context = new SimpleOpenNI(this, SimpleOpenNI.RUN_MODE_MULTI_THREADED);
		context.setMirror(true);
		context.enableDepth();
		context.enableRGB();
		context.alternativeViewPointDepthToImage();
		handGesture = SimpleOpenNI.GESTURE_HAND_RAISE;

		// Update the kinect to calculate the scene limits
		context.update();
		kPoints = new KinectPoints(context.depthMapRealWorld(), context.rgbImage(), context.depthMap(), 5);
		limits = KinectHelper.calculateLimits(kPoints);
		//limits[0] = new PVector(-1100, -1500, 0);
		//limits[1] = new PVector(1100, 1000, 3300);

		// Initialize the scan box
		sBox = new ScanBox(PVector.add(limits[0], limits[1]).mult(0.5f), 400);

		// Initialize the semitransparent floor
		transparentFloor = new Floor(this, color(0));

		// Initialize the sculpture
		sculpt = new Sculpture(100f, sculptureSides, 2);

		// Initialize the controlP5 window. This should come after all the other definitions
		controlPanel = new KinectControlPanel(this, 0, 0);

		// Start the controller sketch window
		PApplet.runSketch(new String[] { KinectControlPanel.class.getName() }, controlPanel);

		PImage bierImg = loadImage("src/kinectScanner/mass.png");
		PImage bretzelImg = loadImage("src/kinectScanner/bretzel.png");

		bier = new MovingImg(bierImg);
		bier.angle = PI;

		brezeln = new MovingImg[30];

		for (int i = 0; i < brezeln.length; i++) {
			MovingImg brezel = new MovingImg(bretzelImg);
			brezel.position = getRandomPosition();
			brezel.velocity.set(0, -5, 0);
			brezeln[i] = brezel;
		}

		backgroundFig = loadImage("src/kinectScanner/UAMO2013.jpg");
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Write the frame rate in the screen title (thanks to amnon.owed for the tip!)
		surface.setTitle("UAMO 2013: interactive sculpture // " + (int) frameRate + " fps");

		// Update the kinect points
		context.update();
		kPoints = new KinectPoints(context.depthMapRealWorld(), context.rgbImage(), context.depthMap(), resolution);
		kPoints.constrainPoints(limits);

		// Position the scene
		// background(150);
		background(backgroundFig);
		translate(width / 2, height / 2, 0);
		rotateX(rotX);
		rotateY(rotY);
		scale(zoom);
		translate(0, 0, -1500);

		// Draw the floor
		transparentFloor.paint(this, limits);

		// Draw the scan box
		if (drawBox) {
			sBox.paint(this, 255);
		}

		// Lights with real colors don't look very nice
		if (monochrome) {
			ambientLight(100, 100, 100);
			directionalLight(255 - 100, 255 - 100, 255 - 100, 0, 0, 1);
			lightFalloff(1, 0, 0);
			lightSpecular(0, 0, 0);
		}

		// Draw the kinect points
		if (drawBands && !drawScan) {
			if (monochrome) {
				kPoints.drawAsTriangles(this, monochromeCol);
			} else {
				kPoints.drawAsTriangles(this);
			}
		}

		if (drawPixels && !drawScan) {
			if (monochrome) {
				kPoints.drawAsPixels(this, 2, monochromeCol);
			} else {
				kPoints.drawAsPixels(this, 2);
			}
		}

		// Take a scan
		if (takeScan && !takeSlitScan) {
			if (framesPerScan <= 1) {
				scan = new Scan(kPoints, sBox);
				scanCounter++;
				takeScan = false;
				println("Take scan: Done (scan " + scanCounter + ")");
			} else {
				if (frameIterator < framesPerScan) {
					frameIterator++;
					scansToAverage.add(new Scan(kPoints, sBox));
					println("Take scan: Running (frame " + frameIterator + ")");
				} else {
					scan = KinectHelper.averageScans(this, scansToAverage);
					scansToAverage.clear();
					frameIterator = 0;
					scanCounter++;
					takeScan = false;
					println("Take scan: Done (scan " + scanCounter + ")");
				}
			}
		}

		// Draw the last scan taken
		if (drawScan && !takeScan && !takeSlitScan && (scanCounter > 0)) {
			if (monochrome) {
				scan.drawAsTriangles(this, monochromeCol);
			} else {
				scan.drawAsTriangles(this);
			}
		}

		// Save the last scan taken
		if (saveScan && (scanCounter > 0)) {
			String scanFileName = fileDir + fileName + "-" + scanCounter + ".points";
			scan.savePoints(this, scanFileName);
			saveScan = false;
		}

		// Take a slit scan
		if (takeSlitScan && !takeScan) {
			slits.add(new Slit(kPoints, sBox, verticalSlitScan));
			slitScan = KinectHelper.combineSlits(slits, rotateSlitScan, centerSlitScan);
			println("Take slit scan: Running (" + slits.size() + " slits)");
		}

		// Draw the last slit scan taken
		if (drawSlitScan && (slits.size() > 0)) {
			if (monochrome) {
				slitScan.drawAsTriangles(this, monochromeCol);
			} else {
				slitScan.drawAsTriangles(this);
			}
		}

		// Save the last slit scan taken
		if (saveSlitScan && (slits.size() > 0)) {
			slitScanCounter++;
			String slitScanFileName = fileDir + fileName + "-slit" + slitScanCounter + ".points";
			slitScan.savePoints(this, slitScanFileName);
			saveSlitScan = false;
		}

		if (oktoberfest || takeSculpture || handControl) {
			if (!handIsEnabled) {
				context.enableHand();
				context.startGesture(handGesture);
				handIsEnabled = true;
			}
		} else if (handIsEnabled) {
			context.enableHand(false);
			context.endGesture(handGesture);
			handIsEnabled = false;
			handPos = null;
			prevHandPos = null;
		}

		// Draw the sculpture
		if (drawSculpture && !oktoberfest) {
			if (!monochrome) {
				ambientLight(100, 100, 100);
				directionalLight(255 - 100, 255 - 100, 255 - 100, 0, 0, 1);
				lightFalloff(1, 0, 0);
				lightSpecular(0, 0, 0);
			}

			sculpt.paint(this, color(255));

			if (handPos != null) {
				pushStyle();
				noStroke();
				fill(color(255, 20, 20));
				pushMatrix();
				translate(handPos.x, handPos.y, handPos.z);
				sphere(10);
				popMatrix();
				popStyle();
			}
		}

		// Save the last sculpture
		if (saveSculpture) {
			sculptureCounter++;
			String sculptureFileName = fileDir + fileName + "-" + sculptureCounter;
			sculpt.savePoints(this, sculptureFileName);
			saveSculpture = false;
		}

		// Oktoberfest fun
		if (oktoberfest) {
			if (handPos != null) {
				bier.position.set(handPos.x, handPos.y, handPos.z - 60);
				bier.visible = true;
			} else {
				bier.visible = false;
			}

			for (MovingImg brezel : brezeln) {
				if (handPos != null && brezel.closeToPosition(handPos)) {
					brezel.position = getRandomPosition();
				}

				brezel.update();

				if (brezel.position.y < 0) {
					brezel.position = getRandomPosition();
				}
			}

			sortImages(brezeln);

			boolean bierIsPainted = false;

			for (MovingImg brezel : brezeln) {
				if (bier.position.z > brezel.position.z) {
					bier.paint(this);
					bierIsPainted = true;
				}

				brezel.paint(this);
			}

			if (!bierIsPainted) {
				bier.paint(this);
			}
		}

		// Control the scene with the body
		if (handControl && handPos != null) {
			rotY += map(handPos.x - prevHandPos.x, -2000, 2000, -PI, PI);
			rotX -= map(handPos.y - prevHandPos.y, -2000, 2000, -PI, PI);

			if (handPos.z - prevHandPos.z > 0) {
				zoom /= map(handPos.z - prevHandPos.z, 0, 2000, 1, 2);
			} else {
				zoom *= map(-handPos.z + prevHandPos.z, 0, 2000, 1, 2);
			}
		}
	}

	public PVector getRandomPosition() {
		float x = -500f + 1000f * ((float) Math.random());
		float y = 700f + 300f * ((float) Math.random());
		float z = 600f + 1400f * ((float) Math.random());
		return new PVector(x, y, z);
	}

	/**
	 * Sorts the images depending of their z coordinate
	 * 
	 * @param images the array of images to sort
	 */
	public void sortImages(MovingImg[] images) {
		int n = images.length;

		for (int i = 0; i < n - 1; i++) {
			int maxPos = i;
			float maxValue = images[i].position.z;

			for (int j = i + 1; j < n; j++) {
				float value = images[j].position.z;

				if (value > maxValue) {
					maxPos = j;
					maxValue = value;
				}
			}

			if (maxPos != i) {
				MovingImg tmp = images[i];
				images[i] = images[maxPos];
				images[maxPos] = tmp;
			}
		}
	}

	public void mouseDragged() {
		noCursor();
		rotX -= map(mouseY - pmouseY, -height, height, -TWO_PI, TWO_PI);
		rotY -= map(mouseX - pmouseX, -width, width, -TWO_PI, TWO_PI);
	}

	public void mouseReleased() {
		cursor();
	}

	public void keyPressed() {
		switch (keyCode) {
		case UP:
			zoom *= 1.05;
			break;
		case DOWN:
			zoom /= 1.05;
			break;
		}
	}

	/*
	 * SimpleOpenNI hand controls
	 */

	public void onCompletedGesture(SimpleOpenNI curContext, int gestureType, PVector pos) {
		println("SimpleOpenNI hand information: Recognized gesture (" + gestureType + ")");
		if (pos.x > -700 && pos.x < 700 && pos.y > -500 && pos.y < limits[1].y && pos.z > 500 && pos.z < 2500) {
			context.endGesture(gestureType);
			context.startTrackingHand(pos);
			handPos = pos.copy();
			prevHandPos = pos.copy();
		}
	}

	public void onNewHand(SimpleOpenNI curContext, int handId, PVector pos) {
		println("SimpleOpenNI hand information: Hand created (id: " + handId + ")");
	}

	public void onTrackedHand(SimpleOpenNI curContext, int handId, PVector pos) {
		prevHandPos = handPos;
		handPos = pos.copy();

		if (takeSculpture) {
			sculpt.addControlPoint(pos);
		}
	}

	public void onLostHand(SimpleOpenNI curContext, int handId) {
		println("SimpleOpenNI hand information: Hand destroyed (id: " + handId + ")");
		context.startGesture(handGesture);
		handPos = null;
		prevHandPos = null;

		if (handControl) {
			zoom = initZoom;
			rotX = initRotX;
			rotY = initRotY;
		}
	}

	/**
	 * Executes the Processing sketch
	 * 
	 * @param args arguments to be passed to the sketch
	 */
	static public void main(String[] args) {
		String[] sketchName = new String[] { KinectScannerSketch.class.getName() };

		if (args != null) {
			PApplet.main(concat(sketchName, args));
		} else {
			PApplet.main(sketchName);
		}
	}

}
