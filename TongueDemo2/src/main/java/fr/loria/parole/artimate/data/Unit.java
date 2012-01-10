package fr.loria.parole.artimate.data;

import java.util.EnumMap;

public class Unit {
	protected EnumMap<Features, Object> features = new EnumMap<Features, Object>(Features.class);

	public Unit() {
	}

	public Unit(double duration) {
		this(duration, "");
	}

	public Unit(double duration, String label) {
		setDuration(duration);
		setLabel(label);
	}

	public double getDuration() {
		double duration = (Double) features.get(Features.DURATION);
		return duration;
	}

	public String getLabel() {
		String label = (String) features.get(Features.LABEL);
		return label;
	}

	protected void setDuration(double duration) {
		features.put(Features.DURATION, duration);
	}

	protected void setLabel(String label) {
		features.put(Features.LABEL, label);
	}
}
