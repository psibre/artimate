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

import java.util.EnumMap;

import com.google.common.base.Objects;

public class Unit {
	protected EnumMap<Features, Object> features = new EnumMap<Features, Object>(Features.class);

	public Unit() {
	}

	public Unit(double start, double end) {
		this(start, end, "");
	}

	public Unit(double start, double end, String label) {
		setStart(start);
		setEnd(end);
		setLabel(label);
	}

	public double getStart() {
		Double start = (Double) features.get(Features.START);
		return start;
	}

	public double getEnd() {
		Double end = (Double) features.get(Features.END);
		return end;
	}

	public double getDuration() {
		double start = getStart();
		double end = getEnd();
		double duration = end - start;
		return duration;
	}

	public String getLabel() {
		String label = (String) features.get(Features.LABEL);
		return label;
	}

	public int getIndex() {
		int index = (Integer) features.get(Features.INDEX);
		return index;
	}

	public Object getAnimation() {
		Object animation = features.get(Features.ANIMATION);
		return animation;
	}

	protected void setStart(double start) {
		features.put(Features.START, start);
	}

	protected void setEnd(double end) {
		features.put(Features.END, end);
	}

	protected void setLabel(String label) {
		features.put(Features.LABEL, label);
	}

	public void setIndex(int index) {
		features.put(Features.INDEX, index);
	}

	public void setAnimation(Object value) {
		features.put(Features.ANIMATION, value);
	}

	@Override
	public String toString() {
		String id = Objects.toStringHelper(this).addValue(features).toString();
		return id;
	}
}
