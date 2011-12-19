package fr.loria.parole.artimate.segmentation;

import java.util.ArrayList;

public class Segmentation {
	protected ArrayList<Segment> segments;

	public Segmentation() {
		segments = new ArrayList<Segment>();
	}

	public Segmentation(ArrayList<Segment> segments) {
		this.segments = segments;
	}

	public ArrayList<Segment> getSegments() {
		return segments;
	}

	public Segment getSegment(int i) {
		return segments.get(i);
	}
}
