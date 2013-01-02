package edu.tum.cs.ias.labeling.labels.writers;

import java.io.FileNotFoundException;
import java.util.HashMap;


/**
 * Create label file that describes the actions in terms
 * of instances of the respective OWL classes.
 * 
 * The output is an ABOX representation of the action sequence
 * that can be loaded e.g. in the KnowRob knowledge base.
 * 
 * @author Moritz Tenorth, tenorth@cs.tum.edu
 *
 */
public class OwlActionAboxWriter implements LabelWriter {

	@Override
	public void writeToFile(HashMap<Integer, String> actionLabels,
			String filename, int startframe, int endframe) throws FileNotFoundException {
		// TODO implement!
		
	}

}
