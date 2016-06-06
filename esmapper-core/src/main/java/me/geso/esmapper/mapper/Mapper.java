package me.geso.esmapper.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.geso.esmapper.annotation.Id;
import me.geso.esmapper.annotation.Score;
import me.geso.esmapper.exception.EsmapperJsonMappingException;
import me.geso.esmapper.exception.EsmapperRuntimeException;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.lang.reflect.Field;

public class Mapper {
    private final ObjectMapper objectMapper;

    public Mapper() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> T inflateHit(SearchHit hit, Class<? extends T> klass) throws EsmapperJsonMappingException {
        T bean = readJson(hit, klass);
        // TODO optimize reflection
        for (Field field : klass.getDeclaredFields()) {
            try {
                Id id = field.getAnnotation(Id.class);
                if (id != null) {
                    field.setAccessible(true);
                    field.set(bean, hit.getId());
                }

                Score score = field.getAnnotation(Score.class);
                if (score != null) {
                    field.setAccessible(true);
                    field.setFloat(bean, hit.getScore());
                }
            } catch (IllegalAccessException e) {
                throw new EsmapperRuntimeException(e);
            }
        }
        return bean;
    }

    private <T> T readJson(SearchHit hit, Class<T> klass) throws EsmapperJsonMappingException {
        try {
            return objectMapper.readValue(hit.source(), klass);
        } catch (JsonMappingException e) {
            throw new EsmapperJsonMappingException(e, hit, klass);
        } catch (IOException e) {
            // Should not reach here
            throw new EsmapperRuntimeException(e);
        }
    }
}
