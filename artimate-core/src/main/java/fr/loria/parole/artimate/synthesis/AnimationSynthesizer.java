package fr.loria.parole.artimate.synthesis;

import java.util.ListIterator;
import java.util.logging.Logger;

import fr.loria.parole.artimate.data.Unit;
import fr.loria.parole.artimate.data.UnitDB;
import fr.loria.parole.artimate.data.UnitSequence;
import fr.loria.parole.artimate.engine.GameEngineWrapper;
import fr.loria.parole.artimate.synthesis.Synthesizer;

public class AnimationSynthesizer extends Synthesizer {

	private static final Logger logger = Logger.getLogger(AnimationSynthesizer.class.getName());

	private GameEngineWrapper engine;

	public AnimationSynthesizer(UnitDB unitDB, GameEngineWrapper engine) {
		super(unitDB);
		this.engine = engine;
	}

	public void synthesizeAnimation(UnitSequence targets) {
		// get candidates from UnitDB
		UnitSequence candidates = synthesize(targets);

		// create iterators
		assert targets.size() == candidates.size();
		ListIterator<Unit> targetIterator = targets.iterator();
		ListIterator<Unit> candidateIterator = candidates.iterator();

		// interleaved iteration over targets, candidates
		while (targetIterator.hasNext() && candidateIterator.hasNext()) {
			Unit target = targetIterator.next();
			Unit candidate = candidateIterator.next();

			// deep copy animation from candidate to target
			engine.copyAnimation(candidate, target);
		}

		// play synthesized animation
		engine.playAnimation(targets);
	}

}
