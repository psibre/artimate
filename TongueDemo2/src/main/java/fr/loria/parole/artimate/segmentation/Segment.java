package fr.loria.parole.artimate.segmentation;

public class Segment {
	protected float start;
	protected float end;
	protected String label;

	public Segment() {
	}

	public Segment(float start, float end) {
		this.start = start;
		this.end = end;
	}

	public Segment(float start, float end, String label) {
		this(start, end);
		this.label = label;
	}

	public float getStart() {
		return start;
	}

	public void setStart(float start) {
		this.start = start;
	}

	public float getEnd() {
		return end;
	}

	public void setEnd(float end) {
		this.end = end;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
