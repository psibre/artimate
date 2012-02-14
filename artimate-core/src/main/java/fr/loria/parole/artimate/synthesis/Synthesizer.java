package fr.loria.parole.artimate.synthesis;

import fr.loria.parole.artimate.data.UnitDB;
import fr.loria.parole.artimate.data.UnitSequence;

public class Synthesizer {

	protected UnitDB db;
	protected Selector selector;

	public Synthesizer(UnitDB db) {
		this(db, new Selector());
	}

	public Synthesizer(UnitDB db, Selector selector) {
		this.db = db;
		this.selector = selector;
	}

	public UnitSequence synthesize(UnitSequence targets) {
		return selector.select(db, targets);
	}

}
