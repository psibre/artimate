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
package fr.loria.parole.artimate.data.io;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import fr.loria.parole.artimate.data.Unit;
import fr.loria.parole.artimate.data.UnitSequence;

import jregex.Matcher;
import jregex.Pattern;

public class XWavesSegmentation extends UnitSequence {

	private final static Pattern LINE_PATTERN = new Pattern("\\s*({end}\\d+(\\.\\d+)?)\\s+\\d+\\s+({label}.*)\\s*");

	private static final Logger logger = Logger.getLogger(XWavesSegmentation.class.getName());

	public XWavesSegmentation(String fileName) throws Exception {
		super();
		load(fileName);
	}

	public void load(String fileName) throws Exception {
		URL url = getClass().getResource("/" + fileName);
		List<String> lines = Resources.readLines(url, Charsets.UTF_8);

		boolean header = true;
		float lastEndTime = 0;
		Matcher matcher = LINE_PATTERN.matcher();
		int index = 0;
		for (String line : lines) {
			// end of header?
			if (line.trim().equals("#")) {
				header = false;
				continue;
			}
			// ignore header line
			if (header) {
				continue;
			}

			// no longer in header, parse line
			if (!matcher.matches(line)) {
				throw new Exception("Could not parse line: " + line);
			}
			String label = matcher.group("label");
			String end = matcher.group("end");

			// sanity check for valid end times
			float endTime = 0;
			try {
				endTime = Float.parseFloat(end);
			} catch (NumberFormatException e) {
				throw new Exception("File not well-formed, could not parse end time: " + end);
			}
			if (endTime < lastEndTime) {
				throw new Exception("Unit end times are not in ascending order!");
			}

			// convert to frame number and append new segment
			Unit segment = new Unit(lastEndTime, endTime, label);
			segment.setIndex(index++);
			units.add(segment);
			lastEndTime = endTime;
		}
		logger.info("Loaded Xwaves lab file " + fileName);
	}
}
