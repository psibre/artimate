package fr.loria.parole.artimate.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.google.common.io.Files;
import com.google.common.io.PatternFilenameFilter;

/**
 * Carstens AG500 EMA <tt>.pos</tt> file concatenator
 * 
 * @author steiner
 * 
 */
public class PosConcatenator {

	static Logger logger = Logger.getLogger(PosConcatenator.class);

	/**
	 * Concatenate all <tt>*.pos</tt> files in directory <code>arg[0]</code> to a single target <tt>.pos</tt> file
	 * <code>args[1]</code>.
	 * 
	 * @param args
	 *            source directory, target file
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
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
		PatternFilenameFilter posFileFilter = new PatternFilenameFilter(".*\\.pos");
		File[] inputFiles = inputDirectory.listFiles(posFileFilter);
		if (inputFiles == null) {
			logger.error("Could not find input *.pos files in " + inputDirectoryName);
			throw new IOException();
		}
		logger.debug("Found " + inputFiles.length + " input files");

		File outputFile = new File(outputFileName);
		Files.createParentDirs(outputFile);
		FileOutputStream output = Files.newOutputStreamSupplier(outputFile).getOutput();
		logger.info("Appending to " + outputFileName);

		for (File input : inputFiles) {
			Files.copy(input, output);
			logger.debug("Appended " + input.getName());
		}
	}

}
