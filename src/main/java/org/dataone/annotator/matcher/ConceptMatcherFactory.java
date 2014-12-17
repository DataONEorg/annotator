package org.dataone.annotator.matcher;

import org.dataone.annotator.matcher.bioportal.BioPortalService;
import org.dataone.annotator.matcher.esor.EsorService;
import org.dataone.annotator.matcher.orcid.OrcidService;

public class ConceptMatcherFactory {

	public static final int BIOPORTAL = 1;
	public static final int ESOR = 2;
	public static final int ORCID = 3;
	
	/**
	 * Retrieve instance of a concept matching service implementation
	 * @param type
	 * @return
	 */
	public static ConceptMatcher getMatcher(int type) {
		ConceptMatcher matcher = null;
		
		switch (type) {
		case BIOPORTAL:
			matcher = new BioPortalService();
			break;
			
		case ESOR:
			// TODO: implement
			matcher = new EsorService();
			break;	

		case ORCID:
			matcher = new OrcidService();
			break;
		default:
			break;
		}
		return matcher;
	}
	
	/**
	 * For retrieving custom Concept matchers not currently included in the default factory
	 * @param className
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public static ConceptMatcher getMatcher(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		ConceptMatcher matcher = (ConceptMatcher) Class.forName(className).newInstance();

		return matcher;
		
	}
	
}
