package org.dataone.annotator.ontology;

import java.io.StringWriter;
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
import com.hp.hpl.jena.ontology.SomeValuesFromRestriction;
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
	public static String ecsoPrefix = "http://purl.dataone.org/odo/ECSO_";
	
	private OntModel ecsoModel = null;
	private Map<String, String> namespaces = new HashMap<String, String>();
	private int ecsoId = 1000;

	public MeasurementTypeGenerator() {
		
		// prep the namespace prefixes
		namespaces.put("ecso", ecsoPrefix);
		namespaces.put("oboe", AnnotationGenerator.oboe);
		namespaces.put("oboe-core", AnnotationGenerator.oboe_core);
		namespaces.put("oboe-characteristics", AnnotationGenerator.oboe_characteristics);
		
		// retrieve the ECSO ontology
		ecsoModel = ModelFactory.createOntologyModel();
		ecsoModel.read(ecso);
		
	}

	public String generateMeasurementType(String entityLabel, String characteristicLabel) {
		
		String result = null;		
		
		AnnotationGenerator.initializeCache();

		// construct the ontology model for additions
		OntModel m = ModelFactory.createOntologyModel();
		
		Ontology ont = m.createOntology(ecso);
		ont.addImport(m.createResource(AnnotationGenerator.oboe));
		m.addSubModel(OntDocumentManager.getInstance().getModel(AnnotationGenerator.oboe));
		
		// properties
		Property rdfsLabel = ecsoModel.getProperty(AnnotationGenerator.rdfs + "label");
		
		ObjectProperty measuresCharacteristic = ecsoModel.getObjectProperty(AnnotationGenerator.oboe_core + "measuresCharacteristic");
		ObjectProperty measuresEntity = ecsoModel.getObjectProperty(AnnotationGenerator.oboe_core + "measuresEntity");

		// classes
		OntClass entityClass =  ecsoModel.getOntClass(AnnotationGenerator.oboe_core + "Entity");
		OntClass characteristicClass = ecsoModel.getOntClass(AnnotationGenerator.oboe_core + "Characteristic");
		OntClass measurementTypeClass =  ecsoModel.getOntClass(AnnotationGenerator.oboe_core + "MeasurementType");
		
		// create the measurement type from entity and characteristics given
		String measurementTypeLabel = this.getFragment(entityLabel) + " " + this.getFragment(characteristicLabel);

		String partialUri = String.format("%8s", ecsoId).replace(' ', '0');  
		String uri = ecsoPrefix + partialUri;
		OntClass mt =  m.createClass(uri);
		mt.addProperty(rdfsLabel, measurementTypeLabel);
		mt.setSuperClass(measurementTypeClass);
		
		// characteristic
		String characteristicUri = this.lookupConcept(characteristicLabel);
		OntClass characteristic = this.ecsoModel.getOntClass(characteristicUri);
		// TODO: ensure it is a characteristic subclass?
		SomeValuesFromRestriction characteristicRestriction = m.createSomeValuesFromRestriction(null, measuresCharacteristic, characteristic);
		mt.addSuperClass(characteristicRestriction);
		
		// entity
		String entityUri = this.lookupConcept(entityLabel);
		System.out.println("entityUri: " + entityUri);
		OntClass entity = this.ecsoModel.getOntClass(entityUri);
		System.out.println("entity: " + entity);
		System.out.println("measuresEntity: " + measuresEntity);

		// TODO: check that it is an entity subclass?
		SomeValuesFromRestriction entityRestriction = m.createSomeValuesFromRestriction(null, measuresEntity, entity);
		mt.addSuperClass(entityRestriction);

		// an intersection of entity+characteristic?
//		RDFList members = m.createList(new RDFNode[]{entityRestriction, characteristicRestriction});
//		IntersectionClass intersection = m.createIntersectionClass(null, members);
//		mt.addEquivalentClass(intersection);
		
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
            System.out.println( "found matching concept: " + concept);
            return concept;
        }
		
		return concept;

	}
	
	public static void main(String[] args) {
		MeasurementTypeGenerator mtg = new MeasurementTypeGenerator();
		String rdf = mtg.generateMeasurementType("ecso:Tree", "oboe-characteristics:Count");
		System.out.println(rdf);
		
	}
	
	
	
}
