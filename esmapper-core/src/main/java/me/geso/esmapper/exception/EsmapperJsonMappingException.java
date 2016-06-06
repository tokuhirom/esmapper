package me.geso.esmapper.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.elasticsearch.search.SearchHit;

public class EsmapperJsonMappingException extends EsmapperException {
    private final SearchHit hit;
    private final Class<?> klass;

    public EsmapperJsonMappingException(JsonMappingException e, SearchHit hit, Class<?> klass) {
        super("Cannot mapping JSON: " + e.getMessage() + " hit: " + hit + " class:" + klass, e);
        this.hit = hit;
        this.klass = klass;
    }
    
    public SearchHit getHit() {
        return hit;
    }

    public Class<?> getKlass() {
        return klass;
    }
}
