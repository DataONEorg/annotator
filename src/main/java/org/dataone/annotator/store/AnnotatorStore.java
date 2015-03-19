package org.dataone.annotator.store;

public interface AnnotatorStore {

	/**
	 * Create a new annotation from given object
	 * @param annotation
	 * @return the generated identifier for the annotation
	 * @throws Exception
	 */
	public abstract String create(String annotationContent) throws Exception;

	/**
	 * Read the annotation for given id
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public abstract String read(String id) throws Exception;

	/**
	 * Update the given annotation using SID
	 * @param id
	 * @param partialAnnotation
	 * @return
	 */
	public abstract String update(String id, String partialAnnotationContent)
			throws Exception;

	/**
	 * TODO: allow full delete?
	 * Remove the annotation from the store
	 * @param id
	 * @throws Exception
	 */
	public abstract void delete(String id) throws Exception;

	/**
	 * Query annotation store using given query expression
	 * @param query
	 * @return result listing the total matches and each annotation as a "row"
	 * @throws Exception
	 */
	public abstract String search(String query) throws Exception;

	/**
	 * Show the API version information for this store
	 * @return
	 */
	public abstract String root();

	/**
	 * List all the annotations in the store
	 * @return
	 * @throws Exception
	 */
	public abstract String index() throws Exception;

	/**
	 * Check if the annotation exists 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public abstract boolean exists(String id) throws Exception;

}