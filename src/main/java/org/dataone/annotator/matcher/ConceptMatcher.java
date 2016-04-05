package org.dataone.annotator.matcher;

import java.util.List;
import java.util.Map;

public interface ConceptMatcher {
	
	public List<ConceptItem> getConcepts(String fullText, String unit, String context) throws Exception;

	public List<ConceptItem> getConcepts(Map<String, String> terms) throws Exception;

}
