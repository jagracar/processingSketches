package recursivePuzzle;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * The puzzle class
 * 
 * @author jagracar
 */
public class Puzzle {
	PVector pos;
	int pieceSize;
	int minPieceSize;
	int width;
	int height;
	ArrayList<Piece> pieces;
	Piece movingPiece;
	Hole hole;
	int nSteps;
	int step;

	/**
	 * Constructor
	 * 
	 * @param pos the puzzle top left corner initial position
	 * @param pieceSize the piece size
	 * @param minPieceSize the minimum piece size
	 * @param img the background image
	 */
	Puzzle(PVector pos, int pieceSize, int minPieceSize, PImage img) {
		this.pos = pos.copy();
		this.pieceSize = pieceSize;
		this.minPieceSize = minPieceSize;
		this.width = img.width;
		this.height = img.height;
		this.pieces = new ArrayList<Piece>();
		this.movingPiece = null;
		this.hole = null;
		this.nSteps = 16;
		this.step = 0;

		// Create the hole at a random position and create the other pieces
		int xHole = (int) Math.floor(Math.random() * this.width / this.pieceSize) * this.pieceSize;
		int yHole = (int) Math.floor(Math.random() * this.height / this.pieceSize) * this.pieceSize;

		for (int x = 0; x < this.width; x += this.pieceSize) {
			for (int y = 0; y < this.height; y += this.pieceSize) {
				PVector elementPos = new PVector(this.pos.x + x, this.pos.y + y);

				if (x == xHole && y == yHole) {
					this.hole = new Hole(elementPos, this.pieceSize);
				} else {
					PImage pieceImg = img.get(x, y, this.pieceSize, this.pieceSize);
					this.pieces.add(new Piece(elementPos, this.pieceSize, pieceImg));
				}
			}
		}

		// Calculate the puzzle next move
		nextMove();
	}

	/**
	 * Calculates the puzzle next move
	 */
	public void nextMove() {
		// Move the hole
		hole.nextMove(pos, new PVector(pos.x + width, pos.y + height));
		hole.move();

		// Get the piece that is over the hole
		movingPiece = null;

		for (Piece p : pieces) {
			if (p.pos.dist(hole.pos) == 0) {
				movingPiece = p;
				break;
			}
		}

		// Create a puzzle in that piece if it doesn't exist already
		if (movingPiece.puzzle == null) {
			movingPiece.createPuzzle(4, minPieceSize);
		}
	}

	/**
	 * Moves the puzzle position by a given amount
	 * 
	 * @param shift the hole shift
	 */
	public void shiftPos(PVector shift) {
		// Shift the puzzle global position
		pos.add(shift);

		// Shift the hole and pieces positions
		hole.shiftPos(shift);

		for (Piece p : pieces) {
			p.shiftPos(shift);
		}
	}

	/**
	 * Moves the puzzle piece
	 */
	public void movePiece() {
		// Shift the position of the moving piece
		movingPiece.shiftPos(PVector.mult(hole.movementDir, -1.0f * pieceSize / nSteps));
		step++;

		// Move the other moving pieces in those pieces containing a puzzle
		for (Piece p : pieces) {
			if (p.puzzle != null) {
				p.puzzle.movePiece();
			}
		}

		// Start a new puzzle move every nSteps
		if (step % nSteps == 0) {
			nextMove();
		}
	}

	/**
	 * Paints the puzzle on the screen
	 * 
	 * @param applet the Processing applet
	 */
	public void paint(PApplet applet) {
		for (Piece p : pieces) {
			p.paint(applet);
		}
	}
}
