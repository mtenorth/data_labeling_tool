package edu.tum.cs.ias.labeling.labels.readers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;



/**
 * Configuration file reader for the labeling tool, reading 
 * the set of labels and the different label categories from
 * a YAML file using SnakeYAML
 * 
 * @see http://code.google.com/p/snakeyaml/wiki/Documentation
 * @author Moritz Tenorth, tenorth@cs.tum.edu
 *
 */
public class YamlConfigReader {
	public static class Config {
		public String action_category = "";
		public Double fps = 6.0;
		public Double startTime = 0.0;
	}

	/**
	 * Read the YAML config file containing the sets of label categories
	 * and the labels that can be assigned
	 * 
	 * @param filename Name of the .yaml configuration files
	 * @param label_categories Map that will be extended with the label categories read from the yaml file
	 * @param label_values Map that will be extended with the label values read from the yaml file
	 * @return Name of the action category (the primary label category) 
	 * @throws FileNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public static Config loadConfigFile(String filename, Map<String, String> label_categories, 
			Map<String, List<String>> label_values) throws FileNotFoundException {

		InputStream input;
		input = new FileInputStream(new File(filename));

		Yaml yaml = new Yaml();
		Object config = (HashMap<String, List<String>>) yaml.load(input);

		label_categories.putAll(((Map<String, Map<String, String>>) config).get("label-categories"));
		label_values.putAll(((Map<String, Map<String, List<String>>>) config).get("label-values"));
		
		final Config cfg = new Config();
		cfg.action_category = ((HashMap<String, String>) config).get("action-category");
		cfg.fps = ((HashMap<String, Double>) config).get("fps");
		cfg.startTime = ((HashMap<String, Double>) config).get("start-time");

		return cfg;

	}


	@SuppressWarnings("unchecked")
	public static void loadOwlMapping(String filename, Map<String, String> class2owl, 
			Map<String, String> indiv2owl, Map<String, String> prop2owl) throws FileNotFoundException {

		InputStream input;
		input = new FileInputStream(new File(filename));

		Yaml yaml = new Yaml();
		try {
			Object mapping = (HashMap<String, List<String>>) yaml.load(input);
			class2owl.putAll(((Map<String, Map<String, String>>) mapping).get("classes"));			
			prop2owl.putAll(((Map<String, Map<String, String>>) mapping).get("properties"));	
			indiv2owl.putAll(((Map<String, Map<String, String>>) mapping).get("individuals"));		
		} catch (Exception e) {
			e.printStackTrace();
		}		

	}


}
