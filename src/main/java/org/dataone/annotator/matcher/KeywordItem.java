package org.dataone.annotator.matcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xixiluo on 1/12/15.
 */
public class KeywordItem {
    protected String keyword;
    protected List<String> types;

    public KeywordItem() {}

    public KeywordItem(String keyword) {
        this.keyword = keyword;
        types = new ArrayList<String>();
    }
    public KeywordItem(String keyword, List<String> types){
        this.keyword = keyword;
        this.types = types;
    }

    public void setKeyword(String keyword){
        this.keyword = keyword;
    }

    public String getKeyword(){
        return keyword;
    }

    public void setTypes(List<String> types){
        this.types = types;
    }

    public List<String> getTypes(){
        return types;
    }

    public void addType(String type){
        if(!types.contains(type)){
            types.add(type);
        }
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(keyword);

        boolean first = true;
        for(String type:types){
            if(first){
                sb.append("(");
                first = false;
            }else{
                sb.append(";");
            }

            sb.append(type);
        }

        if(!types.isEmpty()) sb.append(")");

        return sb.toString();
    }
}
