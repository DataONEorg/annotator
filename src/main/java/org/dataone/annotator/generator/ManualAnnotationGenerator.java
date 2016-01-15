package org.dataone.annotator.generator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONObject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.annotator.generator.json.JsonAnnotatorGenerator;
import org.dataone.annotator.matcher.ConceptItem;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

public class ManualAnnotationGenerator {

	private static Log log = LogFactory.getLog(ManualAnnotationGenerator.class);

	private static final String ECSO_PREFIX = "http://purl.dataone.org/odo/ECSO_";

	private Session session;
	
	public ManualAnnotationGenerator() {}
	
	public ManualAnnotationGenerator(Session session) {
		this.session = session;
	}

	public void generateAndUpload(String pidFile) throws Exception {
		
		JsonAnnotatorGenerator jag = new JsonAnnotatorGenerator();
		AnnotationUploader uploader = new AnnotationUploader(session);
		
		SystemMetadata sysMeta = null;

		// fetch the file
		URL url = new URL(pidFile);
    	InputStream inputStream = url.openStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		
		// parse it
		Iterable<CSVRecord> records = CSVFormat.TDF.withHeader().parse(reader);
		
		int count = 0;
		// iterate over the records
		for (CSVRecord record : records) {
			// get the info from the csv file
		    String pkg_id = record.get("pkg_id");
		    String ent_no = record.get("ent_no");
		    String attr_no = record.get("attr_no");
		    String class_id_int = record.get("class_id_int");
		    
		    // skip records without a concept
		    if (class_id_int == null || class_id_int.length() == 0) {
		    	continue;
		    }
    	
		    // look up the pid if it has changed from previous record
			String pid = pkg_id;
	    	Identifier metadataPid = new Identifier();
			metadataPid.setValue(pid);
    		log.debug("Processing referenced pid: " + pid);
	    	if (sysMeta == null || !sysMeta.getIdentifier().equals(metadataPid)) {
	    		log.debug("Fetching System Metadata for: " + pid);
	    		sysMeta = D1Client.getCN().getSystemMetadata(null, metadataPid);
	    	}
			int entityCount = Integer.valueOf(ent_no);
			int attributeCount = Integer.valueOf(attr_no);
			String attributeName = "PLACEHOLDER";
			// pad the given partial URI for ECSO terms
			List<ConceptItem> concepts = new ArrayList<ConceptItem>();
			String partialUri = class_id_int;
			partialUri = String.format("%8s", partialUri).replace(' ', '0');  
			URI uri = new URI(ECSO_PREFIX + partialUri);
			ConceptItem item = new ConceptItem(uri , 1.0);
			concepts.add(item);
			
			// construct the annotation
			JSONObject annotation = jag.constructAttributeAnnotation(sysMeta, entityCount, attributeCount, attributeName, concepts);
			String annotationContent = annotation.toJSONString();
			String annotationPid = annotation.get("id").toString();
			log.debug("Manual annotation, " + annotationPid + " = " + annotationContent);
			
			Identifier annotationIdentifier = new Identifier();
			annotationIdentifier.setValue(annotationPid);
			
			// upload it
			uploader.insertOrUpdate(annotationIdentifier , annotationContent);
			count++;
			
		}
		
		log.debug("Total # annotations generated: " + count);
		
	}
	
}
