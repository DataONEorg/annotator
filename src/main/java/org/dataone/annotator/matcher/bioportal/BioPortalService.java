package org.dataone.annotator.matcher.bioportal;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.dataone.annotator.matcher.ConceptMatcher;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class BioPortalService implements ConceptMatcher {
	
	private static Log logMetacat = LogFactory.getLog(BioPortalService.class);
	
    // for looking up concepts in BioPortal
    private static final String REST_URL = "http://data.bioontology.org";
    private static final String API_KEY = "24e4775e-54e0-11e0-9d7b-005056aa3316";

    @Override
    public List<String> getConcepts(String text) {
    	List <String> concepts = new ArrayList<String>();
    	List<Resource> resources = lookupAnnotationClasses(null, text, null);
    	for (Resource resource: resources) {
    		concepts.add(resource.getURI());
    	}
    	return concepts;
    	
    }
    
    /**
	 * Look up possible concept from BioPortal annotation service.
	 * @see "http://data.bioontology.org/documentation"
	 * @param superClass
	 * @param text
	 * @return
	 */
	private static List<Resource> lookupAnnotationClasses(OntClass superClass, String text, String ontologies) {
		
		// no point calling the service
		if (text == null || text.length() == 0) {
			return null;
		}
		
		List<Resource> results = new ArrayList<Resource>();
		
		try {
			
			String urlParameters = "apikey=" + API_KEY;
			urlParameters += "&format=xml";
			if (ontologies != null) {
				urlParameters += "&ontologies=" + ontologies;
			}
			urlParameters += "&text=" + URLEncoder.encode(text, "UTF-8");
			
			String url = REST_URL + "/annotator?" + urlParameters ;
			URL restURL = new URL(url);
			InputStream is = restURL.openStream();
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList classNodeList = XPathAPI.selectNodeList(doc, "//annotation/annotatedClass/id");
			//NodeList classNodeList = XMLUtilities.getNodeListWithXPath(doc, "//annotation/annotatedClass/id");
			if (classNodeList != null && classNodeList.getLength() > 0) {
				for (int i = 0; i < classNodeList.getLength(); i++) {
					String classURI = classNodeList.item(i).getFirstChild().getNodeValue();
					logMetacat.info("annotator suggested: " + classURI);

					
					if (superClass == null) {
						// just add the suggestion to the list
						results.add(ResourceFactory.createResource(classURI));
					} else {
						// check that it is a subclass of superClass
						Resource subclass = superClass.getModel().getResource(classURI);
						boolean isSubclass = false;
						try {
							isSubclass = superClass.hasSubClass(subclass);
						} catch (ConversionException ce) {
							logMetacat.warn("Skipping unknown subclass: " + classURI, ce);
							// try the next one
							continue;
						}
						// now we can add this class
						if (isSubclass) {
							results.add(subclass);
						}
					}
					
				}
				
			}
		} catch (Exception e) {
			logMetacat.error("Could not lookup BioPortal annotation for text=" + text, e);
		}
		
		return results;
	}
	
	public static Resource lookupAnnotationClass(OntClass superClass, String text, String ontologies) {
		List<Resource> results = lookupAnnotationClasses(superClass, text, ontologies);
		if (results != null && results.size() > 0) {
			return results.get(0);
		}
		return null;
	}

}
