package edu.tum.cs.ias.labeling.labels.writers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;


/**
 * Create label file with lines of the form 
 * 139 211 Pick-Cup-From-Table-...
 * 
 * listing the start- and end frame of a segment and a 
 * hyphen-concatenated sequence of labels for each marker
 * set in the time line.
 * 
 * @author Moritz Tenorth, tenorth@cs.tum.edu
 *
 */
public class LabelsDatFileWriter implements LabelWriter {

	@Override
	public void writeToFile(HashMap<Integer, String> actionLabels, String filename, int startframe, int endframe) 
				throws FileNotFoundException {
		
		// open output streamPrintWriter output;
		PrintWriter output = new PrintWriter(new File(filename)); 
  
		// transform the list of markers into a vector of labels
		Vector<Integer> actEndIdx = new Vector<Integer>(actionLabels.keySet());
		Collections.sort(actEndIdx);

		int start = startframe;

		// write the label vector into the file  
		for(int i=0;i<actEndIdx.size();i++) {
			output.print(start +" "+actEndIdx.get(i) +" "+actionLabels.get(actEndIdx.get(i))+"\n");
			start = Integer.valueOf((actEndIdx.get(i).toString()))+1;
		}

		// close the file
		output.close();  
	}
}
