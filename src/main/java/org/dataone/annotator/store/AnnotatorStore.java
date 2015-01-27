/**
 * 
 */
package org.dataone.annotator.store;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.portal.TokenGenerator;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.DateTimeMarshaller;


import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * @author leinfelder
 *
 */
public class AnnotatorStore {

    public static Log log = LogFactory.getLog(AnnotatorStore.class);

    public static final String ANNOTATION_FORMAT_ID = "http://docs.annotatorjs.org/en/v1.2.x/annotation-format.html";

	private static final String DEFAULT_ENCODING = "UTF-8";
	
	private MNode storageNode = null;

	private Session session;
	
	public AnnotatorStore(HttpServletRequest request) throws BaseException {
		
		// look for certificate-based session (d1 default) 
		try {
			session = CertificateManager.getInstance().getSession(request);
		} catch (InvalidToken e) {
			log.warn(e.getMessage(), e);
		}
		
		// try getting it from the token (annotator library)
		if (session == null) {
			String token = request.getHeader("x-annotator-auth-token");
			session = TokenGenerator.getSession(token);
		}
		
		// try getting the certificate from the portal so that we can act as proxy for the user
		if (session == null) {
			try {
				// register the portal certificate with the certificate manager for the calling subject
				X509Certificate certificate = PortalCertificateManager.getInstance().getCertificate(request);
				PrivateKey key = PortalCertificateManager.getInstance().getPrivateKey(request);
				String certSubject = CertificateManager.getInstance().getSubjectDN(certificate);
				CertificateManager.getInstance().registerCertificate(certSubject , certificate, key);
				
				// now the methods will "know" who is calling them - used in conjunction with Certificate Manager
				session = new Session();
				Subject subject = new Subject();
				subject.setValue(certSubject);
				session.setSubject(subject);
				
			} catch (Exception e) {
				log.error("cound not register user session from portal: " + e.getMessage(), e);
			}
		}
		
		// NOTE: if session is null at this point, we are default to whatever CertificateManager has
		// which may not be the original user from the web
		
		// use an available MN - assume replica nodes allow write operations
		NodeReference nodeRef = null;
		Iterator<Node> nodeIter = D1Client.getCN().listNodes().getNodeList().iterator();
		while (nodeIter.hasNext()) {
			Node node = nodeIter.next();
			if (node.getType().equals(NodeType.MN) && node.isReplicate()) {
				nodeRef = node.getIdentifier();
			}
		}
		
		// use this node for storing/retrieving annotations
		//storageNode = D1Client.getMN(session);
		storageNode = D1Client.getMN(nodeRef );

		
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

		NodeReference authoritativeMemberNode = storageNode.getNodeId();
		sysmeta.setAuthoritativeMemberNode(authoritativeMemberNode );
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
	
	/**
	 * Create a new annotation from given object
	 * @param annotation
	 * @return the generated identifier for the annotation
	 * @throws Exception
	 */
	public String create(JSONObject annotation) throws Exception {
		
		// use the dataone API to create an object for the annotation
		
		// create identifiers for the object
		Identifier pid = storageNode.generateIdentifier(session, "UUID", "annotation");
		Identifier sid = storageNode.generateIdentifier(session, "UUID", "annotation");
		
		// add properties to the annotation
		// TODO: use SID for the identifier when implemented
		annotation.put("id", pid.getValue());
		annotation.put("user", session.getSubject().getValue());
		Date now = Calendar.getInstance().getTime();
		annotation.put("created", DateTimeMarshaller.serializeDateToUTC(now));
		annotation.put("updated", DateTimeMarshaller.serializeDateToUTC(now));

		// generate sys meta
		SystemMetadata sysmeta = computeSystemMetadata(annotation);
		sysmeta.setIdentifier(pid);
		sysmeta.setSeriesId(sid);
		
		// create it on the node
		InputStream object = new ByteArrayInputStream(annotation.toJSONString().getBytes(DEFAULT_ENCODING));
		storageNode.create(session, pid, object, sysmeta);
		
		return pid.getValue();
	}

	/**
	 * Read the annotation for given id
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public JSONObject read(String id) throws Exception {
		// read the annotation out as JSON object
		Identifier sid = new Identifier();
		sid.setValue(id);
		InputStream object = storageNode.get(session, sid);
		JSONObject annotation = (JSONObject) JSONValue.parse(object);
		return annotation;
	}

	/**
	 * TODO: implement when series ID is supported
	 * @param id
	 * @param partialAnnotation
	 * @return
	 */
	public JSONObject update(String id, JSONObject partialAnnotation) throws Exception {
		// we really just use SID to mke it look like an update to the same identifier
		
		// use the dataone API to update the annotation
		Identifier sid = new Identifier();
		sid.setValue(id);
		
		// get the original pid
		Identifier originalPid = storageNode.getSystemMetadata(session, sid).getIdentifier();
		
		// create identifier for the new revision
		Identifier pid = storageNode.generateIdentifier(session, "UUID", "annotation");
				
		// get the existing annotation content
		JSONObject annotation = this.read(id);
		
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
		
		return annotation;
		
	}

	/**
	 * TODO: allow full delete?
	 * Remove the annotation from the store
	 * @param id
	 * @throws Exception
	 */
	public void delete(String id) throws Exception {
		// read the annotation out as JSON object
		Identifier sid = new Identifier();
		sid.setValue(id);
		
		//storageNode.archive(session, sid);
		storageNode.delete(session, sid);


	}

	/**
	 * Query annotation store using given query expression
	 * @param query
	 * @return result listing the total matches and each annotation as a "row"
	 * @throws Exception
	 */
	public JSONObject search(String query) throws Exception {
		
		JSONObject results = new JSONObject();
		
		// TODO: better search algorithm!
		JSONArray annotations = this.index();
		
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
		return results ;
	}

	/**
	 * Show the API version information for this store
	 * @return
	 */
	public JSONObject root() {
		JSONObject versionInfo = new JSONObject();
		versionInfo.put("name", "Metacat Annotator Store API");
		versionInfo.put("version", "1.2.9");
		return versionInfo ;
	}

	/**
	 * List all the annotations in the store
	 * @return
	 * @throws Exception
	 */
	public JSONArray index() throws Exception {
		
		JSONArray annotations = new JSONArray();
		ObjectFormatIdentifier objectFormatId = new ObjectFormatIdentifier();
		objectFormatId.setValue(ANNOTATION_FORMAT_ID);
		Integer start = 0;
		Integer count = 1000;
		ObjectList objects = storageNode.listObjects(session, null, null, objectFormatId, null, true, start, count);
		//ObjectList objects = storageNode.listObjects(session, null, null, objectFormatId, true, start, count);

		for (ObjectInfo info: objects.getObjectInfoList()) {
			Identifier pid = info.getIdentifier();
			SystemMetadata sysMeta = storageNode.getSystemMetadata(session, pid);
			// remember we don't have true delete yet
			if ( (sysMeta.getArchived() != null && sysMeta.getArchived().booleanValue()) || sysMeta.getObsoletedBy() != null) {
				continue;
			}
			JSONObject annotation = this.read(pid.getValue());
			annotations.add(annotation);
		}

		return annotations ;
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

