package org.dataone.annotator.matcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by xixiluo on 1/14/15.
 */
public class QueryItemTest {

    /**
     * constructor for the test
     */
    public QueryItemTest() {
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * Release any objects after tests are complete
     */
    @After
    public void tearDown() {
    }


    @Test
    public void testQueryItem() {
        QueryItem test1 = new QueryItem("snow(water;something),  depth  ");
        assertEquals("snow(water), depth", "snow", test1.getKeywordItemList().get(0).getKeyword());
        assertEquals("snow(water), depth", "depth", test1.getKeywordItemList().get(1).getKeyword());
        assertEquals("snow(water), depth", "water", test1.getKeywordItemList().get(0).getTypes().get(0));
        assertEquals("snow(water), depth", "something", test1.getKeywordItemList().get(0).getTypes().get(1));
        assertEquals("snow(water), depth", 2, test1.getKeywordItemList().get(0).getTypes().size());


    }

    @Test
    public void testQueryToString(){
        QueryItem test1 = new QueryItem("snow(water;something),depth(unit)");
        assertEquals("toString", "snow(water;something),depth(unit)", test1.toString());


    }

    @Test
    public void testStringToQuery(){

    }
}
