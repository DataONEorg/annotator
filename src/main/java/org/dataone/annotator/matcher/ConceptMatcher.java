package org.dataone.annotator.matcher;

import java.util.List;

public interface ConceptMatcher {
	
	public List<String> getConcepts(String fullText);

}
