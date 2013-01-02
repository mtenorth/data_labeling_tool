package edu.tum.cs.ias.labeling.labels.writers;

import java.io.FileNotFoundException;
import java.util.HashMap;


/**
 * 
 * Interface for label output writers that accept a sequence of
 * action labels and output them to a file.
 * 
 * @author Moritz Tenorth, tenorth@cs.tum.edu
 *
 */
public interface LabelWriter {

	public void writeToFile(HashMap<Integer, String> actionLabels, String filename, int startframe, int endframe) 
				throws FileNotFoundException;
	
}
