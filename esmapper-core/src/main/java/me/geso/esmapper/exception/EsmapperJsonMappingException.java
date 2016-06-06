package me.geso.esmapper.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

public class EsmapperJsonMappingException extends EsmapperException {
    private final SearchRequestBuilder searchRequestBuilder;
    private final SearchResponse searchResponse;
    private final SearchHit hit;
    private final Class<?> klass;

    public EsmapperJsonMappingException(JsonMappingException e, SearchRequestBuilder searchRequestBuilder, SearchResponse searchResponse, SearchHit hit, Class<?> klass) {
        super("Cannot mapping JSON: " + e.getMessage() + " Query:" + searchRequestBuilder + " class:" + klass, e);
        this.searchRequestBuilder = searchRequestBuilder;
        this.searchResponse = searchResponse;
        this.hit = hit;
        this.klass = klass;
    }

    public SearchResponse getSearchResponse() {
        return searchResponse;
    }

    public SearchHit getHit() {
        return hit;
    }

    public Class<?> getKlass() {
        return klass;
    }

    public SearchRequestBuilder getSearchRequestBuilder() {

        return searchRequestBuilder;
    }
}
