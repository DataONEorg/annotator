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
import java.security.cert.X509Certificate;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.log4j.Logger;
import org.dataone.client.auth.CertificateManager;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.portal.TokenGenerator;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.SubjectInfo;

/**
 * Metacat REST handler for Annotator storage API
 * http://docs.annotatorjs.org/en/v1.2.x/storage.html#core-storage-api 
 *  
 */
public class AnnotatorRestServlet extends HttpServlet {

    protected Logger logMetacat;
    
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
        logMetacat = Logger.getLogger(this.getClass());
        super.init(config);
    }

    /** Handle "GET" method requests from HTTP clients */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        logMetacat.debug("HTTP Verb: GET");
        String resource = this.getResource(request);
        
    	AnnotatorStore as = null;
		try {
			as = new AnnotatorStore(request);
		} catch (BaseException e) {
			throw new ServletException(e);
		}

        
        if (resource.startsWith("annotations/")) {
        	String id = request.getPathInfo().substring(request.getPathInfo().lastIndexOf("/") + 1);
        	try {
				JSONObject annotation = as.read(id);
				annotation.writeJSONString(response.getWriter());
				return;
			} catch (Exception e) {
				throw new ServletException(e);
			}
        	
        }
        
        // handle listing them
        if (resource.startsWith("annotations")) {
        	try {
				JSONArray annotations = as.index();
				annotations.writeJSONString(response.getWriter());
				return;
			} catch (Exception e) {
				throw new ServletException(e);
			}
        	
        }
        
        // handle searching them
        if (resource.startsWith("search")) {
        	String query = request.getQueryString();
        	try {
				JSONObject results = as.search(query);
				results.writeJSONString(response.getWriter());
				return;
			} catch (Exception e) {
				e.printStackTrace();
				throw new ServletException(e);
			}
        	
        }
        
        // handle token request
        if (resource.startsWith("token")) {
        	// generate a token for this user based on information in the request
        	try {        	
        		X509Certificate certificate = PortalCertificateManager.getInstance().getCertificate(request);
        		String userId = CertificateManager.getInstance().getSubjectDN(certificate);        		
        		String fullName = null;
        		SubjectInfo subjectInfo = CertificateManager.getInstance().getSubjectInfo(certificate);
        		if (subjectInfo != null) {
        			fullName = subjectInfo.getPerson(0).getFamilyName();
        			if (subjectInfo.getPerson(0).getGivenNameList() != null && subjectInfo.getPerson(0).getGivenNameList().size() > 0) {
        				fullName = subjectInfo.getPerson(0).getGivenName(0) + fullName;
        			}
        		}
    			String token = TokenGenerator.getJWT(userId, fullName);
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
        logMetacat.debug("HTTP Verb: POST");

        AnnotatorStore as = null;
		try {
			as = new AnnotatorStore(request);
		} catch (BaseException e) {
			throw new ServletException(e);
		}
		
        String resource = this.getResource(request);
        if (resource.equals("annotations")) {
        	try {
        		// get the annotation from the request
        		InputStream is = request.getInputStream();
    			JSONObject annotation = (JSONObject) JSONValue.parse(is);
    			
    			// create it on the node
				String id = as.create(annotation);
				
				// TODO: determine which is the correct approach for responding to CREATE
				
				// redirect to read
				response.setStatus(303);
				response.sendRedirect(request.getRequestURI() + "/" + id);
				
				// write it back in the response
				JSONObject result = as.read(id);
				result.writeJSONString(response.getWriter());
				
			} catch (Exception e) {
				throw new ServletException(e);
			}
        	
        }
    }

    /** Handle "DELETE" method requests from HTTP clients */
    @Override
    protected void doDelete(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        logMetacat.debug("HTTP Verb: DELETE");
        
        AnnotatorStore as = null;
		try {
			as = new AnnotatorStore(request);
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
        logMetacat.debug("HTTP Verb: PUT");
    }

    /** Handle "HEAD" method requests from HTTP clients */
    @Override
    protected void doHead(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        logMetacat.debug("HTTP Verb: HEAD");
    }
}
