package org.dataone.annotator.store;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MockAnnotatorStore implements AnnotatorStore {

	private Map<String, String> annotations = new HashMap<String, String>();
	
	@Override
	public String create(String annotationContent) throws Exception {
		String id = UUID.randomUUID().toString();
		annotations.put(id, annotationContent);
		return id;
	}

	@Override
	public String read(String id) throws Exception {
		return annotations.get(id);
	}

	@Override
	public String update(String id, String partialAnnotationContent)
			throws Exception {
		// TODO: merge
		annotations.put(id, partialAnnotationContent);
		return partialAnnotationContent;
	}

	@Override
	public void delete(String id) throws Exception {
		annotations.remove(id);
	}

	@Override
	public String search(String query) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String root() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String index() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists(String id) throws Exception {
		return annotations.containsKey(id);
	}

}
