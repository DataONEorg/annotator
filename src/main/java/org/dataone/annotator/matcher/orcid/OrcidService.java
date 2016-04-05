package org.dataone.annotator.matcher.orcid;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.xpath.XPathAPI;
import org.dataone.annotator.matcher.ConceptItem;
import org.dataone.annotator.matcher.ConceptMatcher;
import org.dataone.annotator.matcher.QueryItem;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public

class OrcidService implements ConceptMatcher {
	
	private static Log log = LogFactory.getLog(OrcidService.class);
	
    //private static final String REST_URL = "http://pub.sandbox.orcid.org/v1.1/search/orcid-bio";
    private static final String REST_URL = "http://pub.orcid.org/v1.1/search/orcid-bio";

    @Override
    public List<ConceptItem> getConcepts(String text, String unit, String context) throws Exception {
    	return lookupOrcid(text, null, null, null);
    	
    }

	@Override
	public List<ConceptItem> getConcepts(Map<String, String> queryItems) throws Exception {
		StringBuffer sb = new StringBuffer();
		for (String value: queryItems.values()) {
			sb.append(value);
			sb.append(" ");
		}
		return lookupOrcid(sb.toString(), null, null, null);

	}
    /**
	 *
	 * Look up possible ORCID from orcid service.
	 * @see "http://support.orcid.org/knowledgebase/articles/132354-searching-with-the-public-api"
	 * @param surName
	 * @param givenNames
	 * @param otherNames
	 * @return
	 */
	public static List<ConceptItem> lookupOrcid(String text, String surName, List<String> givenNames, List<String> otherNames) {
		
		String url = null;

		List<ConceptItem> results = null;
		
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
			
			NodeList orcidSearchResultList = XPathAPI.selectNodeList(doc, "//*[local-name()=\"orcid-search-result\"]");
			if (orcidSearchResultList != null) {
				results = new ArrayList<ConceptItem>();
				
				for (int i = 0; i< orcidSearchResultList.getLength(); i++) {
					Node resultNode = orcidSearchResultList.item(i);
					Node score = XPathAPI.selectSingleNode(resultNode, "*[local-name()=\"relevancy-score\"]");
					Node orcidUriNode = XPathAPI.selectSingleNode(resultNode, "*[local-name()=\"orcid-profile\"]/*[local-name()=\"orcid-identifier\"]/*[local-name()=\"uri\"]");

					if (orcidUriNode == null) {
						log.warn("Skipping ORCID result - no identifier URI");
						continue;
					}
					
					String orcidUri = orcidUriNode.getFirstChild().getNodeValue();
					log.info("Found ORCID URI: " + orcidUri);
					
					double weight = Double.parseDouble(score.getFirstChild().getNodeValue());
					ConceptItem item = new ConceptItem(new URI(orcidUri), weight);
					
					results.add(item);
				}
				
			}
			
		} catch (Exception e) {
			log.error("Could not lookup ORCID using: " + url, e);
		}
		
		return results;
	}
	
}
