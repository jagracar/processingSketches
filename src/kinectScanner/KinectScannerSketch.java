package kinectScanner;

import java.util.ArrayList;

import SimpleOpenNI.SimpleOpenNI;
import processing.core.PApplet;
import processing.core.PVector;
import sun.nio.cs.ext.SJIS;

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
	public float xmin = Float.MAX_VALUE;
	public float xmax = -Float.MAX_VALUE;
	public float ymin = Float.MAX_VALUE;
	public float ymax = -Float.MAX_VALUE;
	public float zmin = Float.MAX_VALUE;
	public float zmax = -Float.MAX_VALUE;
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

	private SimpleOpenNI context;
	public ScanBox sBox;
	public Ball ball;
	private TransparentFloor transparentFloor;
	public KinectPoints kPoints;
	private Scan scan;
	public Scan slitScan;
	private int scanCounter = 0;
	private int slitScanCounter = 0;
	private int sculptureCounter = 0;
	private ArrayList<Scan> scansToAverage = new ArrayList<Scan>();
	public ArrayList<Slit> slits = new ArrayList<Slit>();
	private int frameIterator = 0;
	private int lastGesture;
	private PVector handPos = new PVector();
	private PVector prevHandPos = new PVector();
	public boolean firstTimeControl = false;
	private ControlPanel controlPanel;

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
		context.enableHand();
		context.startGesture(SimpleOpenNI.GESTURE_HAND_RAISE);
		// context.enableUser();

		// Update the kinect to calculate the scene limits
		context.update();
		kPoints = new KinectPoints(context.depthMapRealWorld(), context.rgbImage(), context.depthMap(), 5);
		KinectHelper.calculateLimits(kPoints);
		xmin = -1100;
		xmax = 1100;
		ymin = -1500;
		ymax = 1000;
		zmin = 0;
		zmax = 3300;

		// Initialize the scan box
		sBox = new ScanBox(new PVector((xmax + xmin) / 2, (ymax + ymin) / 2, (zmax + zmin) / 2), 400);

		// Initialize the semitransparent floor
		transparentFloor = new TransparentFloor(this, 2 * xmin, 2 * xmax, ymin, ymax, zmin, zmax, color(0));

		// Initialize the sculpture
		// sculpt = new Sculpture();
		// sculpt.setSides(sculptureSides);

		// Initialize the ball for the hand position
		ball = new Ball(handPos,10);

		// Initialize the controlP5 window. This should come after all the other definitions
		controlPanel = new ControlPanel(this, 0, 0);

		// Start the controller sketch window
		PApplet.runSketch(new String[] { ControlPanel.class.getName() }, controlPanel);

		// imageMode(CENTER);
		// mass = loadImage("/home/jgracia/sketchbook/mass3.png");
		// beer = new Bier(mass, initHandPos);
		// bretz = loadImage("/home/jgracia/sketchbook/bretzel2.png");
		// bretzel1 = new Bretzel(bretz,new PVector(random(-500,500),random(700,1000),random(600,2000)));
		// bretzel2 = new Bretzel(bretz,new PVector(random(-500,500),random(700,1000),random(600,2000)));
		// bretzel3 = new Bretzel(bretz,new PVector(random(-500,500),random(700,1000),random(600,2000)));

		// backgroundFig = loadImage("UAMO2013.jpg");
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
		kPoints.constrainPoints(xmin, xmax, ymin, ymax, zmin, zmax);

		// Position the scene
		background(150);
		// background(backgroundFig);
		translate(width / 2, height / 2, 0);
		rotateX(rotX);
		rotateY(rotY);
		scale(zoom);
		translate(0, 0, -1500);

		// Draw the floor
		transparentFloor.paint(this);

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

		// Draw the sculpture
		if (drawSculpture && !oktoberfest) {
			if (!monochrome) {
				ambientLight(100, 100, 100);
				directionalLight(255 - 100, 255 - 100, 255 - 100, 0, 0, 1);
				lightFalloff(1, 0, 0);
				lightSpecular(0, 0, 0);
			}

			// sculpt.paint(color(255));
			ball.update(handPos);
			ball.paint(this, color(255, 20, 20));
		}

		// Save the last sculpture
		if (saveSculpture) {// && sculpt.nPoints > 2) {
			sculptureCounter++;
			String sculptureFileName = fileDir + fileName + "-" + sculptureCounter;
			// sculpt.savePoints(sculptureFileName);
			saveSculpture = false;
		}

		// Oktoberfest fun
		if (oktoberfest) {
			// bretzel1.update();
			// bretzel2.update();
			// bretzel3.update();
			//
			// float[] zpos = {bretzel1.pos.z,bretzel2.pos.z,bretzel3.pos.z,handPos.z};
			// zpos = sort(zpos);
			//
			// for(int i = 3; i >= 0 ; i--){
			// if(bretzel1.pos.z == zpos[i]){
			// bretzel1.paint();
			// }
			// if(bretzel2.pos.z == zpos[i]){
			// bretzel2.paint();
			// }
			// if(bretzel3.pos.z == zpos[i]){
			// bretzel3.paint();
			// }
			// if(handPos.z == zpos[i]){
			// beer.paint(handPos);
			// }
			// }
			//
			// bretzel1.checkOver(handPos);
			// bretzel2.checkOver(handPos);
			// bretzel3.checkOver(handPos);
		}

		// Control the scene with the body
		if (handControl && !(handPos.x == 0 && handPos.y == 0 && handPos.z == 0)) {
			if (firstTimeControl) {
				firstTimeControl = false;
			} else {
				rotY += map(handPos.x - prevHandPos.x, -2000, 2000, -PI, PI);
				rotX -= map(handPos.y - prevHandPos.y, -2000, 2000, -PI, PI);

				if (handPos.z - prevHandPos.z > 0) {
					zoom /= map(handPos.z - prevHandPos.z, 0, 2000, 1, 2);
				} else {
					zoom *= map(-handPos.z + prevHandPos.z, 0, 2000, 1, 2);
				}
			}

			prevHandPos.set(handPos);
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
		if (pos.x > -700 && pos.x < 700 && pos.y > -500 && pos.y < ymax && pos.z > 500 && pos.z < 2500) {
			context.endGesture(gestureType);
			context.startTrackingHand(pos);
			lastGesture = gestureType;
		}
	}

	public void onNewHand(SimpleOpenNI curContext, int handId, PVector pos) {
		println("SimpleOpenNI hand information: Hand created (id: " + handId + ")");
	}

	public void onTrackedHand(SimpleOpenNI curContext, int handId, PVector pos) {
		handPos.set(pos);

		if (takeSculpture) {
			// sculpt.addPoint(pos);
		}
	}

	public void onLostHand(SimpleOpenNI curContext, int handId) {
		println("SimpleOpenNI hand information: Hand destroyed (id: " + handId + ")");
		context.startGesture(lastGesture);
		handPos.set(0, 0, 0);

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
