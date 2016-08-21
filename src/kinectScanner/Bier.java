package kinectScanner;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

class Bier {
	PVector initPos;
	PImage img;

	Bier(PImage img, PVector initPos) {
		this.img = img;
		this.initPos = initPos.copy();
	}

	void paint(PApplet p, PVector pos) {
		if (pos.dist(initPos) > 0.1) {
			p.pushMatrix();
			p.translate(pos.x, pos.y + 20, pos.z - 50);
			p.rotateZ(PApplet.PI);
			p.image(img, 0, 0);
			p.popMatrix();
		}
	}
}