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
