package org.dataone.annotator.generator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.annotator.store.AnnotatorStore;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;

public class AnnotationUploader {
	
	private static Log log = LogFactory.getLog(AnnotationUploader.class);
		
	private AnnotationGenerator generator = null;
	
	private AnnotatorStore store = null;
	
	public AnnotationUploader(Session session) throws Exception {
				
		String generatorClassName = Settings.getConfiguration().getString("annotator.generator.className");
		generator = (AnnotationGenerator) Class.forName(generatorClassName).newInstance();
		
		String storeClassName = Settings.getConfiguration().getString("annotator.store.className");
		try {
			store = (AnnotatorStore) Class.forName(storeClassName).getDeclaredConstructor(Session.class).newInstance(session);		
		} catch (NoSuchMethodException nsme) {
			log.warn("Could not construct annotator store with session parameter using impl: " + storeClassName);
			store = (AnnotatorStore) Class.forName(storeClassName).newInstance();
		}
	}
	
	public void process(List<String> identifiers) throws Exception {
		
		// loop through our annotations
		for (String identifier: identifiers) {
			log.debug("Generating annotations for: " + identifier);
			Identifier pid = new Identifier();
			pid.setValue(identifier);
			Map<Identifier, String> annotations = generator.generateAnnotations(pid);
			
			Iterator<Entry<Identifier, String>> annotationIter = annotations.entrySet().iterator();
			while (annotationIter.hasNext()) {
				Entry<Identifier, String> entry = annotationIter.next();
				Identifier annotationIdentifier = entry.getKey();
				String annotationContent = entry.getValue();
				log.debug("Annotation: " + annotationContent);
				if (store.exists(annotationIdentifier.getValue())) {
					log.debug("Updating annotation: " + annotationIdentifier.getValue());
					store.update(annotationIdentifier.getValue(), annotationContent);
				} else {
					log.debug("Creating annotation: " + annotationIdentifier.getValue());
					store.create(annotationContent);
				}
			}
		}	
	}
	
}
