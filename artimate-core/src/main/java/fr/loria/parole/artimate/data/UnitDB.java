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

import java.util.List;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableListMultimap.Builder;
import com.google.common.collect.Multimap;

public class UnitDB {

	protected Multimap<String, Unit> units;

	public UnitDB(UnitSequence timeline) {
		Builder<String, Unit> builder = new ImmutableListMultimap.Builder<String, Unit>();
		for (Unit unit : timeline) {
			builder.put(unit.getLabel(), unit);
		}
		units = builder.build();
	}

	public List<Unit> getUnitList(String key) {
		List<Unit> list = (List<Unit>) units.get(key);
		return list;
	}
}
