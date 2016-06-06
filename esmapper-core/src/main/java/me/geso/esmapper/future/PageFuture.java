package me.geso.esmapper.future;

import me.geso.esmapper.ElasticsearchMapper;
import me.geso.esmapper.exception.EsmapperJsonMappingException;
import me.geso.esmapper.pager.Page;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PageFuture<T> implements Future<Page<T>> {

    private final ElasticsearchMapper mapper;
    private final ListenableActionFuture<SearchResponse> future;
    private final Class<T> klass;
    private final int page;
    private final int entriesPerPage;

    public PageFuture(ElasticsearchMapper mapper, ListenableActionFuture<SearchResponse> future, Class<T> klass, int page, int entriesPerPage) {
        this.mapper = mapper;
        this.future = future;
        this.klass = klass;
        this.page = page;
        this.entriesPerPage = entriesPerPage;
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
    public Page<T> get() throws InterruptedException, ExecutionException {
        SearchResponse searchResponse = future.get();
        return handleResponse(searchResponse);
    }

    @Override
    public Page<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        SearchResponse searchResponse = future.get(timeout, unit);
        return handleResponse(searchResponse);
    }

    private Page<T> handleResponse(SearchResponse searchResponse) throws ExecutionException {
        SearchHit[] hits = searchResponse.getHits().getHits();
        ArrayList<T> objects = new ArrayList<>();
        for (SearchHit hit : hits) {
            try {
                objects.add(mapper.inflateHit(hit, klass));
            } catch (EsmapperJsonMappingException e) {
                throw new ExecutionException(e);
            }
        }
        return new Page<>(page, entriesPerPage, searchResponse, objects);
    }
}
