package org.dataone.annotator.generator;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Identifier;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public abstract class AnnotationGenerator {
	
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
	
	public AnnotationGenerator() {
		// import the ontologies we use - shared among all instances
		initializeCache();
	}
	
	protected static void initializeCache() {
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
	
	public Map<Identifier, String> generateAnnotations(Identifier id) throws Exception {
		return null;
	}

}
