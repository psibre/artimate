package fr.loria.parole.artimate.data;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * A sequence of Units ordered along a dimension. This dimension can be time, or something else, such as target cost.
 * 
 * @author steiner
 * 
 */
public class UnitSequence implements Iterable<Unit> {
	protected List<Unit> units;

	public UnitSequence() {
		units = new ArrayList<Unit>();
	}

	public UnitSequence(List<Unit> units) {
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
