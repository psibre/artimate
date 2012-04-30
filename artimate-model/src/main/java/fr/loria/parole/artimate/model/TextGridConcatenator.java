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
