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
import java.util.concurrent.ConcurrentHashMap;

public class Mapper {
    private static final ConcurrentHashMap<Class<?>, ClassInfo> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public Mapper() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> T inflateHit(SearchHit hit, Class<? extends T> klass) throws EsmapperJsonMappingException {
        T bean = readJson(hit, klass);
        ClassInfo classInfo = cache.computeIfAbsent(klass, ClassInfo::build);
        if (classInfo.hasIdField()) {
            classInfo.setId(bean, hit.id());
        }
        if (classInfo.hasScoreField()) {
            classInfo.setScore(bean, hit.score());
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

    private static class ClassInfo {
        private Field idField;
        private Field scoreField;

        public ClassInfo(Field idField, Field scoreField) {
            this.idField = idField;
            this.scoreField = scoreField;
        }

        public boolean hasIdField() {
            return idField != null;
        }

        public boolean hasScoreField() {
            return scoreField != null;
        }

        public static ClassInfo build(Class<?> aClass) {
            Field idField = null;
            Field scoreField = null;
            for (Field field : aClass.getDeclaredFields()) {
                if (idField == null) {
                    Id id = field.getAnnotation(Id.class);
                    if (id != null) {
                        field.setAccessible(true);
                        if (!field.getType().isAssignableFrom(String.class)) {
                            throw new EsmapperRuntimeException("Id field should be String.");
                        }
                        idField = field;
                    }
                }

                if (scoreField == null) {
                    Score score = field.getAnnotation(Score.class);
                    if (score != null) {
                        field.setAccessible(true);
                        if (!field.getType().isAssignableFrom(float.class)) {
                            throw new EsmapperRuntimeException("Score field should be float.");
                        }
                        scoreField = field;
                    }
                }
            }
            return new ClassInfo(idField, scoreField);
        }

        public <T> void setId(T bean, String id) {
            try {
                idField.set(bean, id);
            } catch (IllegalAccessException e) {
                // Should not reach here. Because the type was already checked.
                throw new EsmapperRuntimeException(e);
            }
        }

        public <T> void setScore(T bean, float score) {
            try {
                scoreField.set(bean, score);
            } catch (IllegalAccessException e) {
                // Should not reach here. Because the type was already checked.
                throw new EsmapperRuntimeException(e);
            }
        }
    }
}
