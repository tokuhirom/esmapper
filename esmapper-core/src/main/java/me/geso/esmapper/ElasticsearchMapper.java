package me.geso.esmapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.geso.esmapper.entity.IdSettable;
import me.geso.esmapper.entity.ScoreSettable;
import me.geso.esmapper.pager.LoadMore;
import me.geso.esmapper.pager.Page;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ElasticsearchMapper {
    private final ObjectMapper objectMapper;

    public ElasticsearchMapper() {
        this.objectMapper = new ObjectMapper();
    }

    public Future<Long> count(SearchRequestBuilder searchRequestBuilder) {
        ListenableActionFuture<SearchResponse> actionFuture = searchRequestBuilder.setSize(0)
                .execute();
        return new Future<Long>() {
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
            public Long get() throws InterruptedException, ExecutionException {
                return actionFuture.get().getHits().totalHits();
            }

            @Override
            public Long get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return actionFuture.get(timeout, unit).getHits().totalHits();
            }
        };
    }

    public <T> Stream<T> findAll(SearchRequestBuilder searchRequestBuilder, int scrollSize, Class<T> klass) throws JsonParseException, JsonMappingException {
        Iterator<T> iterator = new Iterator<T>() {

            private int page = 0;
            private LinkedList<T> buffer = new LinkedList<>();

            @Override
            public boolean hasNext() {
                if (buffer.isEmpty()) {
                    fill();
                }
                return !buffer.isEmpty();
            }

            private void fill() {
                SearchResponse searchResponse = searchRequestBuilder
                        .setSize(scrollSize)
                        .setFrom(page * scrollSize)
                        .get();

                for (SearchHit hit : searchResponse.getHits()) {
                    try {
                        buffer.addLast(inflateHit(hit, klass));
                    } catch (JsonParseException | JsonMappingException e) {
                        throw new RuntimeException(e);
                    }
                }

                page++;
                if (searchResponse.getHits().getHits().length == 0) {
                }
            }

            @Override
            public T next() {
                return buffer.removeFirst();
            }
        };
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public <T> Future<Page<T>> findPagination(int page, int entriesPerPage, SearchRequestBuilder searchRequestBuilder, Class<T> klass) {
        ListenableActionFuture<SearchResponse> future = searchRequestBuilder
                .setSize(entriesPerPage)
                .setFrom((page - 1) * entriesPerPage)
                .execute();

        return new Future<Page<T>>() {

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
                        objects.add(inflateHit(hit, klass));
                    } catch (JsonParseException | JsonMappingException e) {
                        throw new ExecutionException(e);
                    }
                }
                return new Page<T>(page, entriesPerPage, searchResponse, objects);
            }
        };
    }

    public <T> Future<LoadMore<T>> findLoadMore(int entriesPerPage, SearchRequestBuilder searchRequestBuilder, Class<T> klass) {
        ListenableActionFuture<SearchResponse> future = searchRequestBuilder
                .setSize(entriesPerPage + 1)
                .execute();

        return new Future<LoadMore<T>>() {

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
                        objects.add(inflateHit(hit, klass));
                    } catch (JsonParseException | JsonMappingException e) {
                        throw new ExecutionException(e);
                    }
                }
                return new LoadMore<>(entriesPerPage, hasNext, searchResponse, objects);
            }
        };
    }

    private <T> T inflateHit(SearchHit hit, Class<? extends T> klass) throws JsonParseException, JsonMappingException {
        try {
            T bean = objectMapper.readValue(hit.source(), klass);
            if (bean instanceof IdSettable) {
                ((IdSettable) bean).setId(hit.getId());
            }
            if (bean instanceof ScoreSettable) {
                ((ScoreSettable) bean).setScore(hit.score());
            }
            return bean;
        } catch (JsonParseException | JsonMappingException e) {
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public <T> Future<Optional<T>> findFirst(SearchRequestBuilder searchRequestBuilder, Class<T> klass) {
        ListenableActionFuture<SearchResponse> actionFuture = searchRequestBuilder
                .setSize(1)
                .execute();
        return new Future<Optional<T>>() {
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
                        return Optional.of(inflateHit(hits[0], klass));
                    } catch (JsonParseException | JsonMappingException e) {
                        throw new ExecutionException(e);
                    }
                } else {
                    return Optional.empty();
                }
            }
        };
    }
}
