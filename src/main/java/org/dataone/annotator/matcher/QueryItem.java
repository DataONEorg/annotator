package org.dataone.annotator.matcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xixiluo on 1/12/15.
 */
public class QueryItem {
    protected List<KeywordItem> keywordItemList;

    public QueryItem(final String text) {
        String[] keywordArray = text.trim().toLowerCase().split(",");
        if (keywordArray.length < 1) {
            throw new IllegalArgumentException("empty keyword list");
        }

        keywordItemList = new ArrayList<KeywordItem>();
        for(String keyword:keywordArray){
            keyword = keyword.trim();
            if(!keyword.contains("(")){
                KeywordItem keywordItem = new KeywordItem(keyword);
                keywordItemList.add(keywordItem);
            } else{



                int indexOfLeftPre = keyword.indexOf("(");

                String nestedKeyword = keyword.substring(0, indexOfLeftPre);

                if(!keyword.contains(")")){
                    throw new IllegalArgumentException("missing )");
                }

                int indexOfRightPre = keyword.indexOf(")");

                if(indexOfRightPre < indexOfLeftPre){
                    throw new IllegalArgumentException(") is in front of (");
                }

                String nestedTypes = keyword.substring(indexOfLeftPre+1, indexOfRightPre);

                String[] typeArray = nestedTypes.trim().split(";");
                if(typeArray.length < 1){
                    throw new IllegalArgumentException("empty type list");
                }

                ArrayList<String> typeList = new ArrayList<String>();
                for(String type:typeArray){
                    typeList.add(type);
                }

                KeywordItem keywordItem = new KeywordItem(nestedKeyword, typeList);
                keywordItemList.add(keywordItem);

            }
        }

    }

    public QueryItem(List<KeywordItem> keywordItemList){
        this.keywordItemList = keywordItemList;
    }

    public List<KeywordItem> getKeywordItemList(){
        return this.keywordItemList;
    }

    public void setKeywordItemList(List<KeywordItem> keywordItemList){
        this.keywordItemList = keywordItemList;
    }

    public void addKeywordItem(KeywordItem keywordItem){
        keywordItemList.add(keywordItem);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for(KeywordItem keywordItem:keywordItemList){
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(keywordItem.toString());
        }

        return sb.toString();
    }
}
