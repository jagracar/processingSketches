package recursivePuzzle;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * Recursive puzzle processing sketch
 * 
 * @author jagracar
 */
public class RecursivePuzzleSketch extends PApplet {
	int pieceSize;
	int minPieceSize;
	PImage img;
	Puzzle puzzle;

	/**
	 * Sets the default window size
	 */
	public void settings() {
		size(100, 100);
	}

	/**
	 * Initial sketch setup
	 */
	public void setup() {
		// Initialize the sketch global variables
		pieceSize = (int) pow(2, 6);
		minPieceSize = pieceSize;

		// Load the picture by Sukanto Debnath
		// https://www.flickr.com/photos/sukanto_debnath/2181170020/
		// img = loadImage("http://farm4.staticflickr.com/3137/3081836966_7945315150.jpg");
		img = loadImage("src/recursivePuzzle/picture.jpg");

		// Resize the picture to be a multiple of the puzzle piece size
		img.resize(pieceSize * floor(img.width / pieceSize), pieceSize * floor(img.height / pieceSize));

		// Resize the window to the new image size
		surface.setResizable(true);
		surface.setSize(img.width, img.height);

		// Create the puzzle
		puzzle = new Puzzle(new PVector(0, 0), pieceSize, minPieceSize, img);

		// Set the sketch frame rate
		frameRate(30);
	}

	/**
	 * Draw method
	 */
	public void draw() {
		// Clean the window
		background(255);

		// Paint the puzzle
		puzzle.paint(this);

		// Move the puzzle piece
		puzzle.movePiece();
	}

	/**
	 * Creates a new puzzle each time the mouse is pressed
	 */
	public void mousePressed() {
		// Decrease or increment the minimum piece size
		minPieceSize /= 4;

		if (minPieceSize < 4) {
			minPieceSize = pieceSize;
		}

		// Create a new puzzle
		puzzle = new Puzzle(new PVector(0, 0), pieceSize, minPieceSize, img);
	}

	/**
	 * Executes the Processing sketch
	 * 
	 * @param args arguments to be passed to the sketch
	 */
	static public void main(String[] args) {
		String[] sketchName = new String[] { RecursivePuzzleSketch.class.getName() };

		if (args != null) {
			PApplet.main(concat(sketchName, args));
		} else {
			PApplet.main(sketchName);
		}
	}
}
