/**
 * 
 */
package org.dataone.annotator.store;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.DateTimeMarshaller;

/**
 * @author leinfelder
 *
 */
public class JsonAnnotatorStore implements AnnotatorStore {

    public static Log log = LogFactory.getLog(JsonAnnotatorStore.class);

    public static final String ANNOTATION_FORMAT_ID = "http://docs.annotatorjs.org/en/v1.2.x/annotation-format.html";

	private static final String DEFAULT_ENCODING = "UTF-8";
	
	private MNode storageNode = null;

	private Session session;
	
	public JsonAnnotatorStore(Session session) throws BaseException {
		
		// use the session that is passed in, assume the certificate configuration has been completed
		this.session = session;
		
		// use configured node ref
		NodeReference nodeRef = null;
		String nodeId = Settings.getConfiguration().getString("annotator.nodeid");
		if (nodeId != null) {
			nodeRef = new NodeReference();
			nodeRef.setValue(nodeId);
		}

		// use another available MN with tier 3 support enabled
		if (nodeRef == null) {
			Iterator<Node> nodeIter = D1Client.getCN().listNodes().getNodeList().iterator();
			while (nodeIter.hasNext()) {
				Node node = nodeIter.next();
				if (node.getType().equals(NodeType.MN)) {
					for (Service service: node.getServices().getServiceList()) {
						if (service.getName().equalsIgnoreCase("MNStorage") && service.getAvailable()) {
							nodeRef = node.getIdentifier();
							break;
						}
					}
				}
			}
		}

		// use this node for storing/retrieving annotations
		//storageNode = D1Client.getMN(session);
		storageNode = D1Client.getMN(nodeRef);
		
	}
	
	/**
	 * Generate minimal systemmetadata for new/updated annotation objects
	 * @param annotation
	 * @return
	 * @throws Exception
	 */
	private SystemMetadata computeSystemMetadata(JSONObject annotation) throws Exception {
		SystemMetadata sysmeta = new SystemMetadata();
		
		ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
		formatId.setValue(ANNOTATION_FORMAT_ID);
		sysmeta.setFormatId(formatId);
		
		BigInteger size = BigInteger.valueOf(annotation.toJSONString().getBytes(DEFAULT_ENCODING).length);
		sysmeta.setSize(size);
		
		Checksum checksum = ChecksumUtil.checksum(annotation.toJSONString().getBytes(DEFAULT_ENCODING), "MD5");
		sysmeta.setChecksum(checksum);
		
		Subject rightsHolder = session.getSubject();
		sysmeta.setRightsHolder(rightsHolder);
		sysmeta.setSubmitter(rightsHolder);

		// TODO: update this when bug is fixed: https://redmine.dataone.org/issues/6955
		//NodeReference authoritativeMemberNode = storageNode.getNodeId();
		NodeReference authoritativeMemberNode = storageNode.getCapabilities().getIdentifier();

		sysmeta.setAuthoritativeMemberNode(authoritativeMemberNode);
		sysmeta.setOriginMemberNode(authoritativeMemberNode);
		
		sysmeta.setDateSysMetadataModified(DateTimeMarshaller.deserializeDateToUTC(annotation.get("updated").toString()));
		sysmeta.setDateUploaded(DateTimeMarshaller.deserializeDateToUTC(annotation.get("created").toString()));		
	
		// add access access rules for read
		AccessPolicy accessPolicy = new AccessPolicy();
		JSONObject permissions =  (JSONObject) annotation.get("permissions");
		JSONArray readList = (JSONArray) permissions.get("read");
		for (Object read: readList) {
			AccessRule accessRule = new AccessRule();
			Subject user = new Subject();
			
			String username = read.toString();
			if (username.equals("group:__world__")) {
				user.setValue(Constants.SUBJECT_PUBLIC);
			}
			accessRule.addSubject(user);
			accessRule.addPermission(Permission.READ);
			accessPolicy.addAllow(accessRule);
			
		}
		sysmeta.setAccessPolicy(accessPolicy);

		return sysmeta;
	}
	
	/* (non-Javadoc)
	 * @see org.dataone.annotator.store.AnnotatorStore#create(java.lang.String)
	 */
	@Override
	public String create(String annotationContent) throws Exception {
		
		//parse for manipulation
		JSONObject annotation = (JSONObject) JSONValue.parse(annotationContent);
				
		// use the dataone API to create an object for the annotation
		
		// create identifiers for the object
		Identifier pid = storageNode.generateIdentifier(session, "UUID", "annotation");
		Identifier sid = storageNode.generateIdentifier(session, "UUID", "annotation");
		
		// add properties to the annotation
		annotation.put("id", sid.getValue());
		annotation.put("user", session.getSubject().getValue());
		((JSONArray)((JSONObject) annotation.get("permissions")).get("admin")).add(session.getSubject().getValue());
		((JSONArray)((JSONObject) annotation.get("permissions")).get("update")).add(session.getSubject().getValue());
		((JSONArray)((JSONObject) annotation.get("permissions")).get("delete")).add(session.getSubject().getValue());
		
		Date now = Calendar.getInstance().getTime();
		annotation.put("created", DateTimeMarshaller.serializeDateToUTC(now));
		annotation.put("updated", DateTimeMarshaller.serializeDateToUTC(now));

		// generate sys meta
		SystemMetadata sysmeta = computeSystemMetadata(annotation);
		sysmeta.setIdentifier(pid);
		sysmeta.setSeriesId(sid);
		
		log.warn("Session subject: " + session.getSubject().getValue());
		log.warn("Creating annotation created by: " + sysmeta.getRightsHolder().getValue());
		
		// create it on the node
		InputStream object = new ByteArrayInputStream(annotation.toJSONString().getBytes(DEFAULT_ENCODING));
		storageNode.create(session, pid, object, sysmeta);
		
		return sid.getValue();
	}

	/* (non-Javadoc)
	 * @see org.dataone.annotator.store.AnnotatorStore#read(java.lang.String)
	 */
	@Override
	public String read(String id) throws Exception {
		// read the annotation out as a String object
		Identifier sid = new Identifier();
		sid.setValue(id);
		InputStream object = FileBasedCache.get(id);
		if (object == null) {
			object = storageNode.get(session, sid);
			FileBasedCache.cache(id, object);
			object = FileBasedCache.get(id);
		}
		
		return IOUtils.toString(object, "UTF-8");
	}

	/* (non-Javadoc)
	 * @see org.dataone.annotator.store.AnnotatorStore#update(java.lang.String, java.lang.String)
	 */
	@Override
	public String update(String id, String partialAnnotationContent) throws Exception {
		
		//parse for manipulation
		JSONObject partialAnnotation = (JSONObject) JSONValue.parse(partialAnnotationContent);
		
		// we really just use SID to make it look like an update to the same identifier
		
		// use the dataone API to update the annotation
		Identifier givenIdentifier = new Identifier();
		givenIdentifier.setValue(id);
		
		// get the original pid and sid
		SystemMetadata originalSystemMetadata = storageNode.getSystemMetadata(session, givenIdentifier);
		Identifier originalPid = originalSystemMetadata.getIdentifier();
		Identifier sid = originalSystemMetadata.getSeriesId();

		// create identifier for the new revision
		Identifier pid = storageNode.generateIdentifier(session, "UUID", "annotation");
				
		// get the existing annotation content
		String annotationContent = this.read(id);
		
		JSONObject annotation = (JSONObject) JSONValue.parse(annotationContent);
		
		// merge the existing and new properties
		annotation.putAll(partialAnnotation);
		
		// add audit properties to the annotation
		annotation.put("id", sid.getValue());
		annotation.put("user", session.getSubject().getValue());
		Date now = Calendar.getInstance().getTime();
		//annotation.put("created", DateTimeMarshaller.serializeDateToUTC(now));
		annotation.put("updated", DateTimeMarshaller.serializeDateToUTC(now));

		// generate sys meta
		SystemMetadata sysmeta = computeSystemMetadata(annotation);
		sysmeta.setIdentifier(pid);
		sysmeta.setSeriesId(sid);
		sysmeta.setObsoletes(originalPid);
		
		// update it on the node
		InputStream object = new ByteArrayInputStream(annotation.toJSONString().getBytes(DEFAULT_ENCODING));
		storageNode.update(session, originalPid, object, pid, sysmeta);
		FileBasedCache.remove(id);
		
		return annotation.toJSONString();
		
	}

	/* (non-Javadoc)
	 * @see org.dataone.annotator.store.AnnotatorStore#delete(java.lang.String)
	 */
	@Override
	public void delete(String id) throws Exception {
		// read the annotation out as JSON object
		Identifier sid = new Identifier();
		sid.setValue(id);
		
		storageNode.archive(session, sid);
		
		FileBasedCache.remove(id);
		
		//TODO: allow deletes by anyone, not just the node admin
		//storageNode.delete(session, sid);


	}

	/* (non-Javadoc)
	 * @see org.dataone.annotator.store.AnnotatorStore#search(java.lang.String)
	 */
	@Override
	public String search(String query) throws Exception {
		return searchIndex(query, false);
		//return searchList(query);

	}
	
	/**
	 * A naive "search" that lists the objects, then filters them.
	 * This is not an efficient search technique and should be avoided 
	 * in favor of the solr-base search method
	 * @deprecated
	 */
	private String searchList(String query) throws Exception {
		
		JSONObject results = new JSONObject();
		
		String annotationsContent = this.index();
		JSONArray annotations = (JSONArray) JSONValue.parse(annotationsContent);
		
		Collection<Predicate> predicates = new ArrayList<Predicate>();
		List<NameValuePair> criteria = URLEncodedUtils.parse(query, Charset.forName(DEFAULT_ENCODING));
		for (NameValuePair pair: criteria) {
			if (pair.getName().equals("limit") || pair.getName().equals("offset")) {
				continue;
			}
			// otherwise add the criteria
			predicates.add(new AnnotationPredicate(pair.getName(), pair.getValue()));
			
		}
		Predicate allPredicate = PredicateUtils.allPredicate(predicates);
		CollectionUtils.filter(annotations, allPredicate);
		
		results.put("total", annotations.size());
		results.put("rows", annotations);
		
		return results.toJSONString();
	}
	
	
	/**
	 * A less naive search that filters the initial result with a solr query, then filters it 
	 * again with any additional criteria that are not exposed in the solr index.
	 * This is the preferred implementation
	 * @param query
	 * @return
	 * @throws Exception
	 */
	private String searchIndex(String query, boolean isList) throws Exception {
		
		String solrQuery = "q=" + URLEncoder.encode("formatId:\"" + ANNOTATION_FORMAT_ID + "\"", "UTF-8");

		// parse the query syntax
		Collection<Predicate> predicates = new ArrayList<Predicate>();
		List<NameValuePair> criteria = URLEncodedUtils.parse(query, Charset.forName(DEFAULT_ENCODING));
		for (NameValuePair pair: criteria) {
			// ignore these parameters for paging
			if (pair.getName().equals("limit") || pair.getName().equals("offset")) {
				continue;
			}
			// initial filter by the uri that is being annotated - use only the identifier portion
			if (pair.getName().equals("uri")) {
				String pid = pair.getValue();
				
				// substring to find pid, assume either resolve or object endpoint is used
				String resolveToken =  "/" + Constants.RESOURCE_RESOLVE + "/";
				String objectToken =  "/" + Constants.RESOURCE_OBJECTS + "/";
				if (pid.contains(resolveToken)) {
					pid = pid.split(resolveToken)[1];
				} else if (pid.contains(objectToken)) {
					pid = pid.split(objectToken)[1];
				} else {
					// Do nothing: not sure what format it is in
				}
				
				solrQuery += URLEncoder.encode("+sem_annotates:\"" + pid + "\"", "UTF-8");
			}
			// add the criteria for further filtering after initial retrieval
			predicates.add(new AnnotationPredicate(pair.getName(), pair.getValue()));
		}
		
		solrQuery += URLEncoder.encode("-obsoletedBy:*", "UTF-8");
		
		//more solr options
		solrQuery += "&fl=id,sem_annotates,sem_annotated_by&wt=json&rows=10000";
		log.debug("solrQuery = " + solrQuery);

		// search the index
		InputStream solrResultStream = storageNode.query(session, "solr", solrQuery);

		JSONObject solrResults = (JSONObject) JSONValue.parse(solrResultStream);
		log.debug("solrResults = " + solrResults.toJSONString());
		
		JSONArray annotations = new JSONArray();

		if (solrResults != null && solrResults.containsKey("response")) {
			JSONArray solrDocs = (JSONArray)((JSONObject) solrResults.get("response")).get("docs");
			log.debug("solrDocs = " + solrDocs.toJSONString());
			
			for (Object solrDoc: solrDocs) {
				log.debug("solrDoc = " + solrDoc.toString());
				
				String id = ((JSONObject) solrDoc).get("id").toString();
				log.debug("id = " + id);
				
				// check if archived
				// TODO: needed?
//				Identifier pid = new Identifier();
//				pid.setValue(id);
//				SystemMetadata sysMeta = storageNode.getSystemMetadata(session, pid);
//				if ( (sysMeta.getArchived() != null && sysMeta.getArchived().booleanValue()) || sysMeta.getObsoletedBy() != null) {
//					continue;
//				}
				
				String annotationContent = this.read(id);
				JSONObject annotation = (JSONObject) JSONValue.parse(annotationContent);
				log.debug("annotation = " + annotation);

				annotations.add(annotation);
			}
		}
		
		// apply additional filters for fields that may not be in the solr index
		Predicate allPredicate = PredicateUtils.allPredicate(predicates);
		CollectionUtils.filter(annotations, allPredicate);
		
		// reuse this method for search and index implementations
		if (isList) {
			return annotations.toJSONString();
		}
		
		JSONObject results = new JSONObject();
		results.put("total", annotations.size());
		results.put("rows", annotations);

		return results.toJSONString();
	}

	/* (non-Javadoc)
	 * @see org.dataone.annotator.store.AnnotatorStore#root()
	 */
	@Override
	public String root() {
		JSONObject versionInfo = new JSONObject();
		versionInfo.put("name", "Metacat Annotator Store API");
		versionInfo.put("version", "1.2.9");
		return versionInfo.toJSONString();
	}

	/* (non-Javadoc)
	 * @see org.dataone.annotator.store.AnnotatorStore#index()
	 */
	@Override
	public String index() throws Exception {
		String query = "limit=-1";
		return searchIndex(query, true);
	}

	/* (non-Javadoc)
	 * @see org.dataone.annotator.store.AnnotatorStore#exists(java.lang.String)
	 */
	@Override
	public boolean exists(String id) throws Exception {
		try {
			String object = this.read(id);
			return object != null;
		} catch (NotFound e) {
			return false;
		}
	}

}

class AnnotationPredicate implements Predicate {

	private String name;
	private String value;
	
	public AnnotationPredicate(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	@Override
	public boolean evaluate(Object obj) {
		JSONObject annotation = (JSONObject) obj;
		// simple string equals for now
		String actualValue = (String) annotation.get(name);
		if (actualValue == null) {
			return false;
		}
		return actualValue.equals(value);
	}
		
}

