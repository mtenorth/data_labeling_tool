package edu.tum.cs.ias.labeling;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.google.common.base.Joiner;

import controlP5.ControlEvent;
import controlP5.ControlGroup;
import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.ScrollList;
import controlP5.Slider;
import controlP5.Textfield;

import processing.core.PApplet;
import processing.core.PImage;
import codeanticode.gsvideo.*;
import edu.tum.cs.ias.labeling.labels.readers.LabelsDatFileReader;
import edu.tum.cs.ias.labeling.labels.readers.YamlConfigReader;
import edu.tum.cs.ias.labeling.labels.writers.LabelWriter;
import edu.tum.cs.ias.labeling.labels.writers.LabelsDatFileWriter;
import edu.tum.cs.ias.labeling.labels.writers.OwlActionAboxWriter;
import edu.tum.cs.ias.labeling.labels.writers.PerFrameLabelsWriter;



public class DataLabeling extends PApplet {

	private static final long serialVersionUID = 6676699230725174424L;

	ControlP5 controlP5;

	int myColorBackground = color(0,0,0);
	PImage cam0;
	

	GSMovie movie;
	String movieFile = "untitled.ogv";
	boolean playMovie = false;

	int frame = 1;
	int startframe = 1;
	int endframe = 20;
	
	ArrayList<Integer> colors = new ArrayList<Integer>();
	
	LinkedHashMap<String, String> label_categories;
	LinkedHashMap<String, List<String>> label_values;
	YamlConfigReader.Config cfg;

	HashMap<String, Integer>   action2ID = new HashMap<String, Integer>();
	HashMap<Integer, String>   actionLabels = new HashMap<Integer, String>();
	
	
	String[] current_label;

	// ID definitions:
	private static int START_ID = 100;


	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	// GUI SETUP
	// 

	public void setup() {
		
		size(1000,700);
		controlP5 = new ControlP5(this);
		label_categories = new LinkedHashMap<String, String>();
		label_values = new LinkedHashMap<String, List<String>>();


		// load movie file
		GSVideo.localGStreamerPath = sketchPath+"/gstreamer/windows32";
		String movie_file = selectAndloadMovieFile();

		
		
		// guess main folder, assume standard layout (video in main/video/... folder
		String main_folder = new File(movie_file).getParentFile().getParent();
		String labelPath = main_folder + "/annotations/";
		String configPath = main_folder + "/config/";
		
		try {
			// check if the guessed path was correct and contains a config.yaml
			if(new File(configPath+"config.yaml").isFile()) {
				cfg = YamlConfigReader.loadConfigFile(configPath + "config.yaml", label_categories, label_values);
				
			} else {
				
				// ask the user for the config file
				String configFile = selectConfigFile();
				cfg = YamlConfigReader.loadConfigFile(configFile, label_categories, label_values);
				
				
				main_folder = new File(configFile).getParentFile().getParent();
				labelPath = main_folder + "annotations/";
				configPath = main_folder + "config/";
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		
		// try to load existing labels.dat file in output folder, 
		// fail silently if it does not exist
		try {
			LabelsDatFileReader.loadLabelsDat(labelPath, this.actionLabels);
			
		} catch (IOException e) { }

		
		
		// build the GUI elements		
		
		this.current_label = new String[label_categories.keySet().size()];
		for(int i=0;i<current_label.length;i++)
			current_label[i]="";
		
		// build inverse lookup table name->id
		for(int k=0;k<label_values.get(cfg.action_category).size();k++) {
			action2ID.put(label_values.get(cfg.action_category).get(k), k);
		}
		
		createControlElements(labelPath);

	}



	/////////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	// GUI UPDATE
	//


	public void draw() {

		background(0);

		// draw the camera images
		image(movie, 40, 30,   320, 240);


		// draw the time line
		fill(64); noStroke();
		rect(0, 300, 1000, 45);

		stroke(0);
		line(40, 325, 40, 335);
		line(40, 330, 960,330);

		drawLabelSequence(actionLabels, 40, 330, 920);

	}


	void drawLabelSequence(HashMap<Integer, String> labels, float x, float y, float width) {

		strokeWeight(2);

		Iterator<Map.Entry<Integer, String>> i = labels.entrySet().iterator();
		while (i.hasNext()) {

			Map.Entry<Integer, String> me = i.next();

			String ac = split(me.getValue().toString(), '-')[0];
			String id = action2ID.get(ac).toString();
			stroke(Integer.valueOf(colors.get(Integer.valueOf(id) % colors.size()).toString()));

			float px_x = x + (float) (Integer.valueOf(me.getKey().toString()) - startframe) / (float)(endframe-startframe)   * width;
			line(px_x, y-3, px_x, y+3);
		}
		strokeWeight(1);
	}



	/////////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	// EVENT HANDLING
	// 


	public void movieEvent(GSMovie movie) {
		movie.read();
		
		// if the movie is playing normally, increase frame counter
		if(playMovie) {
			this.frame++;
			//controlP5.controller("frame").setValue(this.frame);
		}
	}


	public void controlEvent(ControlEvent theEvent) {

		if(theEvent.name().equals("label")) {return;}
		if(theEvent.name().equals("directory")) {return;}
		if(theEvent.name().equals("filename")) {return;}

		// jump to video position when clicking on the slider
		if(theEvent.name().equals("frame")) {
			movie.play();
			movie.jump(frame);
			movie.pause();
			
			if(actionLabels.containsKey(frame)) {
				((Textfield)controlP5.controller("label")).setValue(actionLabels.get(frame));
			}
		}


		///////////////////////////////////////////////////////////////////////////////
		// commit button

		if(theEvent.name().equals("commit")) {

			// build label string with '-' and set the marker
			actionLabels.put(frame, Joiner.on("-").join(this.current_label));
			return;
		}

		///////////////////////////////////////////////////////////////////////////////
		// load button  

		if(theEvent.name().equals("load")) {

			// determine image range
			startframe = Integer.valueOf(((Textfield) controlP5.controller("start")).getText());
			endframe = Integer.valueOf(((Textfield) controlP5.controller("end")).getText());
			frame = startframe;

			// set path for loading images
			movieFile = ((Textfield) controlP5.controller("directory")).getText();

			// adjust the slider
			((Slider)controlP5.controller("frame")).setMin(startframe);
			((Slider)controlP5.controller("frame")).setMax(endframe);
			((Slider)controlP5.controller("frame")).setValue(startframe);


			// clear previously created labels
			actionLabels.clear();
			return;


			///////////////////////////////////////////////////////////////////////////////
			// save button

		} else if(theEvent.name().equals("save")) {
			
			// save the information to the file
			String filename = ((Textfield) controlP5.controller("filename")).getText();
			saveToFile(filename);
			return;
		}

		///////////////////////////////////////////////////////////////////////////////
		// label buttons

		if(!theEvent.isGroup() && (!theEvent.name().equals("frame"))) {

			// actual button events, not the click on the button group
			
			// 
			int global_id = ((int) theEvent.value() - START_ID) / START_ID;
				
			if(!theEvent.name().equals("none")) {
				this.current_label[global_id] = theEvent.name();
			} else {
				this.current_label[global_id] = "";
			}

			if(theEvent.value()>=0) {  
				((Textfield)controlP5.controller("label")).setValue(Joiner.on(" ").join(this.current_label));
			}
		}



	}


	public void keyPressed() {	    

		if (movie.isSeeking()) return;

		// scroll with the arrow keys
		if (keyCode == RIGHT) {

			if (frame < movie.length() - 1){
				frame++;
				movie.play();
				movie.jump(frame);
				movie.pause();
				playMovie=false;
			}
			controlP5.controller("frame").setValue(frame);
			if(actionLabels.containsKey(frame)) {
				((Textfield)controlP5.controller("label")).setValue(actionLabels.get(frame));
			}
			return;
		}
		if (keyCode == LEFT) {

			if (0 < frame) {
				frame--; 
				movie.play();
				movie.jump(frame);
				movie.pause();
				playMovie=false;
			}
			controlP5.controller("frame").setValue(frame);
			if(actionLabels.containsKey(frame)) {
				((Textfield)controlP5.controller("label")).setValue(actionLabels.get(frame));
			}
			return;
		}

		// jump to next marker
		if (keyCode == KeyEvent.VK_PAGE_UP) {

			Vector<Integer> actEndIdx = new Vector<Integer>(actionLabels.keySet());
			Collections.sort(actEndIdx);

			// set to the last frame if we are already beyond the marker-ed area
			if(frame>=Integer.valueOf(actEndIdx.get(actEndIdx.size()-1).toString())) {
				frame=endframe; controlP5.controller("frame").setValue(frame);
				return;
			}

			for(int i=0;i<actEndIdx.size();i++) {
				if(Integer.valueOf(actEndIdx.get(i).toString())>frame+1) {
					frame=Integer.valueOf(actEndIdx.get(i).toString());
					controlP5.controller("frame").setValue(frame);
					
					if(actionLabels.containsKey(frame)) {
						((Textfield)controlP5.controller("label")).setValue(actionLabels.get(frame));
					}
					
					return;
				}
			}
			return;
		}

		// jump to previous marker
		if (keyCode == KeyEvent.VK_PAGE_DOWN) {

			Vector<Integer> actEndIdx = new Vector<Integer>(actionLabels.keySet());
			Collections.sort(actEndIdx);

			//set to 0 if we are before the marker-ed area
			if(frame<=Integer.valueOf(actEndIdx.get(0).toString())) {
				frame=1; controlP5.controller("frame").setValue(frame);
				return;
			}

			for(int i=actEndIdx.size()-1;i>=0;i--) {
				if(Integer.valueOf(actEndIdx.get(i).toString())<frame-1) {
					frame=Integer.valueOf(actEndIdx.get(i).toString());
					controlP5.controller("frame").setValue(frame);
					
					if(actionLabels.containsKey(frame)) {
						((Textfield)controlP5.controller("label")).setValue(actionLabels.get(frame));
					}
					
					return;
				}
			}
			return;
		}

		// commit with enter
		if (keyCode == ENTER) {
			actionLabels.put(frame, Joiner.on("-").join(this.current_label));
			return;
		}
		
		// set action to 'none' with ESCAPE
		if (key == ESC) {
			this.current_label = new String[label_categories.keySet().size()];		for(int i=0;i<current_label.length;i++)
				current_label[i]="";
			((Textfield)controlP5.controller("label")).setValue(Joiner.on(" ").join(this.current_label));
			return;
		}

		// remove marker with del
		if (keyCode == DELETE && actionLabels.containsKey(frame)) {
			actionLabels.remove(frame);
			return;
		}

		// toggle play/pause with SPACE
		if (key == ' ') {
			
			if(!playMovie) {
				playMovie=true;
				movie.play();
			} else {
				playMovie=false;
				movie.pause();
				controlP5.controller("frame").setValue(frame);
			}
			return;
		}


	}


	


	/////////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	// INITIALIZATION AND SETUP
	// 
	

	private void createControlElements(String folderPathOut) {
		
		
		///////////////////////////////////////////////////////////////////////////////
		// button lists

		controlP5.addTextfield("label",50,360,420,20).setId(12345);
		controlP5.addButton("commit",1,490,360,120,20);

		int scr_list_x = 50;
		int scr_list_y = 420;
		
		int id = START_ID;
		// create one scroll-list for each category:
		for(String cat: label_categories.keySet()) {
			
			ScrollList act = controlP5.addScrollList(cat,scr_list_x,scr_list_y,120,250);
			act.setLabel(label_categories.get(cat));

			for(int i=0;i<label_values.get(cat).size();i++) {
				act.addItem(label_values.get(cat).get(i), id+i);
			}
			
			id+=100;
			scr_list_x += 150;
		}


		///////////////////////////////////////////////////////////////////////////////  
		// slider and timeline

		controlP5.addSlider("frame",startframe,endframe,40,310,920,10);


		///////////////////////////////////////////////////////////////////////////////
		// loading/saving

		controlP5.setColorActive(200);


		ControlGroup l2 = controlP5.addGroup("save labels",450,160,500);

		Textfield dbfield = controlP5.addTextfield("filename", 0,5,380,20);
		dbfield.setValue(folderPathOut + "labels.dat");
		dbfield.setGroup(l2);

		Controller savebutton = controlP5.addButton("save",1,400,5,100,20);
		savebutton.setGroup(l2);
		

		// fill color mapping
		colors = new ArrayList<Integer>();

		colors.add(0xFF00CC99);  // cyan
		colors.add(0xFFFFFFFF);  // white
		colors.add(0xFFFF9900);  // yellow
		colors.add(0xFFFF3300);  // orange
		colors.add(0xFF990000);  // red
		colors.add(0xFF0033FF);  // light blue
		colors.add(0xFF000099);  // dark blue
		colors.add(0xFFFF0099);  // pink
		colors.add(0xFF660066);  // purple
		colors.add(0xFF009900);  // green
		colors.add(0xFF00FF00);  // light green
	}



	private String selectAndloadMovieFile() {
		String inFile = selectInput();
		if (inFile == null) {

			println("No file was selected...");

			// try to read default file:
			movie = new GSMovie(this, movieFile);
			movie.read();

		} else {
			this.movieFile = inFile;
			movie = new GSMovie(this, movieFile);
			movie.read();
		}

		// Pausing the video at the first frame. 
		movie.play();
		movie.jump(startframe);
		movie.pause();
		this.endframe = (int) movie.length();
		System.out.println(movie.length());
		
		return inFile;
	}


	// dialog for selecting the config.yaml
	private String selectConfigFile() {

		String config_file = selectInput("Please select the config.yaml file");
		return config_file;
	}
	
	

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	// DATA EXPORT
	// 
	
	/**
	 * Export using all exporters in edu.tum.cs.ias.labeling.labels.writers
	 * 
	 * @param filename Filename that will be extended with extensions depending on the exported file type
	 */
	void saveToFile(String filename) {

		try {
			
			LabelWriter out = new LabelsDatFileWriter();
			out.writeToFile(this.actionLabels, filename, this.startframe, this.endframe);

			out = new PerFrameLabelsWriter();
			out.writeToFile(this.actionLabels, filename, this.startframe, this.endframe);

			//out = new BlogDbWriter();
			//out.writeToFile(this.actionLabels, filename, this.startframe, this.endframe);
			
			out = new OwlActionAboxWriter(cfg.fps, cfg.startTime);
			out.writeToFile(this.actionLabels, filename, this.startframe, this.endframe);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String _args[]) {
		PApplet.main(new String[] { edu.tum.cs.ias.labeling.DataLabeling.class.getName() });
	}
}
