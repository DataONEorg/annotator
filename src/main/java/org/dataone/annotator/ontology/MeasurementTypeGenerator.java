package org.dataone.annotator.ontology;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.annotator.generator.AnnotationGenerator;

import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.ontology.SomeValuesFromRestriction;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class MeasurementTypeGenerator {
	
	private static Log log = LogFactory.getLog(MeasurementTypeGenerator.class);

	public static String ecso = "https://raw.githubusercontent.com/DataONEorg/sem-prov-ontologies/master/observation/d1-ECSO.owl";
	public static String ecsoPrefix = "http://purl.dataone.org/odo/ECSO_";
	public static String taxaPrefix = "http://purl.dataone.org/odo/TAXA_";

	
	private OntModel ecsoModel = null;
	private Map<String, String> namespaces = new HashMap<String, String>();
	private int classId = 1000;

	public MeasurementTypeGenerator() {
		
		// prep the namespace prefixes
		namespaces.put("ecso", ecsoPrefix);
		namespaces.put("taxa", taxaPrefix);
		namespaces.put("oboe", AnnotationGenerator.oboe);
		namespaces.put("oboe-core", AnnotationGenerator.oboe_core);
		namespaces.put("oboe-characteristics", AnnotationGenerator.oboe_characteristics);
		
		// retrieve the ECSO ontology
		ecsoModel = ModelFactory.createOntologyModel();
		ecsoModel.read(ecso);
		
		AnnotationGenerator.initializeCache();

		// construct the ontology model for additions
		m = ModelFactory.createOntologyModel();
		
		Ontology ont = m.createOntology(ecso);
		ont.addImport(m.createResource(AnnotationGenerator.oboe));
		m.addSubModel(OntDocumentManager.getInstance().getModel(AnnotationGenerator.oboe));
		
		// properties
		rdfsLabel = ecsoModel.getProperty(AnnotationGenerator.rdfs + "label");
		
		measuresCharacteristic = ecsoModel.getObjectProperty(AnnotationGenerator.oboe_core + "measuresCharacteristic");
		measuresEntity = ecsoModel.getObjectProperty(AnnotationGenerator.oboe_core + "measuresEntity");

		// classes
		entityClass =  ecsoModel.getOntClass(AnnotationGenerator.oboe_core + "Entity");
		characteristicClass = ecsoModel.getOntClass(AnnotationGenerator.oboe_core + "Characteristic");
		measurementTypeClass =  ecsoModel.getOntClass(AnnotationGenerator.oboe_core + "MeasurementType");
		
	}
	
	private OntModel m = null;
	private Property rdfsLabel = null;
	private ObjectProperty measuresCharacteristic = null;
	private ObjectProperty measuresEntity = null;

	// classes
	private OntClass entityClass =  null;
	private OntClass characteristicClass = null;
	private OntClass measurementTypeClass =  null;
	

	public OntClass generateMeasurementType(String entityLabel, String characteristicLabel) {
		
		// create the measurement type from entity and characteristics given
		String measurementTypeLabel = this.getFragment(entityLabel) + " " + this.getFragment(characteristicLabel);
		
		// check if we have it already
		String existingType = this.lookupConcept("ecso:" + measurementTypeLabel);
		if (existingType != null) {
			OntClass mt =  m.createClass(existingType);
			return mt;
		}
		
		// characteristic
		String characteristicUri = this.lookupConcept(characteristicLabel);
		if (characteristicUri == null) {
			return null;
		}

		// entity
		String entityUri = this.lookupConcept(entityLabel);
		if (entityUri == null) {
			return null;
		}
		
		// start the measurement type
		String partialUri = String.format("%8s", classId++).replace(' ', '0');  
		String uri = ecsoPrefix + partialUri;
		OntClass mt =  m.createClass(uri);
		mt.addProperty(rdfsLabel, measurementTypeLabel);
		mt.setSuperClass(measurementTypeClass);
		
		// add characteristic
		OntClass characteristic = this.ecsoModel.getOntClass(characteristicUri);
		SomeValuesFromRestriction characteristicRestriction = m.createSomeValuesFromRestriction(null, measuresCharacteristic, characteristic);
		mt.addSuperClass(characteristicRestriction);
		
		// add entity
		OntClass entity = this.ecsoModel.getOntClass(entityUri);
		SomeValuesFromRestriction entityRestriction = m.createSomeValuesFromRestriction(null, measuresEntity, entity);
		mt.addSuperClass(entityRestriction);
	
		return mt;
		
	}
	
	public OntClass generateEntity(String entityString) {
		
		// create the entity subclass
		String entityLabel = this.getFragment(entityString);
		String entityNamespace = this.getNamespace(entityString);

		String partialUri = String.format("%8s", classId++).replace(' ', '0');  
		String uri = entityNamespace + partialUri;
		OntClass entitySubclass =  m.createClass(uri);
		entitySubclass.addProperty(rdfsLabel, entityLabel);
		entitySubclass.setSuperClass(entityClass);
		
		return entitySubclass;
		
	}
	
	public String getModelAsString() {
		StringWriter sw = new StringWriter();
		m.write(sw, "RDF/XML");
		String result = sw.toString();
		
		return result;
	}
	
	private String getNamespace(String fullLabel) {
		String prefix = fullLabel.split(":")[0];
		return namespaces.get(prefix);
	}
	
	private String getFragment(String fullLabel) {
		String fragment = fullLabel.split(":")[1];
		return fragment;
	}
	
	public String lookupConcept(String fullLabel) {
		
		String concept = null;
		
		String prefix = this.getNamespace(fullLabel);
		String fragment = this.getFragment(fullLabel);
		
		// try finding the resource as if a uri
		String uri = prefix + fragment;
		Resource resource = ResourceFactory.createResource(uri);
		if (this.ecsoModel.containsResource(resource)) {
			return uri;
		}
		
		// maybe it is a label
		String queryString = "" +
                "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "select ?class where {\n" +
                "  ?class rdfs:label \"" + fragment + "\"\n" +
                "}";
        ResultSet results = QueryExecutionFactory.create(queryString, this.ecsoModel).execSelect();
        while (results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            concept = solution.get("class").toString();
            log.debug( "found matching concept: " + concept);
            return concept;
        }
		
		return concept;

	}
	
	public static void main(String[] args) {
		MeasurementTypeGenerator mtg = new MeasurementTypeGenerator();
		OntClass measurementType = mtg.generateMeasurementType("ecso:Tree", "oboe-characteristics:Count");
		OntClass entity = mtg.generateEntity("taxa:Cyperus");

		String rdf = mtg.getModelAsString();
		System.out.println(rdf);
		
	}

	public String generateTypes(String pidFile) throws Exception {
		// fetch the file
		URL url = new URL(pidFile);
    	InputStream inputStream = url.openStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		
		// parse it
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(reader);
		
		int count = 0;
		// iterate over the rows
		for (CSVRecord record : records) {
			// get the info from the csv file
		    String entityLabel = record.get("Entity");
		    String characteristicLabel = record.get("Characteristic");
		    String class_id_int = record.get("class_id_int");
		    
		    // skip records that already have a MeasurementType concept
		    if (class_id_int != null && class_id_int.length() > 0) {
		    	continue;
		    }
		    
		    // or that don't have enough information
		    if (entityLabel == null || entityLabel.length() == 0) {
		    	continue;
		    }
		    if (characteristicLabel == null || characteristicLabel.length() == 0) {
		    	continue;
		    }
		    
		    this.generateMeasurementType(entityLabel, characteristicLabel);
		    count++;
		}
		log.debug("Generated class count: " + count);
		
		String rdf = this.getModelAsString();
		System.out.println(rdf);
		return rdf;
		
	}
	
	
	
}
