package org.dataone.annotator.matcher;

import java.net.URI;

public class ConceptItem {

	protected URI uri;
	
	protected String label;
	
	protected String definition;
	
	protected double weight;
	
	public ConceptItem() {}
	
	public ConceptItem(URI uri, double weight) {
		this.uri = uri;
		this.weight = weight;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}
	

}
