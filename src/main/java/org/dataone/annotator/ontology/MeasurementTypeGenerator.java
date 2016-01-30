package org.dataone.annotator.ontology;

import java.io.StringWriter;

import org.dataone.annotator.generator.AnnotationGenerator;

import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.IntersectionClass;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class MeasurementTypeGenerator {

	public static String ecso = "https://purl.org/dataone/odo/ecso.owl";
	public static String ecsoPrefix = "https://purl.org/dataone/odo/ECSO_";
	public static int ecsoId = 1000;


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

		OntClass mt =  m.createClass(ecsoPrefix + ecsoId);
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
	
	public static void main(String[] args) {
		MeasurementTypeGenerator mtg = new MeasurementTypeGenerator();
		String rdf = mtg.generateMeasurementType("tree", "height");
		System.out.println(rdf);
		
	}
	
	
	
}
