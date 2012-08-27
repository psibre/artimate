/*
 * #%L
 * Artimate Model Compiler
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
package fr.loria.parole.artimate.model;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.praat.PraatFile;
import org.praat.PraatTextFile;
import org.praat.TextGrid;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.PatternFilenameFilter;

import fr.loria.parole.labelutils.Annotation;

/**
 * Praat <tt>.TextGrid</tt> file concatenator, with conversion to XWaves <tt>.lab</tt> format
 * 
 * @author steiner
 * 
 */
public class TextGridConcatenator {

	static Logger logger = Logger.getLogger(TextGridConcatenator.class);

	/**
	 * Concatenate all <tt>*.TextGrid</tt> files in directory <code>arg[0]</code> to a single target <tt>.lab</tt> file
	 * <code>args[1]</code>.
	 * 
	 * @param args
	 *            source directory, target file
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String inputDirectoryName = args[0];
		String outputFileName = args[1];

		// configure logging
		String logFormat = System.getProperty("log.format");
		String logLevel = System.getProperty("log.level");
		if (logFormat == null || logFormat.isEmpty()) {
			logFormat = "[%c{1}] [%p] %m%n";
		}
		ConsoleAppender consoleAppender = new ConsoleAppender(new PatternLayout(logFormat));
		BasicConfigurator.configure(consoleAppender);
		Level level = Level.toLevel(logLevel);
		Logger.getRootLogger().setLevel(level);

		File inputDirectory = new File(inputDirectoryName);
		PatternFilenameFilter tgFileFilter = new PatternFilenameFilter(".*\\.TextGrid");
		File[] inputFiles = inputDirectory.listFiles(tgFileFilter);
		if (inputFiles == null) {
			logger.error("Could not find input *.TextGrid files in " + inputDirectoryName);
			throw new IOException();
		}
		logger.debug("Found " + inputFiles.length + " input files");

		File outputFile = new File(outputFileName);
		Files.createParentDirs(outputFile);
		logger.info("Appending to " + outputFileName);

		Annotation annotation = null;
		for (File input : inputFiles) {
			TextGrid inputTextGrid = (TextGrid) new PraatTextFile().read(input);
			if (annotation == null) {
				annotation = new Annotation(inputTextGrid);
			} else {
				annotation.append(new Annotation(inputTextGrid));
			}

			logger.debug("Appended " + input.getName());
		}
		TextGrid outputTextGrid = annotation.toTextGrid();
		PraatFile.writeText(outputTextGrid, outputFile, Charsets.UTF_8);
	}

}
