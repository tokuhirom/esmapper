package me.geso.esmapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.geso.esmapper.entity.IdSettable;
import me.geso.esmapper.entity.ScoreSettable;
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
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ElasticsearchMapper {
    private final ObjectMapper objectMapper;

    public ElasticsearchMapper() {
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
        try {
            T bean = objectMapper.readValue(hit.source(), klass);
            if (bean instanceof IdSettable) {
                ((IdSettable) bean).setId(hit.getId());
            }
            if (bean instanceof ScoreSettable) {
                ((ScoreSettable) bean).setScore(hit.score());
            }
            return bean;
        } catch (JsonMappingException e) {
            throw new EsmapperJsonMappingException(e, hit, klass);
        } catch (IOException e) {
            // Should not reach here
            throw new EsmapperRuntimeException(e);
        }
    }

}
