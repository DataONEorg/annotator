package org.dataone.annotator.matcher.orcid;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.xpath.XPathAPI;
import org.dataone.annotator.matcher.ConceptMatcher;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class OrcidService implements ConceptMatcher {
	
	private static Log log = LogFactory.getLog(OrcidService.class);
	
    //private static final String REST_URL = "http://pub.sandbox.orcid.org/v1.1/search/orcid-bio";
    private static final String REST_URL = "http://pub.orcid.org/v1.1/search/orcid-bio";

    @Override
    public List<String> getConcepts(String text) {
    	return lookupOrcid(text, null, null, null);
    	
    }
    
    /**
	 * Look up possible ORCID from orcid service.
	 * @see "http://support.orcid.org/knowledgebase/articles/132354-searching-with-the-public-api"
	 * @param surName
	 * @param givenNames
	 * @param otherNames
	 * @return
	 */
	public static List<String> lookupOrcid(String text, String surName, List<String> givenNames, List<String> otherNames) {
		
		String url = null;

		List<String> results = null;
		
		try {
			
			String urlParameters = "";
			
			if (text != null) {
				urlParameters += "\"" + text + "\""; 
			} else {
				if (surName != null) {
					urlParameters += "+family-name:\"" + surName + "\"";
				}
				if (otherNames != null) {
					for (String otherName: otherNames) {
						urlParameters += "+other-names:\"" + otherName + "\""; 
					}
				}
				if (givenNames != null) {
					for (String givenName: givenNames) {
						urlParameters += "+given-names:\"" + givenName + "\""; 
					}
				}
			}
			
			urlParameters = URLEncoder.encode(urlParameters, "UTF-8");
			
			url = REST_URL + "?q=" + urlParameters + "&rows=1";
			HttpClient client = new DefaultHttpClient();
			HttpGet method = new HttpGet(url);
			method.addHeader("Accept", "application/orcid+xml");
			HttpResponse response = client.execute(method);
			InputStream is = response.getEntity().getContent();
			
			Node doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList orcidUriNodeList = XPathAPI.selectNodeList(doc, "//*[local-name()=\"orcid-identifier\"]/*[local-name()=\"uri\"]");
			if (orcidUriNodeList != null) {
				results = new ArrayList<String>();
				for (int i = 0; i< orcidUriNodeList.getLength(); i++) {
					Node n = orcidUriNodeList.item(i);
					String orcidUri = n.getFirstChild().getNodeValue();
					log.info("Found ORCID URI: " + orcidUri);
					results.add(orcidUri);
				}
				
			}
			
		} catch (Exception e) {
			log.error("Could not lookup ORCID using: " + url, e);
		}
		
		return results;
	}
	
}
