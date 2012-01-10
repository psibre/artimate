package fr.loria.parole.artimate.data;

import java.util.EnumMap;

public class Unit {
	protected EnumMap<Features, Object> features = new EnumMap<Features, Object>(Features.class);

	public Unit() {
	}

	public Unit(double start, double end) {
		this(start, end, "");
	}

	public Unit(double start, double end, String label) {
		setStart(start);
		setEnd(end);
		setLabel(label);
	}

	public double getStart() {
		Double start = (Double) features.get(Features.START);
		return start;
	}

	public double getEnd() {
		Double end = (Double) features.get(Features.END);
		return end;
	}

	public double getDuration() {
		double duration;
		if (features.containsKey(Features.DURATION)) {
			duration = (Double) features.get(Features.DURATION);
		} else {
			double start = getStart();
			double end = getEnd();
			duration = end - start;
			setDuration(duration);
		}
		return duration;
	}

	public String getLabel() {
		String label = (String) features.get(Features.LABEL);
		return label;
	}

	protected void setStart(double start) {
		features.put(Features.START, start);
	}

	protected void setEnd(double end) {
		features.put(Features.END, end);
	}

	protected void setDuration(double duration) {
		features.put(Features.DURATION, duration);
	}

	protected void setLabel(String label) {
		features.put(Features.LABEL, label);
	}
}
