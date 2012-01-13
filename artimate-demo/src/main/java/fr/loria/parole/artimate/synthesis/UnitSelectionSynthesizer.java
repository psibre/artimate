package fr.loria.parole.artimate.synthesis;

import fr.loria.parole.artimate.data.UnitDB;

public class UnitSelectionSynthesizer extends Synthesizer {

	public UnitSelectionSynthesizer(UnitDB db) {
		super(db, new UnitSelector());
	}

}
