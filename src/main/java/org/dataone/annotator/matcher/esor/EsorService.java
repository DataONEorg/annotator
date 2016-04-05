package org.dataone.annotator.matcher.esor;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.dataone.annotator.matcher.ConceptItem;
import org.dataone.annotator.matcher.ConceptMatcher;
import org.dataone.annotator.ontology.MeasurementTypeGenerator;
import org.json.JSONArray;
import org.json.JSONObject;

public class EsorService implements ConceptMatcher {

	private static Log log = LogFactory.getLog(EsorService.class);

	//private static final String REST_URL = "http://127.0.0.1:9100/search";

	//temporary server for testing
	private static final String REST_URL = "https://esor.tw.rpi.edu/annotate/annotate";

	private MeasurementTypeGenerator mtg = null;
	
	public EsorService() {
    	mtg  = new MeasurementTypeGenerator();
	}


	@Override
	public List<ConceptItem> getConcepts(String fullText, String unit, String context) throws Exception {
		//merge two results with different escape condition
		String query = parseTerm(fullText);
		String escapedSpaceQuery = escapeToSpace(query);
		String escapedCommaQuery = escapeToComma(query);

		if(true){
			return lookupEsor(escapedSpaceQuery, unit, context);
		}else{
			List<ConceptItem> res_escapedSpace = lookupEsor(escapedSpaceQuery, unit, context);
			List<ConceptItem> res_escapedComma = lookupEsor(escapedCommaQuery, unit, context);
			return mergeRes(res_escapedSpace, res_escapedComma);
		}

		//return lookupEsor(fullText);
	}

	@Override
	public List<ConceptItem> getConcepts(Map<String, String> queryItems) throws Exception {
		StringBuffer sb = new StringBuffer();
		for (String value: queryItems.values()) {
			sb.append(value);
			sb.append(" ");
		}
		//return lookupEsor(sb.toString());
		return getConcepts(sb.toString(), null, null);
	}

	private List<ConceptItem> lookupEsor(String query, String unit, String context) throws Exception  {

		HttpClient client = HttpClients.createDefault();
		// remove quotes for now
		// see: https://github.com/DataONEorg/sem-prov-design/issues/134
		//query = query.replaceAll("\"", "");
		//String uriStr = REST_URL + "?query=" + URLEncoder.encode(query, "UTF-8");
		String uriStr = REST_URL;
		//uriStr += "?minScore=2&numResult=10";
		//uriStr +=  "?query=" + query;
		log.debug("uriStr=" + uriStr);

		// use post to handle potentially long parameter values
		HttpPost method = new HttpPost(uriStr);
		method.setHeader("Accept", "*/*");
		JSONObject jsonParams = new JSONObject();
	    jsonParams.put("query", query);
	    if (context != null) {
		    jsonParams.put("context", context);
	    }
	    if (unit != null) {
		    jsonParams.put("unit", unit);
	    }

	    StringEntity entity = new StringEntity(jsonParams.toString(), ContentType.APPLICATION_JSON);

		method.setEntity(entity);
		HttpResponse response = client.execute(method);
		int code = response.getStatusLine().getStatusCode();
		if (2 != code / 100) {
				throw new Exception("response code " + code + " for resource at " + uriStr);
			}
		InputStream body = response.getEntity().getContent();
		String jsonStr = IOUtils.toString(body, "UTF-8");
		log.debug("jsonStr=" + jsonStr);

		JSONObject json = new JSONObject(jsonStr);
		//String query = json.getString("query");
		JSONArray results = json.getJSONArray("results");

		//analysis the result and return
		ArrayList<ConceptItem> concepts = new ArrayList<ConceptItem>();
		for (int i = 0; i < results.length(); i++){
			JSONObject r = results.getJSONObject(i);
			JSONArray annotations = r.getJSONArray("annotations");
			for (int j = 0; j < annotations.length(); j++) {
				JSONObject a = annotations.getJSONObject(j);
				String url = a.getString("url");
				String score = a.getString("score");
				System.out.println("url=" + url + ", score=" + score);

				
				//returned result may be empty
				if (url.length() > 0) {
					// check that it is a subclass of our measurementType
					boolean isSubclass = mtg.isMeasurementTypeSubclass(url);
					if (isSubclass) {
						ConceptItem c = new ConceptItem(new URI(url), Double.parseDouble(score));
						concepts.add(c);
					}
				} else{
					//System.out.println("NA");
				}
			}

		}
		
		// sort them by score
		Collections.sort(concepts, new Comparator<ConceptItem>() {

			@Override
			public int compare(ConceptItem o1, ConceptItem o2) {
				// compare the weights
				return Double.compare(o1.getWeight(), o2.getWeight());
			}
			
		});
		// descending
		Collections.reverse(concepts);

		return concepts;
	}


	//escape input query
	private String parseTerm(String str) throws Exception{

		if(str.contains("(")){
			str =  str.substring(0, str.indexOf("("));
		}

		str = str.replaceAll("\\s+$", "");

		str = replaceFromSlashToSpace(replaceFromDotToSpace(replaceFromDashToSpace(str)));
		str = str.replace("%", " percent");
		str = insertSpaceBeforeCapital(str);
		str = URLEncoder.encode(str, "UTF-8").replaceAll("\\+", "%20");
		return str;
	}

	private String replaceFromDotToSpace(String str) {
		return str.replace(".", " ");
	}

	private String replaceFromSlashToSpace(String str){
		return str.replace("/", " ");
	}

	private String insertSpaceBeforeCapital(String str){
		char[] charArr = str.toCharArray();
		StringBuilder sb = new StringBuilder();
		for(int i = 0 ; i < charArr.length; i++){
			if(i>0 && charArr[i] >= 'A' && charArr[i] <= 'Z' && charArr[i-1] >= 'a'&& charArr[i-1] <= 'z')
				sb.append(" ");
			sb.append(charArr[i]);
		}
		return sb.toString();
	}

	private String replaceFromDashToSpace(String original){
		return original.replace("_", " ");
	}

	private String escapeToSpace(String original){
		return original.replace(" ", "%20");
	}

	private String escapeToComma(String original){
		return original.replace("%20", ",");
	}

	private List<ConceptItem> mergeRes(List<ConceptItem> res_escapedSpace, List<ConceptItem> res_escapedComma) {
		if(res_escapedSpace.size()==0) return res_escapedComma;
		if(res_escapedComma.size()==0) return res_escapedSpace;

		int indexS = 0;
		int indexC = 0;
		while(indexS < res_escapedSpace.size()){
			if(indexC < res_escapedComma.size() && res_escapedComma.get(indexC).getWeight() >= res_escapedSpace.get(indexS).getWeight()){
				res_escapedSpace.add(indexS, res_escapedComma.get(indexC));
				indexS++;
				indexC++;
			}else{
				indexS++;
			}
		}

		for(int i = indexC; i < res_escapedComma.size(); i++ ){
			res_escapedSpace.add(res_escapedComma.get(i));
		}

		return res_escapedSpace;
	}
}
