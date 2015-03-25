/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: leinfelder $'
 *     '$Date: 2014-12-09 13:10:31 -0800 (Tue, 09 Dec 2014) $'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.dataone.annotator.store;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.CertificateManager;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.portal.TokenGenerator;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;

/**
 * Metacat REST handler for Annotator storage API
 * http://docs.annotatorjs.org/en/v1.2.x/storage.html#core-storage-api 
 *  
 */
public class AnnotatorRestServlet extends HttpServlet {

    public static Log log = LogFactory.getLog(AnnotatorRestServlet.class);
    
    public static Session getSession(HttpServletRequest request) throws BaseException {
		
		log.debug("Inspecting request for session information");
	
		Session session = null;
				
		// look for certificate-based session (d1 default) 
		try {
			session = CertificateManager.getInstance().getSession(request);
			log.debug("Session from original request: " + session);

		} catch (InvalidToken e) {
			log.warn(e.getMessage(), e);
		}
		
		// try getting it from the token (annotator library)
		if (session == null) {
			debugHeaders(request);
			String token = request.getHeader("x-annotator-auth-token");
			session = TokenGenerator.getSession(token);
			log.debug("Session from x-annotator-auth-token: " + session);
		}
		
		// see if we can proxy as the user
		if (session != null) {
			try {
				
				log.warn("looking up certificate from portal");
				
				// register the portal certificate with the certificate manager for the calling subject
				X509Certificate certificate = PortalCertificateManager.getInstance().getCertificate(request);
				PrivateKey key = PortalCertificateManager.getInstance().getPrivateKey(request);
				String certSubject = CertificateManager.getInstance().getSubjectDN(certificate);
				String sessionSubject = session.getSubject().getValue();

				// TODO: verify that the users are the same
				log.warn("Certifcate subject: " + certSubject);
				log.warn("Session subject: " + sessionSubject);

				// now the methods will "know" who is calling them - used in conjunction with Certificate Manager
				CertificateManager.getInstance().registerCertificate(certSubject , certificate, key);
				log.warn("Registered portal certificate for: " + certSubject);

				session = new Session();
				Subject subject = new Subject();
				subject.setValue(certSubject);
				session.setSubject(subject);
				
			} catch (Exception e) {
				log.error("cound not register user session from portal: " + e.getMessage(), e);
			}
		}
		
		// FIXME: for now, just use the CN certificate for everything
//		try {
//			String nodeProperties = "/etc/dataone/node.properties";
//			Settings.augmentConfiguration(nodeProperties);
//			String certificateDirectory = Settings.getConfiguration().getString("D1Client.certificate.directory");
//			String certificateFilename = Settings.getConfiguration().getString("D1Client.certificate.filename");
//			String certificateLocation = certificateDirectory + File.separator + certificateFilename;
//			CertificateManager.getInstance().setCertificateLocation(certificateLocation);
//			Subject subject =  ClientIdentityManager.getCurrentIdentity();
//			session = new Session();
//			session.setSubject(subject);
//		} catch (Exception e) {
//			ServiceFailure sf = new ServiceFailure("000", e.getMessage());
//			sf.initCause(e);
//			throw sf;
//		}
		
		// NOTE: if session is null at this point, we are default to whatever CertificateManager has
		// which may not be the original user from the web

		return session;
	}
    
    private static void debugHeaders(HttpServletRequest request) {
		Enumeration<String> headers = request.getHeaderNames();
		while (headers.hasMoreElements()) {
			String name = (String) headers.nextElement();
			String value = request.getHeader(name);
			log.debug("Header: " + name + "=" + value);
			//System.out.println("Header: " + name + "=" + value);

		}
	}
    
    private String getResource(HttpServletRequest request) {
    	// get the resource
        String resource = request.getPathInfo();
        resource = resource.substring(resource.indexOf("/") + 1);
        return resource;
    }
    
    /**
     * Initalize servlet by setting logger
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /** Handle "GET" method requests from HTTP clients */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        log.debug("HTTP Verb: GET");
        String resource = this.getResource(request);
        
    	AnnotatorStore as = null;
		try {
			as = new JsonAnnotatorStore(getSession(request));
		} catch (BaseException e) {
			throw new ServletException(e);
		}

        
        if (resource.startsWith("annotations/")) {
        	String id = request.getPathInfo().substring(request.getPathInfo().lastIndexOf("/") + 1);
        	try {
				String result = as.read(id);
				IOUtils.write(result, response.getOutputStream());
				return;
			} catch (Exception e) {
				throw new ServletException(e);
			}
        	
        }
        
        // handle listing them
        if (resource.startsWith("annotations")) {
        	try {
				String result = as.index();
				IOUtils.write(result, response.getOutputStream());
				return;
			} catch (Exception e) {
				throw new ServletException(e);
			}
        	
        }
        
        // handle searching them
        if (resource.startsWith("search")) {
        	String query = request.getQueryString();
        	try {
				String result = as.search(query);
				IOUtils.write(result, response.getOutputStream());
				return;
			} catch (Exception e) {
				e.printStackTrace();
				throw new ServletException(e);
			}
        	
        }
        
        // handle token request
        if (resource.startsWith("token")) {
        	String token = "";
        	// generate a token for this user based on information in the request
        	try {        	
        		X509Certificate certificate = PortalCertificateManager.getInstance().getCertificate(request);
        		if (certificate != null) {
	        		String userId = CertificateManager.getInstance().getSubjectDN(certificate);        		
	        		String fullName = null;
	        		SubjectInfo subjectInfo = CertificateManager.getInstance().getSubjectInfo(certificate);
	        		if (subjectInfo != null) {
	        			fullName = subjectInfo.getPerson(0).getFamilyName();
	        			if (subjectInfo.getPerson(0).getGivenNameList() != null && subjectInfo.getPerson(0).getGivenNameList().size() > 0) {
	        				fullName = subjectInfo.getPerson(0).getGivenName(0) + fullName;
	        			}
	        		}
	    			token = TokenGenerator.getJWT(userId, fullName);
	    			
	    			// make sure we keep the cookie on the reponse
	        		Cookie cookie = PortalCertificateManager.getInstance().getCookie(request);
	        		String identifier = cookie.getValue();
					PortalCertificateManager.getInstance().setCookie(identifier, response);

        		}
				response.getWriter().print(token);
				return;
			} catch (Exception e) {
				e.printStackTrace();
				throw new ServletException(e);
			}
        	
        }
        
        
    }

    /** Handle "POST" method requests from HTTP clients */
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        log.debug("HTTP Verb: POST");

        AnnotatorStore as = null;
		try {
			as = new JsonAnnotatorStore(getSession(request));
		} catch (BaseException e) {
			throw new ServletException(e);
		}
		
        String resource = this.getResource(request);
        if (resource.equals("annotations")) {
        	try {
        		// get the annotation from the request
        		InputStream is = request.getInputStream();
    			
        		String annotationContent = IOUtils.toString(is, "UTF-8");
        		
    			// create it on the node
				String id = as.create(annotationContent);
				
				// TODO: determine which is the correct approach for responding to CREATE
				
				// redirect to read?
				// see: http://docs.annotatorjs.org/en/v1.2.x/storage.html
				boolean redirect = false;
				if (redirect) {
					response.setStatus(303);
					response.sendRedirect(request.getRequestURI() + "/" + id);
				} else {
					response.setStatus(200);
					// write it back in the response
					annotationContent = as.read(id);
					IOUtils.write(annotationContent, response.getOutputStream());
				}
				
			} catch (Exception e) {
				throw new ServletException(e);
			}
        	
        }
    }

    /** Handle "DELETE" method requests from HTTP clients */
    @Override
    protected void doDelete(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        log.debug("HTTP Verb: DELETE");
        
        AnnotatorStore as = null;
		try {
			as = new JsonAnnotatorStore(getSession(request));
		} catch (BaseException e) {
			throw new ServletException(e);
		}
        
        String resource = this.getResource(request);
        if (resource.startsWith("annotations/")) {
        	String id = request.getPathInfo().substring(request.getPathInfo().lastIndexOf("/") + 1);
        	try {
        		// delete the annotation
        		as.delete(id);
        		
        		// say no content
        		response.setContentLength(0);
				response.setStatus(204);
				
			} catch (Exception e) {
				throw new ServletException(e);
			}
        	
        }
        
    }

    /** Handle "PUT" method requests from HTTP clients */
    @Override
    protected void doPut(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        log.debug("HTTP Verb: PUT");
        
        AnnotatorStore as = null;
		try {
			as = new JsonAnnotatorStore(getSession(request));
		} catch (BaseException e) {
			throw new ServletException(e);
		}
		
        String resource = this.getResource(request);
        if (resource.startsWith("annotations/")) {
        	try {
        		
            	String id = request.getPathInfo().substring(request.getPathInfo().lastIndexOf("/") + 1);

        		// get the annotation from the request
        		InputStream is = request.getInputStream();
    			
    			// update it on the node
				String result = as.update(id, IOUtils.toString(is, "UTF-8"));
								
				// redirect to read?
				// see: http://docs.annotatorjs.org/en/v1.2.x/storage.html
				boolean redirect = false;
				if (redirect) {
					response.setStatus(303);
					response.sendRedirect(request.getRequestURI() + "/" + id);
				} else {
					response.setStatus(200);
					// write it back in the response
					IOUtils.write(result, response.getOutputStream());
				}
				
				
				
			} catch (Exception e) {
				throw new ServletException(e);
			}
        }
        	
        
    }

    /** Handle "HEAD" method requests from HTTP clients */
    @Override
    protected void doHead(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        log.debug("HTTP Verb: HEAD");
    }
}
