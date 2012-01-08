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

	public Segment get(int i) {
		return segments.get(i);
	}

	public int size() {
		return segments.size();
	}

	public double getStart(int s) {
		double start = 0;
		for (int i = 0; i < s; i++) {
			start += segments.get(i).getDuration();
		}
		return start;
	}

	public double getEnd(int s) {
		double end = getStart(s) + segments.get(s).getDuration();
		return end;
	}

	public int indexOf(String label) {
		for (int i = 0; i < segments.size(); i++) {
			if (segments.get(i).getLabel().equals(label)) {
				return i;
			}
		}
		return -1;
	}
}
