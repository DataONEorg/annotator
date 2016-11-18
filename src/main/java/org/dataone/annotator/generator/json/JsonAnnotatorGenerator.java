package org.dataone.annotator.generator.json;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.annotator.generator.AnnotationGenerator;
import org.dataone.annotator.matcher.ConceptItem;
import org.dataone.annotator.matcher.ConceptMatcher;
import org.dataone.annotator.matcher.ConceptMatcherFactory;
import org.dataone.annotator.matcher.bioportal.BioPortalService;
import org.dataone.annotator.matcher.esor.CosineService;
import org.dataone.annotator.matcher.esor.EsorService;
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

public class JsonAnnotatorGenerator extends AnnotationGenerator {

	private static Log log = LogFactory.getLog(JsonAnnotatorGenerator.class);
	
	private ConceptMatcher conceptMatcher;
		
	private ConceptMatcher orcidMatcher;


	/**
	 * Constructor initializes the
	 */
	public JsonAnnotatorGenerator() throws Exception {
		super();
		
		// initialize the concept matcher implementations we will use
		String matcherClassName = Settings.getConfiguration().getString("annotator.matcher.className");
		conceptMatcher = ConceptMatcherFactory.getMatcher(matcherClassName);
		//orcidMatcher = ConceptMatcherFactory.getMatcher(ConceptMatcherFactory.ORCID);

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
		if (conceptMatcher instanceof CosineService) {
			return generateAnnotationsCosine(metadataPid);
		}
		return generateAnnotationsFromEML(metadataPid);
		//return generateAnnotationsFromIndex(metadataPid);
	}
	
	/**
	 * Generates annotations using EML Datamanager library for parsing
	 * @param metadataPid
	 * @return
	 * @throws Exception
	 */
    private Map<Identifier, String> generateAnnotationsFromEML(Identifier metadataPid) throws Exception {
    	
    	DataPackage dataPackage = this.getDataPackage(metadataPid);
    	SystemMetadata sysMeta = D1Client.getCN().getSystemMetadata(null, metadataPid);
    	
    	// TODO: use abstract content for context
    	String context = dataPackage.getAbstract();
    	
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
						attributeText.append(attributeUnit);			    	
						
						// just log for MOB's template
						/*********/
						boolean printTemplate = false;
						if (printTemplate) {
							log.info(
									"Manual annotation template"
									+ "\t" + metadataPid.getValue()
									+ "\t" + entityCount
									+ "\t" + attributeCount
									+ "\t" + attributeText
									+ "\t" + attributeName
									+ "\t" + conceptMatcher.getClass().getName()
									);
				    	
							// skip to next
							attributeCount++;
							continue;
						}
						/*********/

						
						// look up concepts for all the attribute text we have
						List<ConceptItem> concepts = conceptMatcher.getConcepts(attributeText.toString(), attributeUnit, context);
						if (concepts != null && concepts.size() > 0) {
							
							// debug
							for (ConceptItem conceptItem: concepts) {
								// tab delimited for easy analysis afterward
								log.info(
										"Suggested annotation summary"
										+ "\t" + metadataPid.getValue()
										+ "\t" + entityCount
										+ "\t" + attributeCount
										+ "\t" + attributeText
										+ "\t" + conceptItem.getUri().toString()
										+ "\t" + conceptItem.getLabel()
										+ "\t" + conceptItem.getDefinition()
										+ "\t" + conceptMatcher.getClass().getName()
										);
							}
						
							
							// construct the annotation with this information
							JSONObject annotation = 
									constructAttributeAnnotation(
											sysMeta, 
											entityCount, 
											attributeCount, 
											attributeName, 
											concepts);
								
							// write the annotation out
							StringWriter sw = new StringWriter();
					    	annotation.writeJSONString(sw);
							Identifier pid = new Identifier();
							pid.setValue(annotation.get("id").toString());
							annotations.put(pid, sw.toString());
						
						}

				    	// on to the next attribute
						attributeCount++;
						
					}
				}
				entityCount++;
			}
		}
		
		if (orcidMatcher != null) {
			// look up creators from the EML metadata for an attribution annotation
			List<Party> creators = dataPackage.getCreators();
			if (creators != null && creators.size() > 0) {	
				
				// use an orcid if we can find one from their system
				String creatorText = creators.get(0).getOrganization() + " " + creators.get(0).getSurName() + " " + creators.get(0).getGivenNames();
				List<ConceptItem> concepts = orcidMatcher.getConcepts(creatorText, null, null);
				if (concepts != null) {
					JSONObject annotation = createAnnotationTemplate(sysMeta);
					JSONArray creatorTags = new JSONArray();
					for (ConceptItem item: concepts) {
						String orcidUri = item.getUri().toString();
						creatorTags.add(orcidUri);
						// TODO: more than one match?
						break;
					}
					annotation.put("tags", creatorTags);
					
					String xpointer = "#xpointer(/eml/dataset/creator[" + 1 + "]/individual/surname)";
			    	annotation.put("field", "orcid_sm");
			    	annotation.put("resource", xpointer);
			    	annotation.put("quote", creators.get(0).getSurName());
			    	annotation.put("oa:Motivation", "prov:wasAttributedTo");
			    	
			    	// the range for the highlighted text
			    	JSONObject range = new JSONObject();
			    	range.put("start", "//*[@id='creator_" + 1 + "']/div[1]");
			    	range.put("end", "//*[@id='creator_" + 1 + "']/div[1]");
			    	range.put("startOffset", 0);
			    	range.put("endOffset", creators.get(0).getSurName().length());
			    	JSONArray ranges = new JSONArray();
			    	ranges.add(range);
					annotation.put("ranges", ranges);
			    	
			    	StringWriter sw = new StringWriter();
					annotation.writeJSONString(sw);
					Identifier pid = new Identifier();
					pid.setValue(annotation.get("id").toString());
					annotations.put(pid, sw.toString());
				} 
			}	
		}
		
		
		return annotations;
		
	}
    
    /**
	 * Generates annotations using cosine endpoint
	 * @param metadataPid
	 * @return
	 * @throws Exception
	 */
    private Map<Identifier, String> generateAnnotationsCosine(Identifier metadataPid) throws Exception {
    	
    	SystemMetadata sysMeta = D1Client.getCN().getSystemMetadata(null, metadataPid);
    	    	
		Map<Identifier, String> annotations = new HashMap<Identifier, String>();
		
		// get the cosine response
		String jsonStr = CosineService.lookupCosine(metadataPid.getValue());
		
		org.json.JSONObject json = new org.json.JSONObject(jsonStr);
		org.json.JSONArray graph = json.getJSONArray("@graph");

		int entityCount = 1;
		int attributeCount = 1;
		String attributeName = null;
		
		//process the results
		for (int i = 0; i < graph.length(); i++) {
			org.json.JSONObject entry = graph.getJSONObject(i);
			
			// the concepts there?
			if (!entry.has("http://purl.org/dc/terms/subject")) {
				continue;
			}
			
			// the pointer
			
			String pointer = (String) entry.get("@id");
			pointer = pointer.substring(pointer.indexOf("#"));
			String indices = pointer.replaceAll("\\D+", "_");
			indices = indices.substring(1, indices.length()-1);
			log.debug("indices=" + indices);
			entityCount = Integer.parseInt(indices.split("_")[0]);
			attributeCount = Integer.parseInt(indices.split("_")[1]);
			
			// the text
			String label = (String) entry.get("rdfs:label");
			attributeName = label;
			
			// the concept[s] - one annotation per concept
			org.json.JSONArray subjects = null;
			Object subjObj = entry.get("http://purl.org/dc/terms/subject");
			// handle single or multiple
			if (subjObj instanceof org.json.JSONArray) {
				subjects = (org.json.JSONArray) subjObj;
			} else {
				subjects = new org.json.JSONArray();
				subjects.put(subjObj);
			}
			
			for (int j = 0; j < subjects.length(); j++) {
				org.json.JSONObject a = subjects.getJSONObject(j);
				String url = a.getString("@id");
				double score = 0;
				System.out.println("url=" + url);

				ConceptItem c = new ConceptItem(new URI(url), score);
				ArrayList<ConceptItem> concepts = new ArrayList<ConceptItem>();
				concepts.add(c);
				
				// construct the annotation with this information
				JSONObject annotation = 
						constructAttributeAnnotation(
								sysMeta, 
								entityCount, 
								attributeCount, 
								attributeName, 
								concepts);
					
				// write the annotation out
				StringWriter sw = new StringWriter();
		    	annotation.writeJSONString(sw);
				Identifier pid = new Identifier();
				pid.setValue(annotation.get("id").toString());
				annotations.put(pid, sw.toString());	
					
			}
				
		}
		
		return annotations;
    }
    
    public JSONObject constructAttributeAnnotation(
    		SystemMetadata sysMeta, 
    		int entityCount, 
    		int attributeCount, 
    		String attributeName, 
    		List<ConceptItem> concepts) throws Exception {
    	
    	// capture the annotation
    	JSONObject annotation = createAnnotationTemplate(sysMeta);
    	
		// for selecting particular part of the metadata
		String xpointer = "#xpointer(/eml/dataset/dataTable[" + entityCount + "]/attributeList/attribute[" + attributeCount + "])";
    	
		// index using targeted field for the matching algorithm
		String fieldName = "sem_annotation";
		String matcher = conceptMatcher.getClass().getName();
		if (conceptMatcher instanceof BioPortalService) {
			fieldName = "sem_annotation_bioportal_sm";
		}
		if (conceptMatcher instanceof EsorService) {
			fieldName = "sem_annotation_esor_sm";
		}
		if (conceptMatcher instanceof CosineService) {
			fieldName = "sem_annotation_cosine_sm";
		}
		
    	annotation.put("field", fieldName);
    	annotation.put("resource", xpointer);
    	annotation.put("quote", attributeName);
    	annotation.put("oa:Motivation", "oa:tagging");
    	annotation.put("source", matcher);
    	
    	// target a (hopefully stable) div for the highlight
		
    	// the range for the highlighted text
    	JSONObject range = new JSONObject();
    	range.put("start", "//*[@id='sem_entity_" + entityCount + "_attribute_" + attributeCount + "']");
    	range.put("end", "//*[@id='sem_entity_" + entityCount + "_attribute_" + attributeCount + "']");
    	range.put("startOffset", 0);
    	range.put("endOffset", attributeName.length());
    	JSONArray ranges = new JSONArray();
    	ranges.add(range);
		annotation.put("ranges", ranges);
		
		if (concepts != null && concepts.size() > 0) {

			// add the concept[s] as tag[s]
			JSONArray tags = new JSONArray();
			for (ConceptItem conceptItem: concepts) {
				tags.add(conceptItem.getUri().toString());
				// only include one match since we are targeting MeasurementType subclasses
				break;
			}
			annotation.put("tags", tags);

			return annotation;
		}
		
		return null;

    }
    
    
    /**
     * Generates annotations from SOLR index entry
     * @param metadataPid
     * @return
     * @throws Exception
     */
    private Map<Identifier, String> generateAnnotationsFromIndex(Identifier metadataPid) throws Exception {

    	// get the values from solr
        String paramString = "?q=id:" + URLEncoder.encode("\"" + metadataPid.getValue() + "\"", "UTF-8") + "&fl=attribute,attributeName,origin,id" + "&wt=json";
        log.debug("paramString=" + paramString);
        
        InputStream solrStream = D1Client.getCN().query(null, "solr", paramString);
	
        JSONParser jsonParser = new JSONParser();
        Object results = jsonParser.parse(solrStream);
        solrStream.close();
        log.debug("results:" + results);
        JSONObject solrResults = (JSONObject) results;
        
        // get the first matching doc (should be only)
        JSONObject solrDoc = null;
        try {
        	solrDoc = (JSONObject) ((JSONArray)((JSONObject) solrResults.get("response")).get("docs")).get(0);
        } catch (Exception e) {
        	log.warn(e.getMessage(), e);
        	return null;
        }
        
    	SystemMetadata sysMeta = D1Client.getCN().getSystemMetadata(null, metadataPid);
    	
    	String context = solrDoc.get("abstract").toString();
    	
		Map<Identifier, String> annotations = new HashMap<Identifier, String>();
		
		// loop through the attributes
		JSONArray attributeNames = (JSONArray) solrDoc.get("attributeName");
		//JSONArray fullAttributes = (JSONArray) solrDoc.get("attribute");

		int attributeCount = 1;
		if (attributeNames != null) {
			Iterator<Object> attributeIter = attributeNames.iterator();
			
			while (attributeIter.hasNext()) {
				Object attribute = attributeIter.next();
				
				String attributeName = attribute.toString();
				//String attributeText = fullAttributes.get(attributeCount - 1).toString();

				log.debug("Attribute name: " + attributeName);
				//log.debug("Attribute text: " + attributeText);

				// capture the annotation
		    	JSONObject annotation = createAnnotationTemplate(sysMeta);
		    	
				// for selecting particular part of the metadata
				String xpointer = "#xpointer(//attribute[" + attributeCount + "])";
		    	
				// index using targeted field for the matching algorithm
				String fieldName = "sem_annotation";
				String matcher = conceptMatcher.getClass().getName();
				if (conceptMatcher instanceof BioPortalService) {
					fieldName = "sem_annotation_bioportal_sm";
				}
				if (conceptMatcher instanceof EsorService) {
					fieldName = "sem_annotation_esor_sm";
				}
				
		    	annotation.put("field", fieldName);
		    	annotation.put("resource", xpointer);
		    	annotation.put("quote", attributeName);
		    	annotation.put("oa:Motivation", "oa:tagging");
		    	annotation.put("source", matcher);
		    	
		    	// the range for the highlighted text
		    	JSONObject range = new JSONObject();
		    	range.put("start", "//*[@id='attributeName_" + attributeCount + "']/div[1]");
		    	range.put("end", "//*[@id='attributeName_" + attributeCount + "']/div[1]");
		    	range.put("startOffset", 0);
		    	range.put("endOffset", attributeName.length());
		    	JSONArray ranges = new JSONArray();
		    	ranges.add(range);
				annotation.put("ranges", ranges);
		    			
				// look up concepts for all the attribute text we have
				List<ConceptItem> concepts = conceptMatcher.getConcepts(attributeName, null, context);
				if (concepts != null && concepts.size() > 0) {
					// add the tags
					JSONArray tags = new JSONArray();
					for (ConceptItem conceptItem: concepts) {
						tags.add(conceptItem.getUri().toString());
						// TODO: more than one match?
						break;
					}
					annotation.put("tags", tags);
				} else {
					// no annotation for things without tags
					continue;
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
		
		
		if (orcidMatcher != null) {
			// look up creators from the EML metadata for an attribution annotation
			JSONArray creators = (JSONArray) solrDoc.get("origin");
			if (creators != null && creators.size() > 0) {	
				
				String creator = creators.get(0).toString();
				// use an orcid if we can find one from their system
				String creatorText = creator;
				List<ConceptItem> concepts = orcidMatcher.getConcepts(creatorText, null, null);
				if (concepts != null && concepts.size() > 0) {
					JSONObject annotation = createAnnotationTemplate(sysMeta);
					JSONArray creatorTags = new JSONArray();
					for (ConceptItem item: concepts) {
						String orcidUri = item.getUri().toString();
						creatorTags.add(orcidUri);
					}
					annotation.put("tags", creatorTags);
					
					String xpointer = "#xpointer(//origin[" + 1 + "])";
			    	annotation.put("field", "orcid_sm");
			    	annotation.put("resource", xpointer);
			    	annotation.put("quote", creator);
			    	annotation.put("oa:Motivation", "prov:wasAttributedTo");
			    	
			    	// the range for the highlighted text
			    	JSONObject range = new JSONObject();
			    	range.put("start", "//*[@id='origin_" + 1 + "']/div[1]");
			    	range.put("end", "//*[@id='origin_" + 1 + "']/div[1]");
			    	range.put("startOffset", 0);
			    	range.put("endOffset", creator.length());
			    	JSONArray ranges = new JSONArray();
			    	ranges.add(range);
					annotation.put("ranges", ranges);
			    	
			    	StringWriter sw = new StringWriter();
					annotation.writeJSONString(sw);
					Identifier pid = new Identifier();
					pid.setValue(annotation.get("id").toString());
					annotations.put(pid, sw.toString());
				} else {
					// do nothing
				}
			}
		}
		
		
		return annotations;
		
	}
	
	private JSONObject createAnnotationTemplate(SystemMetadata sysMeta) throws Exception {
		

		// reusable fields for each annotation we generated
    	JSONObject annotation = new JSONObject();
    	
    	// each needs a unique ID
    	String uuid = UUID.randomUUID().toString();
		Identifier annotationPid = new Identifier();
		annotationPid.setValue(uuid);
		
    	annotation.put("id", annotationPid.getValue());
    	annotation.put("pid", sysMeta.getIdentifier().getValue());
    	annotation.put("uri", Settings.getConfiguration().getProperty("D1Client.CN_URL") + "/v2/resolve/" + sysMeta.getIdentifier().getValue());
    	annotation.put("consumer", Settings.getConfiguration().getProperty("annotator.consumerKey"));
    	
    	// TODO: transfer all permissions from sysmeta?
    	JSONObject permissions = (JSONObject) JSONValue.parse(
    			"{" +
    				"\"read\": [\"group:__world__\"], " +
    				"\"update\": [" +
    				"\"" +
    					sysMeta.getRightsHolder().getValue()
    				+ "\"" +
    				"], " +
    		        "\"delete\": [" +
    		        "\"" +
    		        	sysMeta.getRightsHolder().getValue()
    		        + "\"" +
    		        "], " +
    		        "\"admin\": [" +
    		        "\"" +
    		        	sysMeta.getRightsHolder().getValue()
    		        + "\"" +
    		        "]" +
    		     "}"
    			);
		annotation.put("permissions", permissions);
		String now = DateTimeMarshaller.serializeDateToUTC(Calendar.getInstance().getTime());
		annotation.put("created", now);
		annotation.put("updated", now);
		//TODO: who is considered the automated user?
		//annotation.put("user", Settings.getConfiguration().getProperty("cn.nodeSubject"));
		annotation.put("user", sysMeta.getRightsHolder().getValue());

		return annotation;
	}
	
	
	private DataPackage getDataPackage(Identifier pid) throws Exception {
		// get package from Member
		InputStream emlStream = D1Client.getCN().get(null, pid);

		// parse the metadata
		DataPackageParserInterface parser = new Eml200DataPackageParser();
		parser.parse(emlStream);
		emlStream.close();
		DataPackage dataPackage = parser.getDataPackage();
		return dataPackage;
	}
	
	public static void main(String[] args) throws Exception {

			testGenerate();
			System.exit(0);
	}
	
	public static void testGenerate() throws Exception {
		Settings.getConfiguration().setProperty("D1Client.CN_URL", "https://cn-sandbox-2.test.dataone.org/cn");
		Identifier metadataPid = new Identifier();
		metadataPid.setValue("https://pasta.lternet.edu/package/metadata/eml/knb-lter-arc/20032/2");
		JsonAnnotatorGenerator ds = new JsonAnnotatorGenerator();
		Iterator<String> annotations = ds.generateAnnotations(metadataPid).values().iterator();
		while (annotations.hasNext()) {
			String jsonString = annotations.next();
			log.debug("JSON annotation: \n" + jsonString );
		}
		
	}
	
	
}
