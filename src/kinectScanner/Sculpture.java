package kinectScanner;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;
import toxi.geom.Spline3D;
import toxi.geom.Vec3D;

/**
 * Class used to store and paint sculptures created with the Kinect
 * 
 * @author Javier Graciá Carpio (jagracar)
 */
class Sculpture {

	/**
	 * The minimum distance allowed between two consecutive spline control points
	 */
	private static final float MINIMUM_DISTANCE = 50;

	/**
	 * The sculpture section radius
	 */
	private float sectionRadius;

	/**
	 * The number of sides in each section
	 */
	private int sectionSides;

	/**
	 * The number of vertices between two spline control points
	 */
	private int nSubdivisions;

	/**
	 * The 3D spline curve
	 */
	private Spline3D spline;

	/**
	 * The last control point added to the spline
	 */
	private PVector previousPoint;

	/**
	 * The sections array list
	 */
	private ArrayList<SculptureSection> sections;

	/**
	 * Constructor
	 * 
	 * @param sectionRadius the sculpture section radius
	 * @param sectionSides the number of sculpture section sides
	 * @param nSubdivisions the number of spline vertices between to control points
	 */
	public Sculpture(float sectionRadius, int sectionSides, int nSubdivisions) {
		this.sectionRadius = sectionRadius;
		this.sectionSides = sectionSides;
		this.nSubdivisions = nSubdivisions;
		this.spline = new Spline3D();
		this.previousPoint = new PVector();
		this.sections = new ArrayList<SculptureSection>();
	}

	/**
	 * Adds a new control point to the sculpture
	 * 
	 * @param point the new control point
	 */
	public void addControlPoint(PVector point) {
		if (getNumControlPoints() == 0 || point.dist(previousPoint) > MINIMUM_DISTANCE) {
			spline.add(point.x, point.y, point.z);
			previousPoint.set(point);

			// Calculate the sculpture sections
			calculateSections();
		}
	}

	/**
	 * Returns the sculpture spline control points
	 * 
	 * @return the spline control points
	 */
	private int getNumControlPoints() {
		return spline.getPointList().size();
	}

	/**
	 * Sets the number of sides in each sculpture section
	 * 
	 * @param newSectionSides the new number of sculpture section sides
	 */
	public void setSectionSides(int newSectionSides) {
		if (sectionSides != newSectionSides && newSectionSides > 1) {
			sectionSides = newSectionSides;

			// Calculate the sculpture sections
			calculateSections();
		}
	}

	/**
	 * Calculates the sculpture sections between consecutive spline vertices
	 */
	private void calculateSections() {
		if (getNumControlPoints() > 1) {
			// Clear the sections array
			sections.clear();

			// Obtain the new sections
			ArrayList<Vec3D> vertices = (ArrayList<Vec3D>) spline.computeVertices(nSubdivisions);
			Vec3D refPoint = new Vec3D();
			Vec3D refNormal = vertices.get(1).sub(vertices.get(0)).normalize();

			for (int i = 0; i < vertices.size() - 1; i++) {
				SculptureSection section = new SculptureSection(vertices.get(i), vertices.get(i + 1), refPoint,
						refNormal, sectionRadius, sectionSides);
				refPoint = section.points[0];
				refNormal = section.normal;
				sections.add(section);
			}
		}
	}

	/**
	 * Cleans the sculpture, removing the control points and the sculpture sections
	 */
	public void clear() {
		spline = new Spline3D();
		previousPoint.set(0, 0, 0);
		sections.clear();
	}

	/**
	 * Centers the sculpture
	 */
	public void center() {
		// Find the center of the sculpture
		Vec3D sculptureCenter = new Vec3D();
		ArrayList<Vec3D> controlPoints = (ArrayList<Vec3D>) spline.getPointList();

		for (Vec3D controlPoint : controlPoints) {
			sculptureCenter.addSelf(controlPoint);
		}

		sculptureCenter.scaleSelf(1f / controlPoints.size());

		// Update the spline control points
		for (Vec3D controlPoint : controlPoints) {
			controlPoint.subSelf(sculptureCenter);
		}

		previousPoint.sub(sculptureCenter.x, sculptureCenter.y, sculptureCenter.z);

		// Calculate the sculpture sections
		calculateSections();
	}

	/**
	 * Paints the sculpture on the screen
	 * 
	 * @param p the parent Processing applet
	 * @param color the sculpture color
	 */
	public void paint(PApplet p, int color) {
		if (sections.size() > 1) {
			// Paint the front side
			sections.get(0).paint(p, color);

			// Paint the sculpture surface
			p.pushStyle();
			p.noStroke();
			p.fill(color);

			for (int i = 0; i < sections.size() - 1; i++) {
				SculptureSection section1 = sections.get(i);
				SculptureSection section2 = sections.get(i + 1);

				p.beginShape(PApplet.TRIANGLE_STRIP);

				for (int j = 0; j < section1.points.length; j++) {
					Vec3D point1 = section1.points[j];
					Vec3D point2 = section2.points[j];
					p.vertex(point1.x, point1.y, point1.z);
					p.vertex(point2.x, point2.y, point2.z);
				}

				Vec3D closePoint1 = section1.points[0];
				Vec3D closePoint2 = section2.points[0];
				p.vertex(closePoint1.x, closePoint1.y, closePoint1.z);
				p.vertex(closePoint2.x, closePoint2.y, closePoint2.z);
				p.endShape();
			}

			p.popStyle();

			// Paint the back side
			sections.get(sections.size() - 1).paint(p, color);
		}
	}

	/**
	 * Initializes the spline from a file
	 * 
	 * @param p the parent Processing applet
	 * @param fileName the name of the file containing the spline control points
	 */
	public void initFromFile(PApplet p, String fileName) {
		// Load the file containing the control points
		String[] fileLines = p.loadStrings(fileName);

		// Add the control points to a new spline object
		spline = new Spline3D();

		for (String line : fileLines) {
			String[] coordinates = line.split(" ");
			spline.add(Float.valueOf(coordinates[0]), Float.valueOf(coordinates[1]), Float.valueOf(coordinates[2]));
		}

		// Get the last added point
		Vec3D point = spline.getPointList().get(getNumControlPoints() - 1);
		previousPoint.set(point.x, point.y, point.z);

		// Calculate the sculpture sections
		calculateSections();
	}

	/**
	 * Saves the sculpture control points
	 * 
	 * @param p the parent Processing applet
	 * @param fileName the fine name
	 */
	public void savePoints(PApplet p, String fileName) {
		// Save sculpture control points in the file
		ArrayList<Vec3D> controlPoints = (ArrayList<Vec3D>) spline.getPointList();
		String[] pointsCoordinates = new String[getNumControlPoints()];

		for (int i = 0; i < pointsCoordinates.length; i++) {
			Vec3D point = (Vec3D) controlPoints.get(i);
			pointsCoordinates[i] = point.x + " " + point.y + " " + point.z;
		}

		p.saveStrings(fileName + ".sculpt", pointsCoordinates);
		System.out.println("Sculpture saved in " + fileName + ".sculpt");
	}
}
