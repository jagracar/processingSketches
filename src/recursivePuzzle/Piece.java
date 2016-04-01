package recursivePuzzle;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * The piece class
 * 
 * @author jagracar
 */
public class Piece {
	PVector pos;
	int size;
	PImage img;
	Puzzle puzzle;

	/**
	 * Constructor
	 * 
	 * @param pos the piece initial position
	 * @param size the piece size
	 * @param img the piece background image
	 */
	public Piece(PVector pos, int size, PImage img) {
		this.pos = pos.copy();
		this.size = size;
		this.img = img;
		this.puzzle = null;
	}

	/**
	 * Moves the piece position by a given amount
	 * 
	 * @param shift the piece shift
	 */
	public void shiftPos(PVector shift) {
		pos.add(shift);

		// Move the inner puzzle too if it exists
		if (puzzle != null) {
			puzzle.shiftPos(shift);
		}
	}

	/**
	 * Creates a new puzzle inside the piece if it's not too small
	 * 
	 * @param piecesPerSide the number of pieces per side
	 * @param minPieceSize the minimum puzzle piece size
	 */
	public void createPuzzle(int piecesPerSide, int minPieceSize) {
		// Limit the smaller piece size
		int newPieceSize = size / piecesPerSide;

		if (newPieceSize >= minPieceSize) {
			puzzle = new Puzzle(pos, newPieceSize, minPieceSize, img);
		}
	}

	/**
	 * Paint the piece or the inner puzzle on the screen
	 * 
	 * @param applet the Processing applet
	 */
	public void paint(PApplet applet) {
		if (puzzle != null) {
			puzzle.paint(applet);
		} else {
			applet.image(img, pos.x, pos.y);
		}
	}
}
