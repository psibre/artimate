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
