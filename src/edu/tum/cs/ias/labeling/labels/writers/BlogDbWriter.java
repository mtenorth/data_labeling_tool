package edu.tum.cs.ias.labeling.labels.writers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

import edu.ksu.cis.bnj.ver3.core.BeliefNode;
import edu.tum.cs.ias.labeling.labels.readers.LabelsDatFileReader;
import edu.tum.cs.ias.labeling.labels.readers.YamlConfigReader;
import probcog.srldb.Database;
import probcog.srldb.IRelationArgument;
import probcog.srldb.Link;
import probcog.srldb.Object;
import probcog.srldb.datadict.DDAttribute;
import probcog.srldb.datadict.DDException;
import probcog.srldb.datadict.DDRelation;
import probcog.srldb.datadict.DataDictionary.BLNStructure;


/**
 * Create label file that can be used as training example
 * for learning a BLN of the partial ordering of actions.
 * 
 * @author Moritz Tenorth, tenorth@cs.tum.edu
 *
 */
public class BlogDbWriter implements LabelWriter {


	protected Database db;
	protected HashMap<String, Object> id2action;
	protected HashMap<String, Object> id2object;

	// translations from natural language to roles and concepts
	HashMap<String, String> class2owl = new HashMap<String, String>();
	HashMap<String, String> indiv2owl = new HashMap<String, String>();
	HashMap<String, String> prop2owl  = new HashMap<String, String>();

	protected String curXMLID;
	protected Vector<Link> uncommitedLinks;
	
	String main_dir;
	String sequence_id;
	

	public BlogDbWriter() {
		
		db = new Database();
		id2action = new HashMap<String, Object>();
		id2object = new HashMap<String, Object>();
		uncommitedLinks = new Vector<Link>();
	}



	@Override
	public void writeToFile(HashMap<Integer, String> actionLabels,
			String filename, int startframe, int endframe) throws FileNotFoundException {

		
		main_dir = new File(filename).getParentFile().getParent();
		String label_dir = main_dir + "/annotations/";
		sequence_id = new File(main_dir).getName();
		
		
		// load OWL class mappings
		try {
			YamlConfigReader.loadOwlMapping(main_dir+ "/config/" + "owl-mapping.yaml", this.class2owl, this.indiv2owl, this.prop2owl);
		} catch (IOException e) {}


		String dataset = label_dir;
		String dbdir = dataset+"models/";
		new File(dbdir).mkdir();

		try {

			readLabelsToDB(actionLabels);
				
			// attribute value clustering
			this.commitUncommittedLinks();
			probcog.srldb.datadict.DataDictionary dd = db.getDataDictionary(); 

			// set relation properties, i.e. make belongsTo functional
//			dd.getRelation("objActedOn").setFunctional(new boolean[]{false, true});
			//dd.getRelation("toLocation").setFunctional(new boolean[]{false, true});

			
			// finalize
			this.commitUncommittedLinks();
			db.check();	

			// write training databases
//			db.writeMLNDatabase(new PrintStream(new File(dbdir + "/train.db")));
			db.writeBLOGDatabase(new PrintStream(new File(dbdir + "train.blogdb")));
//			db.writeSRLDB(new FileOutputStream(new File(dbdir + "train.srldb")));


			// TODO: add values to error/group domains
			
			
			// BLN structure
			BLNStructure bs = dd.createBasicBLNStructure();	

			DDAttribute actT_attr     = dd.getAttribute("actionT"); 
			DDAttribute objActOn_attr = dd.getAttribute("objActedOn");
			DDAttribute toLoc_attr    = dd.getAttribute("toLocation");
			DDAttribute fromLoc_att   = dd.getAttribute("fromLocation");
			DDAttribute group_attr    = dd.getAttribute("groupT");
			DDAttribute error_attr    = dd.getAttribute("errorT");
			DDRelation prec_rel       = dd.getRelation("precedes");


			BeliefNode actA1 = bs.getNode(actT_attr);
			actA1.setName("actionT(a1)");

			BeliefNode objActOn1 = bs.getNode(objActOn_attr);
			objActOn1.setName("objActedOn(a1)");

			BeliefNode toLoc1 = bs.getNode(toLoc_attr);
			toLoc1.setName("toLocation(a1)");
			
			BeliefNode fromLoc1 = null;
			if(fromLoc_att!=null) {
				fromLoc1 = bs.getNode(fromLoc_att);
				fromLoc1.setName("fromLocation(a1)");
			} else {
				fromLoc1 = bs.bn.addNode("#fromLocation(a2)");
			}
			
			BeliefNode groupT = bs.getNode(group_attr);
			groupT.setName("groupT(g)");
			
			BeliefNode errorT = bs.getNode(error_attr);
			errorT.setName("errorT(e)");


			BeliefNode actA2     = bs.bn.addNode("#actionT(a2)");
			BeliefNode objActOn2 = bs.bn.addNode("#objActedOn(a2)");
			BeliefNode toLoc2    = bs.bn.addNode("#toLocation(a2)");
			BeliefNode fromLoc2  = bs.bn.addNode("#fromLocation(a2)");


			
			BeliefNode _a1a2 = bs.bn.addDecisionNode("!(a1=a2)");

			
			BeliefNode precedes = bs.getNode(prec_rel);
			precedes.setName("precedes(a1, a2, g, e)");

			bs.bn.bn.connect(_a1a2, precedes);

			bs.bn.bn.connect(actA1, precedes);
			bs.bn.bn.connect(actA2, precedes);
			bs.bn.bn.connect(groupT, precedes);
			bs.bn.bn.connect(errorT, precedes);
			

			// objectActedOn
			bs.bn.bn.connect(objActOn1, precedes);
			bs.bn.bn.connect(objActOn2, precedes);
			
			// toLocation
			bs.bn.bn.connect(toLoc1, precedes);
			bs.bn.bn.connect(toLoc2, precedes);

			// fromLocation
			bs.bn.bn.connect(fromLoc1, precedes);
			bs.bn.bn.connect(fromLoc2, precedes);


			BeliefNode a1a2 = bs.bn.addDecisionNode("a1=a2");
			BeliefNode prec_a1a2 = bs.bn.addNode("precedes(a1, a2, g, e)");
			bs.bn.bn.connect(a1a2, prec_a1a2);

			
			// save
			bs.bn.savePMML(dbdir + "/actionrec.pmml");

			// BLN
			PrintStream bln = new PrintStream(new File(dbdir + "/actionrec.abl"));
			dd.writeBasicBLOGModel(bln);

			System.out.println("BLN output done.");
			

		} catch (IOException e) {
			e.printStackTrace();
		} catch (DDException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}



	public Database commitUncommittedLinks() throws DDException {
		for(Link o : uncommitedLinks)
			o.commit();
		uncommitedLinks.clear();
		return db;
	}



	public void readLabelsToDB(HashMap<Integer, String> actionLabels) throws IOException, DDException, IllegalArgumentException {

		String line = null;

		ArrayList<Object> actionSeq = new ArrayList<Object>();

		Vector<Integer> actEndIdx = new Vector<Integer>(actionLabels.keySet());
		Collections.sort(actEndIdx);

		try {
			Object currentAction, currentObject1, currentObject2, group, error;
			int actionCnt=0;

			
			group = new Object(db, "g", "G_" + sequence_id);
			group.addAttribute("groupT", "Patient");
			
			error = new Object(db, "e", "E_"+ sequence_id);
			error.addAttribute("errorT", "NoError");
			
			
			// write the label vector into the file  
			for(Integer idx : actEndIdx) {
				line = actionLabels.get(idx);

				currentAction = currentObject1 = currentObject2 = null;
				String[] instr = line.split("-");

				if(instr.length<1) continue;

				// check if the first word is in the actions HashMap, otherwise add new action
				String action = instr[0];
				String actionID = Database.upperCaseString(String.format("%s_%s_%02d", action, sequence_id, actionCnt++));

				currentAction = new Object(db, "action", actionID);
				if(!id2action.containsKey(actionID)) {
					id2action.put(actionID, currentAction);
				}
				if(class2owl.containsKey(action.toLowerCase()))
					currentAction.addAttribute("actionT", class2owl.get(action));
				else throw(new IllegalArgumentException("No mapping found for " + action));
				
				actionSeq.add(currentAction);

				if(instr.length>=2) {
					
					// if there's a second word, check if it's in the objects HashMap, otherwise add new object
					String object1 = instr[1];
					
					String object1ID;
					if(class2owl.containsKey(object1.toLowerCase())) {
						object1ID = class2owl.get(object1.toLowerCase());
					} else if(indiv2owl.containsKey(object1.toLowerCase())) {
						object1ID = indiv2owl.get(object1.toLowerCase());
					} else throw(new IllegalArgumentException("No mapping found for " + object1));
					
					
					currentObject1 = new Object(db, "object", object1ID);
					if(!id2object.containsKey(object1ID)) {
						id2object.put(object1ID, currentObject1);
					}
					
					currentAction.addAttribute("objActedOn", object1ID);
					// add the objectActedOn relation between the action and the word
					//uncommitedLinks.add(new Link(db, "objActedOn", currentAction, currentObject1));

				}
				
				if(instr.length>=4) {

					// if there's a third and fourth word, check if it's a valid preposition and an object,
					// create it if necessary
					String prep = instr[2];

					String object2 = instr[3];
//					String object2ID = Database.upperCaseString(String.format("%s_%s_%02d", object2, sequence_id, objectCnt++));
					
					
					String object2ID;
					if(class2owl.containsKey(object2.toLowerCase()))
						object2ID = class2owl.get(object2.toLowerCase());
					else if(indiv2owl.containsKey(object2.toLowerCase()))
						object2ID = indiv2owl.get(object2.toLowerCase());
					else throw(new IllegalArgumentException("No mapping found for " + object2));
					
					currentObject2 = new Object(db, "object", object2ID);
					if(!id2object.containsKey(object2ID)) {
						id2object.put(object2ID, currentObject2);
					}

					currentAction.addAttribute(prop2owl.get(prep.toLowerCase()), object2ID);
					// create link between action and object using prep
//					uncommitedLinks.add(new Link(db, prop2owl.get(prep), currentAction, currentObject2));

				} else {
					
					// add 'none' for all unknown values
					// TODO: extend for all other relations besides toLocation
					
				}
				
				
				// set other attributes to none:
				if(!currentAction.hasAttribute("objActedOn"))
					currentAction.addAttribute("objActedOn", "no_obj");

				if(!currentAction.hasAttribute("toLocation"))
					currentAction.addAttribute("toLocation", "no_to");
				
				if(!currentAction.hasAttribute("fromLocation"))
					currentAction.addAttribute("fromLocation", "no_from");
				
				
				
				if(currentObject1!=null) currentObject1.commit();
				if(currentObject2!=null) currentObject2.commit();
				if(currentAction!=null)  currentAction.commit();
				if(group!=null)  group.commit();
				if(error!=null)  error.commit();

			}

			// add the precedes(a1,a2) relation
			Object a1, a2;
			for(int i1=0;i1<actionSeq.size();i1++) {

				a1=actionSeq.get(i1);
				for(int i2=i1+1;i2<actionSeq.size();i2++) {
					a2=actionSeq.get(i2);
					new Link(db, "precedes", new IRelationArgument[]{a1, a2, group, error}).commit();
				}
			}
		}
		catch(DDException e) {
			throw new RuntimeException(e.getMessage());
		}	
	}
	
	
	public static void main(String[] args) {
		
		// command-line version for reading and parsing labels.dat files and generating BLN files
		
		if(args.length < 1) {
			System.err.println("Usage: batchBlnConversion labels1.dat [labels2.dat ... labels_n.dat]");
		}
		
		for(String file : args) {

			try {
				
				System.out.println("Converting " + file + "...");
				
				HashMap<Integer, String> action_labels = new HashMap<Integer, String>();
				LabelsDatFileReader.loadLabelsDat(new File(file).getParent()+"/", action_labels);
				
				BlogDbWriter writer = new BlogDbWriter();
				writer.writeToFile(action_labels, file, -1, -1);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
}
