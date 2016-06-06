package me.geso.esmapper.future;

import me.geso.esmapper.exception.EsmapperJsonMappingException;
import me.geso.esmapper.mapper.Mapper;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FindFirstFuture<T> implements Future<Optional<T>> {
    private final Mapper mapper;
    private final ListenableActionFuture<SearchResponse> actionFuture;
    private final Class<T> klass;

    public FindFirstFuture(Mapper mapper, ListenableActionFuture<SearchResponse> actionFuture, Class<T> klass) {
        this.mapper = mapper;
        this.actionFuture = actionFuture;
        this.klass = klass;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return actionFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return actionFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return actionFuture.isDone();
    }

    @Override
    public Optional<T> get() throws InterruptedException, ExecutionException {
        return handleResponse(actionFuture.get());
    }

    @Override
    public Optional<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return handleResponse(actionFuture.get(timeout, unit));
    }

    private Optional<T> handleResponse(SearchResponse searchResponse) throws ExecutionException {
        SearchHit[] hits = searchResponse.getHits().getHits();
        if (hits.length > 0) {
            try {
                return Optional.of(mapper.inflateHit(hits[0], klass));
            } catch (EsmapperJsonMappingException e) {
                throw new ExecutionException(e);
            }
        } else {
            return Optional.empty();
        }
    }
}
