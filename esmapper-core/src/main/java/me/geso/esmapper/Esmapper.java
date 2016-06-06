package me.geso.esmapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.geso.esmapper.annotation.Id;
import me.geso.esmapper.annotation.Score;
import me.geso.esmapper.exception.EsmapperJsonMappingException;
import me.geso.esmapper.exception.EsmapperRuntimeException;
import me.geso.esmapper.future.CountFuture;
import me.geso.esmapper.future.FindFirstFuture;
import me.geso.esmapper.future.LoadMoreFuture;
import me.geso.esmapper.future.PageFuture;
import me.geso.esmapper.iterator.SearchIterator;
import me.geso.esmapper.pager.LoadMore;
import me.geso.esmapper.pager.Page;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Esmapper {
    private final ObjectMapper objectMapper;

    public Esmapper() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Future<Long> count(SearchRequestBuilder searchRequestBuilder) {
        ListenableActionFuture<SearchResponse> actionFuture = searchRequestBuilder
                .setSize(0)
                .execute();
        return new CountFuture(actionFuture);
    }

    public <T> Stream<T> findAll(SearchRequestBuilder searchRequestBuilder, int scrollSize, Class<T> klass) {
        Iterator<T> iterator = new SearchIterator<>(this, searchRequestBuilder, scrollSize, klass);
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public <T> Future<Page<T>> findPagination(int page, int entriesPerPage, SearchRequestBuilder searchRequestBuilder, Class<T> klass) {
        ListenableActionFuture<SearchResponse> future = searchRequestBuilder
                .setSize(entriesPerPage)
                .setFrom((page - 1) * entriesPerPage)
                .execute();

        return new PageFuture<>(this, future, klass, page, entriesPerPage);
    }

    public <T> Future<LoadMore<T>> findLoadMore(int entriesPerPage, SearchRequestBuilder searchRequestBuilder, Class<T> klass) {
        ListenableActionFuture<SearchResponse> future = searchRequestBuilder
                .setSize(entriesPerPage + 1)
                .execute();

        return new LoadMoreFuture<>(this, future, entriesPerPage, klass);
    }

    public <T> Future<Optional<T>> findFirst(SearchRequestBuilder searchRequestBuilder, Class<T> klass) {
        ListenableActionFuture<SearchResponse> actionFuture = searchRequestBuilder
                .setSize(1)
                .execute();
        return new FindFirstFuture<>(this, actionFuture, klass);
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
