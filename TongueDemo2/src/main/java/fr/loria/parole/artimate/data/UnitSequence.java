package fr.loria.parole.artimate.data;

import java.util.ArrayList;

public class UnitSequence {
	protected ArrayList<Unit> units;

	public UnitSequence() {
		units = new ArrayList<Unit>();
	}

	public UnitSequence(ArrayList<Unit> units) {
		this.units = units;
	}

	public ArrayList<Unit> getUnits() {
		return units;
	}

	public Unit get(int i) {
		return units.get(i);
	}

	public int size() {
		return units.size();
	}

	public double getStart(int s) {
		double start = 0;
		for (int i = 0; i < s; i++) {
			start += units.get(i).getDuration();
		}
		return start;
	}

	public double getEnd(int s) {
		double end = getStart(s) + units.get(s).getDuration();
		return end;
	}

	public int indexOf(String label) {
		for (int i = 0; i < units.size(); i++) {
			if (units.get(i).getLabel().equals(label)) {
				return i;
			}
		}
		return -1;
	}
}
