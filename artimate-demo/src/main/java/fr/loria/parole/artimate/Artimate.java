package fr.loria.parole.artimate;

import java.util.ListIterator;
import java.util.logging.Logger;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import fr.loria.parole.artimate.data.Unit;
import fr.loria.parole.artimate.data.UnitDB;
import fr.loria.parole.artimate.data.UnitSequence;
import fr.loria.parole.artimate.engine.Ardor3DWrapper;
import fr.loria.parole.artimate.synthesis.Synthesizer;

public class Artimate extends Synthesizer {

	private static final Logger logger = Logger.getLogger(Artimate.class.getName());

	private Ardor3DWrapper engine;

	public Artimate(UnitDB unitDB, Ardor3DWrapper engine) {
		super(unitDB);
		this.engine = engine;
	}

	public void synthesizeAnimation(UnitSequence targets) {
		// get candidates from UnitDB
		UnitSequence candidates = synthesize(targets);

		// create iterators
		assert targets.size() == candidates.size();
		PeekingIterator<Unit> targetIterator = Iterators.peekingIterator(targets.iterator());
		ListIterator<Unit> candidateIterator = candidates.iterator();

		// interleaved iteration over targets, candidates
		while (targetIterator.hasNext() && candidateIterator.hasNext()) {
			Unit target = targetIterator.next();
			Unit candidate = candidateIterator.next();

			// deep copy animation from candidate to target
			engine.copyAnimation(candidate, target);
		}

		engine.playAnimation(targets);
	}

}
