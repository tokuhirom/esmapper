package me.geso.esmapper.future;

import me.geso.esmapper.exception.EsmapperJsonMappingException;
import me.geso.esmapper.mapper.Mapper;
import me.geso.esmapper.pager.LoadMore;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LoadMoreFuture<T> implements Future<LoadMore<T>> {

    private final Mapper mapper;
    private final ListenableActionFuture<SearchResponse> future;
    private final int entriesPerPage;
    private final Class<T> klass;

    public LoadMoreFuture(Mapper mapper, ListenableActionFuture<SearchResponse> future, int entriesPerPage, Class<T> klass) {
        this.mapper = mapper;
        this.future = future;
        this.entriesPerPage = entriesPerPage;
        this.klass = klass;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public LoadMore<T> get() throws InterruptedException, ExecutionException {
        SearchResponse searchResponse = future.get();
        return handleResponse(searchResponse);
    }

    @Override
    public LoadMore<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        SearchResponse searchResponse = future.get(timeout, unit);
        return handleResponse(searchResponse);
    }

    private LoadMore<T> handleResponse(SearchResponse searchResponse) throws ExecutionException {
        SearchHit[] hits = searchResponse.getHits().getHits();
        ArrayList<T> objects = new ArrayList<>();
        boolean hasNext = false;
        for (SearchHit hit : hits) {
            if (objects.size() == entriesPerPage) {
                hasNext = true;
                break;
            }

            try {
                objects.add(mapper.inflateHit(hit, klass));
            } catch (EsmapperJsonMappingException e) {
                throw new ExecutionException(e);
            }
        }
        return new LoadMore<>(entriesPerPage, hasNext, searchResponse, objects);
    }
}
