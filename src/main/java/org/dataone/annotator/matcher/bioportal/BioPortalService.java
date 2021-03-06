package org.dataone.annotator.matcher.bioportal;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.dataone.annotator.matcher.ConceptItem;
import org.dataone.annotator.matcher.ConceptMatcher;
import org.dataone.annotator.ontology.MeasurementTypeGenerator;
import org.dataone.configuration.Settings;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class BioPortalService implements ConceptMatcher {
	
	private static Log log = LogFactory.getLog(BioPortalService.class);
	
    // for looking up concepts in BioPortal
    private String restUrl = null;
    private String apiKey = null;
    private String ontologies = null;
    private MeasurementTypeGenerator mtg = null;

    public BioPortalService() {
    	restUrl = Settings.getConfiguration().getString("annotator.matcher.bioportal.restUrl", "http://data.bioontology.org");
    	apiKey = Settings.getConfiguration().getString("annotator.matcher.bioportal.apiKey", "24e4775e-54e0-11e0-9d7b-005056aa3316");
    	ontologies = Settings.getConfiguration().getString("annotator.matcher.bioportal.ontologies", "ECSO,PROV-ONE,DATA-CITE,DC-TERMS,OWL-TIME");
    	mtg = new MeasurementTypeGenerator();
    }
    

    @Override
    public List<ConceptItem> getConcepts(String text, String unit, String context) throws Exception {
    	
    	// limit suggested annotations to MeasurementType subclasses.
    	//OntClass measurementTypeClass = mtg.getMeasurementTypeClass();
    	//measurementTypeClass = null;

    	List<ConceptItem> concepts = lookupAnnotationClasses(text, ontologies);    	
    	return concepts;
    	
    }

	public List<ConceptItem> getConcepts(Map<String, String> queryItems) throws Exception {
		StringBuffer sb = new StringBuffer();
		for (String value: queryItems.values()) {
			sb.append(value);
			sb.append(" ");
		}
		return getConcepts(sb.toString(), null, null);
	}
    
    /**
	 * Look up possible concept from BioPortal annotation service.
	 * @see "http://data.bioontology.org/documentation"
	 * @param superClass
	 * @param text
	 * @return
	 */
	private List<ConceptItem> lookupAnnotationClasses(String text, String ontologies) {
		
		// no point calling the service
		if (text == null || text.length() == 0) {
			return null;
		}
		
		List<ConceptItem> results = new ArrayList<ConceptItem>();
		
		try {
			
			String urlParameters = "apikey=" + apiKey;
			urlParameters += "&format=xml&include=prefLabel,definition";
			if (ontologies != null) {
				urlParameters += "&ontologies=" + ontologies;
			}
			urlParameters += "&text=" + URLEncoder.encode(text, "UTF-8");
			
			String url = restUrl + "/annotator?" + urlParameters ;
			URL restURL = new URL(url);
			InputStream is = restURL.openStream();
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList classNodeList = XPathAPI.selectNodeList(doc, "//annotation/annotatedClass");

			//NodeList classNodeList = XMLUtilities.getNodeListWithXPath(doc, "//annotation/annotatedClass/id");
			if (classNodeList != null && classNodeList.getLength() > 0) {
				log.info("annotator suggested concept count: " + classNodeList.getLength());

				for (int i = 0; i < classNodeList.getLength(); i++) {
					Node annotatedClassNode = classNodeList.item(i);
					
					String classURI = XPathAPI.selectSingleNode(annotatedClassNode, "id").getFirstChild().getNodeValue();
					log.info("annotator suggested: " + classURI);

					//double rank = i--/classNodeList.getLength();
					
					// for returning the structure
					ConceptItem concept = new ConceptItem();
					concept.setUri(new URI(classURI));
					//concept.setWeight(rank);
					
					// check for label and definition
					Node labelNode = XPathAPI.selectSingleNode(annotatedClassNode, "prefLabel");
					if (labelNode != null) {
						concept.setLabel(labelNode.getFirstChild().getNodeValue());
					}
					Node defNode = XPathAPI.selectSingleNode(annotatedClassNode, "definitionCollection/definition");
					if (defNode != null) {
						concept.setDefinition(defNode.getFirstChild().getNodeValue());
					}
					
					// is this a subclass we want?
					boolean isSubclass = mtg.isMeasurementTypeSubclass(classURI);
					
					// now we can add this class
					if (isSubclass) {
						results.add(concept);
					}
					
				}
				
			}
		} catch (Exception e) {
			log.error("Could not lookup BioPortal annotation for text=" + text, e);
		}
		
		return results;
	}

}
