package kinectScanner;

import processing.core.PApplet;
import processing.core.PVector;

class Ball {
	PVector initPos;
	PVector pos;
	float rad;

	Ball(PVector pos, float rad) {
		this.initPos = pos.copy();
		this.pos = pos.copy();
		this.rad = rad;
	}

	void paint(PApplet p, int col) {
		if (pos.dist(initPos) > 0.1) {
			p.noStroke();
			p.fill(col);
			p.pushMatrix();
			p.translate(pos.x, pos.y, pos.z);
			p.sphere(rad);
			p.popMatrix();
			p.noFill();
		}
	}

	void update(PVector newPos) {
		pos.set(newPos);
	}
}