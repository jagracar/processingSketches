package recursivePuzzle;

import processing.core.PVector;

/**
 * The hole class
 * 
 * @author jagracar
 */
public class Hole {
	PVector pos;
	int size;
	PVector movementDir;

	/**
	 * Constructor
	 * 
	 * @param pos the hole initial position
	 * @param size the hole size
	 */
	public Hole(PVector pos, int size) {
		this.pos = pos.copy();
		this.size = size;
		this.movementDir = new PVector();
	}

	/**
	 * Calculates the next hole position
	 * 
	 * @param minPos the minimum allowed hole position
	 * @param maxPos the maximum allowed hole position
	 */
	public void nextMove(PVector minPos, PVector maxPos) {
		switch ((int) Math.floor(4 * Math.random())) {
		case 0:
			// Check that we don't exceed the limits and we don't undo the previous movement
			if (pos.x + size >= maxPos.x || movementDir.x == -1) {
				nextMove(minPos, maxPos);
			} else {
				movementDir.set(1, 0);
			}

			break;
		case 1:
			// Check that we don't exceed the limits and we don't undo the previous movement
			if (pos.y + size >= maxPos.y || movementDir.y == -1) {
				nextMove(minPos, maxPos);
			} else {
				movementDir.set(0, 1);
			}

			break;
		case 2:
			// Check that we don't exceed the limits and we don't undo the previous movement
			if (pos.x - size < minPos.x || movementDir.x == 1) {
				nextMove(minPos, maxPos);
			} else {
				movementDir.set(-1, 0);
			}

			break;
		case 3:
			// Check that we don't exceed the limits and we don't undo the previous movement
			if (pos.y - size < minPos.y || movementDir.y == 1) {
				nextMove(minPos, maxPos);
			} else {
				movementDir.set(0, -1);
			}

			break;
		}
	}

	/**
	 * Moves the hole to the next position
	 */
	public void move() {
		pos.add(movementDir.x * size, movementDir.y * size);

	}

	/**
	 * Moves the hole position by a given amount
	 * 
	 * @param shift the hole shift
	 */
	public void shiftPos(PVector shift) {
		pos.add(shift);
	}
}
