package kinectScanner;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

class Bretzel {

	PVector pos;
	PImage img;

	Bretzel(PImage img, PVector pos) {
		this.img = img;
		this.pos = pos.copy();
	}

	void update(PApplet p) {
		pos.add(new PVector(0, -5, 0));
		if (pos.y < -900) {
			pos.set(new PVector(p.random(-500, 500), p.random(700, 1000), p.random(600, 2000)));
		}
	}

	void checkOver(PApplet p, PVector hand) {
		if (pos.dist(hand) < 100) {
			pos.set(new PVector(p.random(-500, 500), p.random(700, 1000), p.random(600, 2000)));
		}
	}

	void paint(PApplet p) {
		p.pushMatrix();
		p.translate(pos.x, pos.y, pos.z);
		p.image(img, 0, 0);
		p.popMatrix();
	}

}