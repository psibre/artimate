package fr.loria.parole.artimate.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.common.io.Files;
import com.google.common.io.PatternFilenameFilter;

/**
 * Carstens AG500 EMA <tt>.pos</tt> file concatenator
 * 
 * @author steiner
 * 
 */
public class PosConcatenator {

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

		File inputDirectory = new File(inputDirectoryName);
		PatternFilenameFilter posFileFilter = new PatternFilenameFilter(".*\\.pos");
		File[] inputFiles = inputDirectory.listFiles(posFileFilter);
		if (inputFiles == null) {
			throw new IOException("Could not find input *.pos files in " + inputDirectoryName);
		}

		File outputFile = new File(outputFileName);
		Files.createParentDirs(outputFile);
		FileOutputStream output = Files.newOutputStreamSupplier(outputFile).getOutput();

		for (File input : inputFiles) {
			Files.copy(input, output);
		}
	}

}
