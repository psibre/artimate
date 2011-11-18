package fr.loria.parole.tonguedemo2.io;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import fr.loria.parole.tonguedemo2.segmentation.Segment;

import jregex.Matcher;
import jregex.Pattern;

public class XWavesSegmentation {

	private final static Pattern LINE_PATTERN = new Pattern("\\s*({end}\\d+(\\.\\d+)?)\\s+\\d+\\s+({label}.*)\\s*");

	private static final Logger logger = Logger.getLogger(XWavesSegmentation.class.getName());

	protected ArrayList<Segment> segments;

	public XWavesSegmentation(String fileName) throws Exception {
		load(fileName);
	}

	public void load(String fileName) throws Exception {
		InputStream fileStream = getClass().getResourceAsStream("/" + fileName);
		load(fileStream);
		logger.info("Loaded Xwaves lab file " + fileName);
	}

	private void load(InputStream inputStream) throws Exception {
		List<String> lines = IOUtils.readLines(inputStream);
		segments = new ArrayList<Segment>(lines.size());

		boolean header = true;
		float lastEndTime = 0;
		Matcher matcher = LINE_PATTERN.matcher();
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
				throw new Exception("Segment end times are not in ascending order!");
			}

			// convert to frame number and append new segment
			Segment segment = new Segment(lastEndTime, endTime, label);
			segments.add(segment);
			lastEndTime = endTime;
		}
	}

	public ArrayList<Segment> getSegments() {
		return segments;
	}
}
