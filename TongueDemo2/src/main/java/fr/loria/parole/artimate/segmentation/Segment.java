package fr.loria.parole.artimate.segmentation;

public class Segment {
	protected double duration;
	protected String label;

	public Segment(double duration) {
		this(duration, "");
	}

	public Segment(double duration, String label) {
		this.duration = duration;
		this.label = label;
	}

	public double getDuration() {
		return duration;
	}

	public String getLabel() {
		return label;
	}
}