package edu.tum.cs.ias.labeling.labels.writers;

import java.io.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.SystemOutDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import edu.tum.cs.ias.labeling.labels.readers.YamlConfigReader;
import edu.tum.cs.ias.knowrob.owl.OWLIndividual;
/**
 * Create label file that describes the actions in terms
 * of instances of the respective OWL classes.
 * 
 * The output is an ABOX representation of the action sequence
 * that can be loaded e.g. in the KnowRob knowledge base.
 * 
 * @author Moritz Tenorth, tenorth@cs.tum.edu
 * @author Asil Kaan Bozcuoglu, asil@cs.uni-bremen.de
 *
 */
public class OwlActionAboxWriter implements LabelWriter {

	
	////////////////////////////////////////////////////////////////////////////////
	// Set IRIs for the ontologies used here
	//

	// Base IRI for KnowRob ontology
	public final static String KNOWROB = "http://knowrob.org/kb/knowrob.owl#";
	public final static String DATALABEL = "http://knowrob.org/kb/datalabel.owl#";

	// Base IRI for OWL ontology
	public final static String OWL = "http://www.w3.org/2002/07/owl#";

	// Base IRI for RDFS
	public final static String RDFS = "http://www.w3.org/2000/01/rdf-schema#";

	// Base IRI for semantic map ontology	
	public final static String IAS_MAP = "http://knowrob.org/kb/ias_semantic_map.owl#";

	// ROS package name for KnowRob
	public final static String KNOWROB_PKG = "ias_knowledge_base";

	// OWL file of the KnowRob ontology (relative to KNOWROB_PKG)
	public final static String KNOWROB_OWL = "owl/knowrob.owl";

	// Namespace of OWL File
	public static String NAMESPACE = "http://knowrob.org/kb/knowrob.owl#";

	// Prefix manager
	public final static DefaultPrefixManager PREFIX_MANAGER = new DefaultPrefixManager(KNOWROB);
	static 
	{
		PREFIX_MANAGER.setPrefix("knowrob:", KNOWROB);
		PREFIX_MANAGER.setPrefix("datalabel:", DATALABEL);
		PREFIX_MANAGER.setPrefix("owl:",    OWL);
		PREFIX_MANAGER.setPrefix("rdfs:", RDFS);
	}




	protected HashMap<String, Object> id2action;
	protected HashMap<String, Object> id2object;

	// translations from natural language to roles and concepts
	HashMap<String, String> class2owl = new HashMap<String, String>();
	HashMap<String, String> indiv2owl = new HashMap<String, String>();
	HashMap<String, String> prop2owl  = new HashMap<String, String>();

	protected String curXMLID;
	
	OWLDataFactory factory;
	OWLOntologyManager manager;
	DefaultPrefixManager pm;

	int inst_counter=0;	// counter to create unique instance identifiers
	
	protected Double startTime = 0.0;
	protected Double framesPerSecond = 6.0;

	public OwlActionAboxWriter(Double framesPerSecond, Double startTime) 
	{
		manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		id2action = new HashMap<String, Object>();
		id2object = new HashMap<String, Object>();
		
		this.framesPerSecond = framesPerSecond;
		this.startTime = startTime;
	}
	
	String getTimestamp(int frame) {
		final Double elapsedSeconds = frame/framesPerSecond;
		final DecimalFormat df = new DecimalFormat("#");
		df.setMaximumFractionDigits(9);
		return df.format(startTime + elapsedSeconds*1000.0);
	}
	
	String getClassName(String instance) {
		if(class2owl.containsKey(instance))
			return class2owl.get(instance);
		
		int index = 0;
		for(index=0; index<instance.length(); index++) {
			char c = instance.charAt(index);
			if(c=='_') break;
			if(c >= '0' && c <= '9') break;
		}
		String className = instance.substring(0,index);
		if(class2owl.containsKey(className))
			return class2owl.get(className);
		else
			return className;
	}

	@Override
	public void writeToFile(HashMap<Integer, String> actionLabels,
			String filename, int startframe, int endframe) throws FileNotFoundException 
	{
		// load OWL class mappings
		String main_dir = new File(filename).getParentFile().getParent(); 
		try {
			YamlConfigReader.loadOwlMapping(main_dir+ "/config/" + "owl-mapping.yaml", this.class2owl, this.indiv2owl, this.prop2owl);
		} catch (IOException e) {}
		
		OWLOntology ontology = null;

		Vector<String> per_frame_labels = new Vector<String>();
		Vector<Integer> per_frame_beginning_frames = new Vector<Integer>();
		Vector<Integer> per_frame_ending_frames = new Vector<Integer>();

		
		String curLabel="";
		int beginningFrame = startframe;		
		for(int f = startframe; f <= endframe; f++) 
		{
			// check if there is a label at the current frame
			if(actionLabels.containsKey(f)) 
			{
				curLabel=actionLabels.get(f);
				
				//put the beginning frame, end frame and the label to 
				per_frame_beginning_frames.add(beginningFrame);
				per_frame_ending_frames.add(f);
				per_frame_labels.add(curLabel);
				
				beginningFrame = f + 1;
			}
			
			
		}
		
		
		try 
		{
			
			// Create ontology manager and data factory
			manager = OWLManager.createOWLOntologyManager();
			factory = manager.getOWLDataFactory();

			// Get prefix manager using the base IRI of the JoystickDrive ontology as default namespace
			pm = PREFIX_MANAGER;

			// Create empty OWL ontology
			ontology = manager.createOntology(IRI.create(NAMESPACE));
			manager.setOntologyFormat(ontology, new RDFXMLOntologyFormat());

			// Import KnowRob ontology
			OWLImportsDeclaration oid = factory.getOWLImportsDeclaration(IRI.create("knowrob"));
			AddImport addImp = new AddImport(ontology,oid);
			manager.applyChange(addImp);

			//Action instances 
			OWLNamedIndividual action_inst = null;
			OWLNamedIndividual prev_action_inst = null;
			
			for(int i = 0; i < per_frame_labels.size(); i++)
			{
				String toBeTokenized = per_frame_labels.get(i);
				int beginFrame = per_frame_beginning_frames.get(i).intValue();
				int endFrame = per_frame_ending_frames.get(i).intValue();
				
				StringTokenizer tokenizer = new StringTokenizer(toBeTokenized, "-");

				String className = "";
				String individualName1 = "";
				String propositionName = "";
				String individualName2 = "";	
				String object_name1 = "";	
				String object_name2 = "";	
				
				int ind = 0;
				
				boolean isStatementReversed = false;
				boolean isClassNameForObject1 = false;
				boolean isClassNameForObject2 = false;
				
				while (tokenizer.hasMoreElements()) 
				{
					String tok = tokenizer.nextToken();
					if(tok.isEmpty() || "none".equals(tok.toLowerCase())) {
						ind += 1;
						continue;
					}
					if(ind == 0)
					{
						className = this.class2owl.get(tok.toLowerCase());
					}
					else if (ind == 1)
					{	
						object_name1 = tok;
						
						individualName1 = this.indiv2owl.get(object_name1.toLowerCase());
						
						if(individualName1 == null)
						{
							individualName1 = this.class2owl.get(object_name1.toLowerCase());
							isClassNameForObject1 = true;
						}
						if(individualName1 == null)
						{
							isClassNameForObject1 = false;
							isStatementReversed = true;
							propositionName = this.prop2owl.get(object_name1.toLowerCase());
						}						
						
						
					}
					else if (ind == 2)
					{
						if(isStatementReversed)
						{
							object_name2 = tok;
							
							individualName2 = this.indiv2owl.get(object_name2.toLowerCase());
							
							if(individualName2 == null)
							{
								individualName2 = this.class2owl.get(object_name2.toLowerCase());
								isClassNameForObject2 = true;
							}
						}
						else
						{
							propositionName = this.prop2owl.get(tok.toLowerCase());
						}
					}
					else if (ind == 3)
					{	
						if(isStatementReversed)
						{
							object_name1 = tok;
							
							individualName1 = this.indiv2owl.get(object_name1.toLowerCase());
							if(individualName1 == null)
							{
								individualName1 = this.class2owl.get(object_name1.toLowerCase());
								isClassNameForObject1 = true;
							}
							
						}
						else
						{
							object_name2 = tok;
						
							individualName2 = this.indiv2owl.get(object_name2.toLowerCase());
							
							if(individualName2 == null)
							{
								individualName2 = this.class2owl.get(object_name2.toLowerCase());
								isClassNameForObject2 = true;
							}
						}
					} else {
						// remove a token in any case to avoid infinite loops
						//tokenizer.nextToken();
					}
					ind++;
				}
				
				// Add labeled action to owl
				// OWLNamedIndividual action_inst = factory.getOWLNamedIndividual("datalabel:label_"+i, pm);
				OWLIndividual action_inst_ind =  OWLIndividual.getOWLIndividualOfClass(className);
				action_inst = factory.getOWLNamedIndividual(action_inst_ind.getLabel(), pm);
				OWLClass action_class = factory.getOWLClass("knowrob:" + className, pm);
				manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(action_class, action_inst));
				
				// Add time info
				OWLNamedIndividual timestamp1 = factory.getOWLNamedIndividual("datalabel:timepoint_"+getTimestamp(beginFrame), pm);
				OWLClass time_class = factory.getOWLClass("knowrob:TimePoint", pm);
				manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(time_class, timestamp1));
				
				OWLNamedIndividual timestamp2 = factory.getOWLNamedIndividual("datalabel:timepoint_"+getTimestamp(endFrame), pm);
				manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(time_class, timestamp2));
				
				OWLObjectProperty startTime = factory.getOWLObjectProperty("knowrob:startTime", pm);
				manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(startTime,  action_inst, timestamp1));
				
				OWLObjectProperty endTime = factory.getOWLObjectProperty("knowrob:endTime", pm);
				manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(endTime,  action_inst, timestamp2));
				
				//Add object info
				OWLClass object_class = null;
				OWLNamedIndividual object_inst = null;
				
				if(isClassNameForObject1)
				{
					object_class = factory.getOWLClass("knowrob:" + individualName1, pm);
					OWLIndividual object_inst_ind =  OWLIndividual.getOWLIndividualOfClass(individualName1);
					object_inst = factory.getOWLNamedIndividual("datalabel:" + object_inst_ind.getLabel(), pm);
					manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(object_class, object_inst));
				}
				else
				{
					object_class = factory.getOWLClass("knowrob:" + getClassName(individualName1), pm);
					object_inst = factory.getOWLNamedIndividual("knowrob:" + individualName1, pm);
					manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(object_class, object_inst));
				}
					
				OWLObjectProperty objectActedOn = factory.getOWLObjectProperty("datalabel:objectActedOn", pm);
				manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(objectActedOn, action_inst, object_inst));
				
				//Add proposition info if exists
				if(object_name2 != null && !(object_name2.equals("")) && !(object_name2.equals("null")) && !(object_name2.equals("None")))
				{
					OWLClass object_class2 = null;
					OWLNamedIndividual object_inst2 = null;

					if(isClassNameForObject2)
					{
						object_class2 = factory.getOWLClass("knowrob:" + individualName2, pm);
						
						OWLIndividual object_inst_ind2 =  OWLIndividual.getOWLIndividualOfClass(individualName2);
						object_inst2 = factory.getOWLNamedIndividual("datalabel:" + object_inst_ind2.getLabel(), pm);
						manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(object_class2, object_inst2));
					}
					else
					{
						object_class2 = factory.getOWLClass("knowrob:" + getClassName(individualName2), pm);
						
						object_inst2 = factory.getOWLNamedIndividual("knowrob:" + individualName2, pm);
						manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(object_class2, object_inst2));
					}
					
					OWLObjectProperty eventOccursAt = factory.getOWLObjectProperty("knowrob:" + propositionName, pm);
					manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(eventOccursAt, action_inst, object_inst2));
				}
				
				//Add neighboring actions if exists
				if (prev_action_inst != null)
				{
					OWLObjectProperty previousAction = factory.getOWLObjectProperty("knowrob:nextEvent", pm);
					manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(previousAction, prev_action_inst, action_inst));

					OWLObjectProperty nextAction = factory.getOWLObjectProperty("knowrob:previousEvent", pm);
					manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(nextAction, action_inst, prev_action_inst));
				}

				prev_action_inst = action_inst;
			}
			File output = new File(main_dir + "/annotations/labels.owl");
			IRI documentIRI2 = IRI.create(output);
			manager.saveOntology(ontology, new RDFXMLOntologyFormat(), documentIRI2);
			manager.saveOntology(ontology, new SystemOutDocumentTarget());
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}	
	
}
