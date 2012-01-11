package fr.loria.parole.artimate.data;

import java.util.ArrayList;
import java.util.ListIterator;

public class UnitTrellis implements Iterable<UnitSequence> {

	private ArrayList<UnitSequence> sequences;

	public UnitTrellis() {
		sequences = new ArrayList<UnitSequence>();
	}

	public void addSequence(UnitSequence sequence) {
		sequences.add(sequence);
	}

	public UnitSequence getSequence(int index) {
		UnitSequence sequence = sequences.get(index);
		return sequence;
	}

	@Override
	public ListIterator<UnitSequence> iterator() {
		ListIterator<UnitSequence> iterator = sequences.listIterator();
		return iterator;
	}

}
