package org.dataone.annotator.generator;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.annotator.matcher.ConceptMatcher;
import org.dataone.annotator.matcher.ConceptMatcherFactory;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.types.v1.Identifier;
import org.ecoinformatics.datamanager.parser.Attribute;
import org.ecoinformatics.datamanager.parser.DataPackage;
import org.ecoinformatics.datamanager.parser.Entity;
import org.ecoinformatics.datamanager.parser.Party;
import org.ecoinformatics.datamanager.parser.generic.DataPackageParserInterface;
import org.ecoinformatics.datamanager.parser.generic.Eml200DataPackageParser;

import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class AnnotationGenerator {

	private static Log log = LogFactory.getLog(AnnotationGenerator.class);
	
	public static String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
	public static String owl = "http://www.w3.org/2002/07/owl#";
	public static String oboe = "http://ecoinformatics.org/oboe/oboe.1.0/oboe.owl#";
	public static String oboe_core = "http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#";
	public static String oboe_characteristics = "http://ecoinformatics.org/oboe/oboe.1.0/oboe-characteristics.owl#";
	public static String oboe_sbc = "http://ecoinformatics.org/oboe-ext/sbclter.1.0/oboe-sbclter.owl#";
	public static String oa = "http://www.w3.org/ns/oa#";
	public static String oa_source = "http://www.w3.org/ns/oa.rdf";
	public static String dcterms = "http://purl.org/dc/terms/";
	public static String dcterms_source = "http://dublincore.org/2012/06/14/dcterms.rdf";
	public static String foaf = "http://xmlns.com/foaf/0.1/";
	public static String foaf_source = "http://xmlns.com/foaf/spec/index.rdf";
    public static String prov = "http://www.w3.org/ns/prov#";
    public static String prov_source = "http://www.w3.org/ns/prov.owl";
    public static String cito =  "http://purl.org/spar/cito/";
	public static String OBOE_SBC = "OBOE-SBC";
	
	private static boolean cacheInitialized;
	
	private ConceptMatcher conceptMatcher;
	
	private ConceptMatcher orcidMatcher;


	/**
	 * Constructor initializes the
	 */
	public AnnotationGenerator() {
		
		// initialize the concept matcher impl we will use
		conceptMatcher = ConceptMatcherFactory.getMatcher(ConceptMatcherFactory.BIOPORTAL);
		orcidMatcher = ConceptMatcherFactory.getMatcher(ConceptMatcherFactory.ORCID);

		// import the ontologies we use - shared among all instances
		initializeCache();

	}
	
	private static void initializeCache() {
		if (!cacheInitialized) {
			// cache the ontologies we use
			OntDocumentManager.getInstance().addModel(oboe, ModelFactory.createOntologyModel().read(oboe));
			OntDocumentManager.getInstance().addModel(oboe_sbc, ModelFactory.createOntologyModel().read(oboe_sbc));
			OntDocumentManager.getInstance().addModel(oa, ModelFactory.createOntologyModel().read(oa_source));
			OntDocumentManager.getInstance().addModel(dcterms, ModelFactory.createOntologyModel().read(dcterms_source));
			OntDocumentManager.getInstance().addModel(foaf, ModelFactory.createOntologyModel().read(foaf_source));
			OntDocumentManager.getInstance().addModel(prov, ModelFactory.createOntologyModel().read(prov));
			OntDocumentManager.getInstance().addModel(cito, ModelFactory.createOntologyModel().read(cito));
			cacheInitialized = true;
		}
	}

    /**
     * Generate annotation for given metadata identifier
     * @param metadataPid
     */
    public String generateAnnotation(Identifier metadataPid) throws Exception {
    	
    	DataPackage dataPackage = this.getDataPackage(metadataPid);
    	
		OntModel m = ModelFactory.createOntologyModel();
		Ontology ont = m.createOntology("http://annotation/" + metadataPid.getValue());
		
		ont.addImport(m.createResource(oboe));
		m.addSubModel(OntDocumentManager.getInstance().getModel(oboe));
		
		ont.addImport(m.createResource(oboe_sbc));
		m.addSubModel(OntDocumentManager.getInstance().getModel(oboe_sbc));
		
		ont.addImport(m.createResource(oa));
		m.addSubModel(OntDocumentManager.getInstance().getModel(oa));

		ont.addImport(m.createResource(dcterms));
		m.addSubModel(OntDocumentManager.getInstance().getModel(dcterms));

		ont.addImport(m.createResource(foaf));
		m.addSubModel(OntDocumentManager.getInstance().getModel(foaf));
		
		ont.addImport(m.createResource(prov));
		//m.addSubModel(ModelFactory.createOntologyModel().read(prov_source));

		ont.addImport(m.createResource(cito));
		
		// properties
		ObjectProperty hasBodyProperty = m.getObjectProperty(oa + "hasBody");
		ObjectProperty hasTargetProperty = m.getObjectProperty(oa + "hasTarget");
		ObjectProperty hasSourceProperty = m.getObjectProperty(oa + "hasSource");
		ObjectProperty hasSelectorProperty = m.getObjectProperty(oa + "hasSelector");
		ObjectProperty annotatedByProperty = m.getObjectProperty(oa + "annotatedBy");
		Property identifierProperty = m.getProperty(dcterms + "identifier");
		Property conformsToProperty = m.getProperty(dcterms + "conformsTo");
		Property wasAttributedTo = m.getProperty(prov + "wasAttributedTo");
		Property nameProperty = m.getProperty(foaf + "name");
		Property rdfValue = m.getProperty(rdf + "value");
		
		ObjectProperty ofCharacteristic = m.getObjectProperty(oboe_core + "ofCharacteristic");
		ObjectProperty usesStandard = m.getObjectProperty(oboe_core + "usesStandard");
		ObjectProperty ofEntity = m.getObjectProperty(oboe_core + "ofEntity");
		ObjectProperty hasMeasurement = m.getObjectProperty(oboe_core + "hasMeasurement");

		// classes
		OntClass entityClass =  m.getOntClass(oboe_core + "Entity");
		OntClass observationClass =  m.getOntClass(oboe_core + "Observation");
		OntClass measurementClass =  m.getOntClass(oboe_core + "Measurement");
		OntClass characteristicClass = m.getOntClass(oboe_core + "Characteristic");
		OntClass standardClass =  m.getOntClass(oboe_core + "Standard");
		
		Resource annotationClass =  m.getOntClass(oa + "Annotation");
		Resource specificResourceClass =  m.getOntClass(oa + "SpecificResource");
		Resource fragmentSelectorClass =  m.getOntClass(oa + "FragmentSelector");
		Resource provEntityClass =  m.getResource(prov + "Entity");
		Resource personClass =  m.getResource(prov + "Person");
				
		// these apply to every attribute annotation
		Individual meta1 = m.createIndividual(ont.getURI() + "#meta", provEntityClass);
		meta1.addProperty(identifierProperty, metadataPid.getValue());

		// decide who should be credited with the package
		Individual p1 = null;
		
		// look up creators from the EML metadata
		List<Party> creators = dataPackage.getCreators();
		//creators = Arrays.asList("Matthew Jones");
		if (creators != null && creators.size() > 0) {	
			// use an orcid if we can find one from their system
			String creatorText = creators.get(0).getOrganization() + " " + creators.get(0).getSurName() + " " + creators.get(0).getGivenNames();
			List<String> orcidUris = orcidMatcher.getConcepts(creatorText);
			if (orcidUris != null) {
				String orcidUri = orcidUris.get(0);
				p1 = m.createIndividual(orcidUri, personClass);
				p1.addProperty(identifierProperty, orcidUri);
			} else {
				p1 = m.createIndividual(ont.getURI() + "#person", personClass);
			}
			// include the name we have in the metadata
			if (creators.get(0).getSurName() != null) {
				p1.addProperty(nameProperty, creators.get(0).getSurName());
			} else if (creators.get(0).getOrganization() != null) {
				p1.addProperty(nameProperty, creators.get(0).getOrganization());
			}
		}
		
		// attribute the package to this creator if we have one
		if (p1 != null) {
			meta1.addProperty(wasAttributedTo, p1);
		}
		
		// loop through the tables and attributes
		int entityCount = 1;
		Entity[] entities = dataPackage.getEntityList();
		if (entities != null) {
			for (Entity entity: entities) {
				String entityName = entity.getName();
				
				Individual o1 = m.createIndividual(ont.getURI() + "#observation" + entityCount, observationClass);
				Resource entityConcept = lookupEntity(entityClass, entity);
				if (entityConcept != null) {
					AllValuesFromRestriction avfr = m.createAllValuesFromRestriction(null, ofEntity, entityConcept);
					o1.addOntClass(avfr);
				}
				
				log.debug("Entity name: " + entityName);
				Attribute[] attributes = entity.getAttributeList().getAttributes();
				int attributeCount = 1;
				if (attributes != null) {
					for (Attribute attribute: attributes) {
						
						// for naming the individuals uniquely
						String cnt = entityCount + "_" + attributeCount;
						
						String attributeName = attribute.getName();
						String attributeLabel = attribute.getLabel();
						String attributeDefinition = attribute.getDefinition();
						String attributeType = attribute.getAttributeType();
						String attributeScale = attribute.getMeasurementScale();
						String attributeUnitType = attribute.getUnitType();
						String attributeUnit = attribute.getUnit();
						String attributeDomain = attribute.getDomain().getClass().getSimpleName();
		
						log.debug("Attribute name: " + attributeName);
						log.debug("Attribute label: " + attributeLabel);
						log.debug("Attribute definition: " + attributeDefinition);
						log.debug("Attribute type: " + attributeType);
						log.debug("Attribute scale: " + attributeScale);
						log.debug("Attribute unit type: " + attributeUnitType);
						log.debug("Attribute unit: " + attributeUnit);
						log.debug("Attribute domain: " + attributeDomain);
					
						// look up the characteristic or standard subclasses
						Resource standard = this.lookupStandard(standardClass, attribute);
						Resource characteristic = this.lookupCharacteristic(characteristicClass, attribute);
						
						if (standard != null || characteristic != null) {
							
							// instances
							Individual m1 = m.createIndividual(ont.getURI() + "#measurement" + cnt, measurementClass);
							Individual a1 = m.createIndividual(ont.getURI() + "#annotation" + cnt, annotationClass);
							Individual t1 = m.createIndividual(ont.getURI() + "#target" + cnt, specificResourceClass);
							String xpointer = "xpointer(/eml/dataSet/dataTable[" + entityCount + "]/attributeList/attribute[" + attributeCount + "])";
							Individual s1 = m.createIndividual(ont.getURI() + "#" + xpointer, fragmentSelectorClass);
							s1.addLiteral(rdfValue, xpointer);
							s1.addProperty(conformsToProperty, "http://tools.ietf.org/rfc/rfc3023");
							//s1.addProperty(conformsToProperty, "http://www.w3.org/TR/xptr/");

							
							// statements about the annotation
							a1.addProperty(hasBodyProperty, m1);
							a1.addProperty(hasTargetProperty, t1);
							t1.addProperty(hasSourceProperty, meta1);
							t1.addProperty(hasSelectorProperty, s1);
							//a1.addProperty(annotatedByProperty, p1);
							
							// describe the measurement in terms of restrictions
							if (standard != null) {
								AllValuesFromRestriction avfr = m.createAllValuesFromRestriction(null, usesStandard, standard);
								m1.addOntClass(avfr);
							}
							if (characteristic != null) {
								AllValuesFromRestriction avfr = m.createAllValuesFromRestriction(null, ofCharacteristic, characteristic);
								m1.addOntClass(avfr);
							}
							
							// attach to the observation
							// TODO: evaluate whether the measurement can apply to the given observed entity
							o1.addProperty(hasMeasurement, m1);
						}
						attributeCount++;
						
					}
				}
				entityCount++;
			}
		}
		
		StringWriter sw = new StringWriter();
		// only write the base model
		//m.write(sw, "RDF/XML-ABBREV");
		m.write(sw, null);

		return sw.toString();
		
	}
	
	private Resource lookupStandard(OntClass standardClass, Attribute attribute) {
		// what's our unit?
		String unit = attribute.getUnit().toLowerCase();
		
		// look up the concept using the matcher
		List<String> concepts = conceptMatcher.getConcepts(unit);
		return ResourceFactory.createResource(concepts.get(0));
		//return BioPortalService.lookupAnnotationClass(standardClass, unit, OBOE_SBC);
	}
	
	private Resource lookupCharacteristic(OntClass characteristicClass, Attribute attribute) {
		// what are we looking for?
		String label = attribute.getLabel().toLowerCase();
		String definition = attribute.getDefinition();
		String text = label + " " + definition;
		
		// look up the concept using the matcher
		List<String> concepts = conceptMatcher.getConcepts(text);
		return ResourceFactory.createResource(concepts.get(0));
		//return BioPortalService.lookupAnnotationClass(characteristicClass, text, OBOE_SBC);
		
	}
	
	private Resource lookupEntity(OntClass entityClass, Entity entity) {
		// what's our description like?
		String name = entity.getName();
		String definition = entity.getDefinition();
		
		// look up the concept using the matcher
		List<String> concepts = conceptMatcher.getConcepts(definition);
		return ResourceFactory.createResource(concepts.get(0));
		//return BioPortalService.lookupAnnotationClass(entityClass, definition, OBOE_SBC);
		
	}
	
	private DataPackage getDataPackage(Identifier pid) throws Exception {
		// get package from Member
		CNode cnode = D1Client.getCN();
		InputStream emlStream = cnode.get(null, pid);

		// parse the metadata
		DataPackageParserInterface parser = new Eml200DataPackageParser();
		parser.parse(emlStream);
		DataPackage dataPackage = parser.getDataPackage();
		return dataPackage;
	}

	private void summarize(List<Identifier> identifiers) {

		for (Identifier pid: identifiers) {
		
			log.debug("Parsing pid: " + pid.getValue());
			
			try {
				
				// get the package
				DataPackage dataPackage = this.getDataPackage(pid);
				String title = dataPackage.getTitle();
				log.debug("Title: " + title);
				
				Entity[] entities = dataPackage.getEntityList();
				if (entities != null) {
					for (Entity entity: entities) {
						String entityName = entity.getName();
						log.debug("Entity name: " + entityName);
						Attribute[] attributes = entity.getAttributeList().getAttributes();
						for (Attribute attribute: attributes) {
							String attributeName = attribute.getName();
							String attributeLabel = attribute.getLabel();
							String attributeDefinition = attribute.getDefinition();
							String attributeType = attribute.getAttributeType();
							String attributeScale = attribute.getMeasurementScale();
							String attributeUnitType = attribute.getUnitType();
							String attributeUnit = attribute.getUnit();
							String attributeDomain = attribute.getDomain().getClass().getSimpleName();

							log.debug("Attribute name: " + attributeName);
							log.debug("Attribute label: " + attributeLabel);
							log.debug("Attribute definition: " + attributeDefinition);
							log.debug("Attribute type: " + attributeType);
							log.debug("Attribute scale: " + attributeScale);
							log.debug("Attribute unit type: " + attributeUnitType);
							log.debug("Attribute unit: " + attributeUnit);
							log.debug("Attribute domain: " + attributeDomain);
							
						}		
					}
				}
				
			} catch (Exception e) {
				log.warn("error parsing metadata for: " + pid.getValue(), e);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {

			testGenerate();
//			testSummary();
			System.exit(0);
	}
	
	public static void testGenerate() throws Exception {
		Identifier metadataPid = new Identifier();
		metadataPid.setValue("tao.1.4");
		AnnotationGenerator ds = new AnnotationGenerator();
		String rdfString = ds.generateAnnotation(metadataPid);
		log.info("RDF annotation: \n" + rdfString);
		
	}
	
	public static void testSummary() throws Exception {
		
		// summarize the packages
		AnnotationGenerator ds = new AnnotationGenerator();
		List<Identifier> identifiers = new ArrayList<Identifier>();

		ds.summarize(identifiers);
		System.exit(0);
	}
	
}
