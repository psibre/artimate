package fr.loria.parole.artimate.data;

public class Unit {
	protected double duration;
	protected String label;

	public Unit(double duration) {
		this(duration, "");
	}

	public Unit(double duration, String label) {
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