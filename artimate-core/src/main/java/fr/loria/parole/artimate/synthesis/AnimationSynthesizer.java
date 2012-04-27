/*
 * #%L
 * Artimate Core
 * %%
 * Copyright (C) 2011 - 2012 INRIA
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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
