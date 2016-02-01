package org.dataone.annotator.ontology;

import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.dataone.annotator.generator.AnnotationGenerator;

import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.IntersectionClass;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class MeasurementTypeGenerator {

	public static String ecso = "https://raw.githubusercontent.com/DataONEorg/sem-prov-ontologies/master/observation/d1-ECSO.owl";
	public static String ecsoPrefix = "https://purl.org/dataone/odo/ECSO_";
	
	private OntModel escoModel = null;
	private Map<String, String> namespaces = new HashMap<String, String>();
	private int ecsoId = 1000;

	public MeasurementTypeGenerator() {
		
		// prep the namespace prefixes
		namespaces.put("ecso", ecsoPrefix);
		namespaces.put("oboe", AnnotationGenerator.oboe);
		namespaces.put("oboe-core", AnnotationGenerator.oboe_core);
		namespaces.put("oboe-characteristics", AnnotationGenerator.oboe_characteristics);
		
		// retrieve the ECSO ontology
		escoModel = ModelFactory.createOntologyModel();
		escoModel.read(ecso);
		
	}

	public String generateMeasurementType(String entityLabel, String characteristicLabel) {
		
		String result = null;		
		
		AnnotationGenerator.initializeCache();

		// construct the ontology
		OntModel m = ModelFactory.createOntologyModel();
		Ontology ont = m.createOntology(ecso);
		//m.addSubModel(OntDocumentManager.getInstance().getModel(ecso));
		
		ont.addImport(m.createResource(AnnotationGenerator.oboe));
		m.addSubModel(OntDocumentManager.getInstance().getModel(AnnotationGenerator.oboe));
		
		// properties
		Property rdfValue = m.getProperty(AnnotationGenerator.rdf + "value");
		Property rdfsLabel = m.getProperty(AnnotationGenerator.rdfs + "label");
		
		ObjectProperty measuresCharacteristic = m.getObjectProperty(AnnotationGenerator.oboe_core + "measuresCharacteristic");
		ObjectProperty usesStandard = m.getObjectProperty(AnnotationGenerator.oboe_core + "usesStandard");
		ObjectProperty measuresEntity = m.getObjectProperty(AnnotationGenerator.oboe_core + "measuresEntity");
		ObjectProperty hasMeasurement = m.getObjectProperty(AnnotationGenerator.oboe_core + "hasMeasurement");

		// classes
		OntClass entityClass =  m.getOntClass(AnnotationGenerator.oboe_core + "Entity");
		OntClass observationClass =  m.getOntClass(AnnotationGenerator.oboe_core + "Observation");
		OntClass measurementClass =  m.getOntClass(AnnotationGenerator.oboe_core + "Measurement");
		OntClass measurementTypeClass =  m.getOntClass(AnnotationGenerator.oboe_core + "MeasurementType");
		OntClass characteristicClass = m.getOntClass(AnnotationGenerator.oboe_core + "Characteristic");
		OntClass standardClass =  m.getOntClass(AnnotationGenerator.oboe_core + "Standard");
		
		
		// create the measurement type from entity and characteristics given
		String measurementTypeLabel = entityLabel + " " + characteristicLabel;

		String partialUri = String.format("%8s", ecsoId).replace(' ', '0');  
		String uri = ecso + partialUri;
		OntClass mt =  m.createClass(uri);
		mt.addProperty(rdfsLabel, measurementTypeLabel);
		mt.setSuperClass(measurementTypeClass);
		
		// characteristic
		OntClass characteristic = m.getOntClass(AnnotationGenerator.oboe_core + "Characteristic");
		//characteristic.setSuperClass(characteristicClass);
		AllValuesFromRestriction characteristicRestriction = m.createAllValuesFromRestriction(null, measuresCharacteristic, characteristic);
		//mt.addEquivalentClass(characteristicRestriction);
		
		// entity
		OntClass entity = m.getOntClass(AnnotationGenerator.oboe_core + "Entity");
		//entity.setSuperClass(entityClass);
		AllValuesFromRestriction entityRestriction = m.createAllValuesFromRestriction(null, measuresEntity, entity);
		//mt.addEquivalentClass(entityRestriction);

		RDFList members = m.createList(new RDFNode[]{entityRestriction, characteristicRestriction});
		IntersectionClass intersection = m.createIntersectionClass(null, members);
		mt.addEquivalentClass(intersection);
		
		StringWriter sw = new StringWriter();
		m.write(sw, "RDF/XML");
		result = sw.toString();
		
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
		if (this.escoModel.containsResource(resource)) {
			return uri;
		}
		
		// maybe it is a label
		String queryString = "" +
                "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "select ?class where {\n" +
                "  ?class rdfs:label \"" + fragment + "\"\n" +
                "}";
        ResultSet results = QueryExecutionFactory.create(queryString, this.escoModel).execSelect();
        while (results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            concept = solution.get("class").toString();
            System.out.println( "found matching concept: " + concept);
            return concept;
        }
		
		return concept;

	}
	
	public static void main(String[] args) {
		MeasurementTypeGenerator mtg = new MeasurementTypeGenerator();
		String rdf = mtg.generateMeasurementType("tree", "height");
		System.out.println(rdf);
		
	}
	
	
	
}