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
