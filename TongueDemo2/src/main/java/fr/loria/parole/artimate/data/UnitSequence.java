package fr.loria.parole.artimate.data;

import java.util.ArrayList;
import java.util.ListIterator;

public class UnitSequence implements Iterable<Unit> {
	protected ArrayList<Unit> units;

	public UnitSequence() {
		units = new ArrayList<Unit>();
	}

	public UnitSequence(ArrayList<Unit> units) {
		this.units = units;
	}

	public Unit get(int i) {
		return units.get(i);
	}

	public int size() {
		return units.size();
	}

	@Override
	public ListIterator<Unit> iterator() {
		ListIterator<Unit> iterator = units.listIterator();
		return iterator;
	}
}
