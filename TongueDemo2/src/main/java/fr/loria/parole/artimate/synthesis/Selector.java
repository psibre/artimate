package fr.loria.parole.artimate.synthesis;

import java.util.ArrayList;

import fr.loria.parole.artimate.data.Unit;
import fr.loria.parole.artimate.data.UnitDB;
import fr.loria.parole.artimate.data.UnitSequence;
import fr.loria.parole.artimate.data.UnitTrellis;

public class Selector {

	public UnitSequence select(UnitDB db, UnitSequence targets) {
		UnitTrellis candidates = getCandidates(db, targets);
		UnitSequence path = findPath(candidates, targets);
		return path;
	}

	protected UnitTrellis getCandidates(UnitDB db, UnitSequence targets) {
		UnitTrellis trellis = new UnitTrellis();
		for (Unit target : targets) {
			String key = target.getLabel();
			UnitSequence sequence = new UnitSequence(db.getUnitList(key));
			trellis.addSequence(sequence);
		}
		return trellis;
	}

	protected UnitSequence findPath(UnitTrellis trellis, UnitSequence targets) {
		ArrayList<Unit> units = new ArrayList<Unit>(targets.size());
		for (UnitSequence candidates : trellis) {
			units.add(candidates.get(0));
		}
		return new UnitSequence(units);
	}

}
