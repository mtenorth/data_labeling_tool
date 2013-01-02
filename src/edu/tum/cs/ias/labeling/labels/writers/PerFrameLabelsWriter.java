package edu.tum.cs.ias.labeling.labels.writers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;

/**
 * Create label file with lines of the form
 *  
 * Pick-Cup-From-Table-...
 * Pick-Cup-From-Table-...
 * Pick-Cup-From-Table-...
 * Put-Cup-On-Counter-...
 * ...
 * 
 * For each frame in the video, there is one line in the file
 * that contains the respective annotation. 
 * 
 * @author Moritz Tenorth, tenorth@cs.tum.edu
 *
 */
public class PerFrameLabelsWriter implements LabelWriter{

	@Override
	public void writeToFile(HashMap<Integer, String> actionLabels,
			String filename, int startframe, int endframe) throws FileNotFoundException {

		
		// iterate over the frames backward and write the per-frame labels into the file
		// (backward since we annotate the end of each segment)
		
		Vector<String> per_frame_labels = new Vector<String>();
		String curLabel="";
		
		for(int f=endframe;f>=startframe;f--) {

			// check if there is a label at the current position
			if(actionLabels.containsKey(f)) {
				curLabel=actionLabels.get(f);
			}
			if(!curLabel.equals(""))
				per_frame_labels.add(curLabel);
		}

		// write the per_frame labels into the file in opposite direction
		PrintWriter output = new PrintWriter(new File(filename+".frames"));
		for(int o=per_frame_labels.size()-1;o>=0;o--){ 
			output.print(per_frame_labels.get(o)+"\n");
		}
		output.close(); 
		
	}

}
