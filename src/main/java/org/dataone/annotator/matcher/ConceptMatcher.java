package org.dataone.annotator.matcher;

import java.util.List;

public interface ConceptMatcher {
	
	public List<ConceptItem> getConcepts(String fullText) throws Exception;

}
