package org.dataone.annotator.matcher;

import java.util.List;

/**
 * Created by xixiluo on 1/12/15.
 */
public class QueryItem {
    protected List<KeywordItem> keywordItemList;

    public QueryItem(final String text) {
        String[] keywords = text.split(",");
        if (keywords.length < 1) {
            throw new IllegalArgumentException("empty keyword list");
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
