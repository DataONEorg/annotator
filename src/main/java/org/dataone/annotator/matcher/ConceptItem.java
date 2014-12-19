package org.dataone.annotator.matcher;

import java.net.URI;

public class ConceptItem {

	protected URI uri;
	
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
	

}
