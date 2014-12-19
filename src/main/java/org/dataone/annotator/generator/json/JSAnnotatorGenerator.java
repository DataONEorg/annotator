package org.dataone.annotator.generator.json;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.annotator.generator.AnnotationGenerator;
import org.dataone.annotator.matcher.ConceptItem;
import org.dataone.annotator.matcher.ConceptMatcher;
import org.dataone.annotator.matcher.ConceptMatcherFactory;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.DateTimeMarshaller;
import org.ecoinformatics.datamanager.parser.Attribute;
import org.ecoinformatics.datamanager.parser.DataPackage;
import org.ecoinformatics.datamanager.parser.Entity;
import org.ecoinformatics.datamanager.parser.Party;
import org.ecoinformatics.datamanager.parser.generic.DataPackageParserInterface;
import org.ecoinformatics.datamanager.parser.generic.Eml200DataPackageParser;

public class JSAnnotatorGenerator extends AnnotationGenerator {

	private static Log log = LogFactory.getLog(JSAnnotatorGenerator.class);
	
	private ConceptMatcher conceptMatcher;
	
	private ConceptMatcher orcidMatcher;


	/**
	 * Constructor initializes the
	 */
	public JSAnnotatorGenerator() {
		super();
		
		// initialize the concept matcher impl we will use
		conceptMatcher = ConceptMatcherFactory.getMatcher(ConceptMatcherFactory.BIOPORTAL);
		orcidMatcher = ConceptMatcherFactory.getMatcher(ConceptMatcherFactory.ORCID);

	}
	
    /**
     * Generate annotation for given metadata identifier
     * @param metadataPid
     * 
     *     {
      "reject": false, 
      "user": "uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org", 
      "quote": "T_AIR", 
      "links": [
        {
          "rel": "alternate", 
          "href": "http://annotateit.org/annotations/48hAIW6TQJyg4uW5Utq7iA", 
          "type": "text/html"
        }
      ], 
      "tags": [
        "http://ecoinformatics.org/oboe/oboe.1.0/oboe-characteristics.owl#Temperature"
      ], 
      "ranges": [
        {
          "endOffset": 5, 
          "startOffset": 0, 
          "end": "/section[1]/article[1]/div[1]/div[1]/form[1]/div[6]/div[1]/div[1]/div[6]/div[1]/div[2]/div[3]/div[1]/div[1]", 
          "start": "/section[1]/article[1]/div[1]/div[1]/form[1]/div[6]/div[1]/div[1]/div[6]/div[1]/div[2]/div[3]/div[1]/div[1]"
        }
      ], 
      "id": "48hAIW6TQJyg4uW5Utq7iA", 
      "text": "", 
      "oa:Motivation": "oa:tagging", 
      "pid": "tao.1.6", 
      "permissions": {
        "update": [], 
        "read": [
          "group:__world__"
        ], 
        "delete": [], 
        "admin": []
      }, 
      "uri": "http://localhost:8080/metacat/d1/mn/v1/object/tao.1.6", 
      "consumer": "f780f3e398cf45cbb4e84ed9ec91622a", 
      "field": "annotation_sm", 
      "updated": "2014-11-26T20:46:44.576047+00:00", 
      "created": "2014-11-18T05:12:08.331690+00:00", 
      "resource": "#xpointer(/eml/dataset/dataTable[1]/attributeList/attribute[3])"
    }
     */
	@Override
    public Map<Identifier, String> generateAnnotations(Identifier metadataPid) throws Exception {
    	
    	DataPackage dataPackage = this.getDataPackage(metadataPid);
    	
		Map<Identifier, String> annotations = new HashMap<Identifier, String>();
		
		// loop through the tables and attributes
		int entityCount = 1;
		Entity[] entities = dataPackage.getEntityList();
		if (entities != null) {
			for (Entity entity: entities) {
				String entityName = entity.getName();
				log.debug("Entity name: " + entityName);
				
				Attribute[] attributes = entity.getAttributeList().getAttributes();
				int attributeCount = 1;
				if (attributes != null) {
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
					
						StringBuffer attributeText = new StringBuffer();
						attributeText.append(attributeName);
						attributeText.append(" ");
						attributeText.append(attributeLabel);
						attributeText.append(" ");
						attributeText.append(attributeDefinition);
						attributeText.append(" ");
						attributeText.append(attributeType);
						attributeText.append(" ");
						attributeText.append(attributeScale);
						attributeText.append(" ");
						attributeText.append(attributeUnitType);
						attributeText.append(" ");
						attributeText.append(attributeUnit);
						attributeText.append(" ");
						attributeText.append(attributeDomain);
						
						// capture the annotation
				    	JSONObject annotation = createAnnotationTemplate(metadataPid);
				    	
						// for selecting particular part of the metadata
						String xpointer = "#xpointer(/eml/dataset/dataTable[" + entityCount + "]/attributeList/attribute[" + attributeCount + "])";
				    	
				    	annotation.put("field", "annotation_sm");
				    	annotation.put("resource", xpointer);
				    	annotation.put("quote", attributeName);
						
						// look up concepts for all the attribute text we have
						// TODO: refine this for better matching
						List<ConceptItem> concepts = conceptMatcher.getConcepts(attributeText.toString());
						
						if (concepts != null && concepts.size() > 0) {

							// add the tags
							// TODO: increase granularity to one tag per annotation?
							JSONArray tags = new JSONArray();
							for (ConceptItem conceptItem: concepts) {
								tags.add(conceptItem.getUri().toString());
							}
							annotation.put("tags", tags);
							
						}
						
						// write the annotation out
						StringWriter sw = new StringWriter();
				    	annotation.writeJSONString(sw);
						Identifier pid = new Identifier();
						pid.setValue(annotation.get("id").toString());
						annotations.put(pid, sw.toString());

				    	// on to the next attribute
						attributeCount++;
						
					}
				}
				entityCount++;
			}
		}
		
		// look up creators from the EML metadata for an attribution annotation
		List<Party> creators = dataPackage.getCreators();
		if (creators != null && creators.size() > 0) {	
			
			// use an orcid if we can find one from their system
			String creatorText = creators.get(0).getOrganization() + " " + creators.get(0).getSurName() + " " + creators.get(0).getGivenNames();
			List<ConceptItem> concepts = orcidMatcher.getConcepts(creatorText);
			if (concepts != null) {
				JSONObject annotation = createAnnotationTemplate(metadataPid);
				JSONArray creatorTags = new JSONArray();
				for (ConceptItem item: concepts) {
					String orcidUri = item.getUri().toString();
					creatorTags.add(orcidUri);
				}
				annotation.put("tags", creatorTags);
				
				String xpointer = "#xpointer(/eml/dataset/creator[" + 1 + "]/individual/surname)";
		    	annotation.put("field", "orcid_sm");
		    	annotation.put("resource", xpointer);
		    	annotation.put("quote", creators.get(0).getSurName());
		    	
		    	StringWriter sw = new StringWriter();
				annotation.writeJSONString(sw);
				Identifier pid = new Identifier();
				pid.setValue(annotation.get("id").toString());
				annotations.put(pid, sw.toString());
			} 
		}
		
		
		return annotations;
		
	}
	
	private JSONObject createAnnotationTemplate(Identifier metadataPid) throws Exception {
		
    	SystemMetadata sysMeta = D1Client.getCN().getSystemMetadata(null, metadataPid);

		// reusable fields for each annotation we generated
    	JSONObject annotation = new JSONObject();
    	
    	// each needs a unique ID
    	String uuid = UUID.randomUUID().toString();
		String annotationUri = "http://annotation/" + metadataPid.getValue() + uuid ;
		Identifier annotationPid = new Identifier();
		annotationPid.setValue(annotationUri);
		
    	annotation.put("id", annotationPid.getValue());
    	annotation.put("pid", metadataPid.getValue());
    	annotation.put("uri", Settings.getConfiguration().getProperty("D1Client.CN_URL") + "/v1/resolve/" + metadataPid.getValue());
    	annotation.put("consumer", Settings.getConfiguration().getProperty("annotator.consumerKey"));
    	
    	// TODO: transfer all permissions from sysmeta
    	JSONObject permissions = (JSONObject) JSONValue.parse(
    			"{" +
    				"\"update\": [" +
    					sysMeta.getRightsHolder().toString()
    				+ "], " +
    				"\"read\": [\"group:__world__\"], " +
    		        "\"delete\": [" +
    		        	sysMeta.getRightsHolder().toString()
    		        + "], " +
    		        "\"admin\": [" +
    		        	sysMeta.getRightsHolder().toString()
    		        + "]" +
    		     "}"
    			);
		annotation.put("permissions", permissions);
		String now = DateTimeMarshaller.serializeDateToUTC(Calendar.getInstance().getTime());
		annotation.put("created", now);
		annotation.put("updated", now);
		//TODO: who is considered the automated user?
		annotation.put("user", Settings.getConfiguration().getProperty("cn.nodeSubject"));
		
		return annotation;
	}
	
	
	private DataPackage getDataPackage(Identifier pid) throws Exception {
		// get package from Member
		InputStream emlStream = D1Client.getCN().get(null, pid);

		// parse the metadata
		DataPackageParserInterface parser = new Eml200DataPackageParser();
		parser.parse(emlStream);
		DataPackage dataPackage = parser.getDataPackage();
		return dataPackage;
	}
	
	public static void main(String[] args) throws Exception {

			testGenerate();
			System.exit(0);
	}
	
	public static void testGenerate() throws Exception {
		Identifier metadataPid = new Identifier();
		metadataPid.setValue("tao.1.1");
		JSAnnotatorGenerator ds = new JSAnnotatorGenerator();
		String jsonString = ds.generateAnnotations(metadataPid).values().iterator().next();
		log.info("JSON annotation: \n" + jsonString);
		
	}
	
	
}
