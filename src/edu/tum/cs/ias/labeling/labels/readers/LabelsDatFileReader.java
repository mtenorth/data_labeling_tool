package edu.tum.cs.ias.labeling.labels.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;


/**
 * Read a labels.dat file and add the labels to the action_labels
 * hash map.
 * 
 * @author Moritz Tenorth, tenorth@cs.tum.edu
 */
public class LabelsDatFileReader {

	public static void loadLabelsDat(String pathname, Map<Integer, String> action_labels) 
		throws IOException {

	    BufferedReader reader = new BufferedReader(new FileReader(new File(pathname+"labels.dat")));
	    
	    String line;
	    while((line = reader.readLine())!= null)
	    {
			int endmarker = Integer.valueOf(line.split(" ")[1]);
			String label  = line.split(" ")[2];
			action_labels.put(endmarker, label);
	    }
	}
}
