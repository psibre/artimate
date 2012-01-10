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
}
