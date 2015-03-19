package org.dataone.annotator.matcher.esor;

import org.dataone.annotator.matcher.ConceptItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xixiluo on 2/16/15.
 */
public class EsorClient {

    public static void main(String[] args) throws Exception{
        EsorService esorS = new EsorService();

        List<ConceptItem> res = esorS.getConcepts("Litterfall");
        //List<ConceptItem> res = esorS.getConcepts("carbon%20mass");
        //List<ConceptItem> res = esorS.getConcepts("carbon,mass");


        for(int i = 0 ; i < res.size(); i++){
            ConceptItem c = res.get(i);
            System.out.println(c.getUri());
            System.out.println(c.getWeight());
        }
    }
}
