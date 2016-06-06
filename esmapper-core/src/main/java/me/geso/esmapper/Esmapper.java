package me.geso.esmapper;

import me.geso.esmapper.future.CountFuture;
import me.geso.esmapper.future.FindFirstFuture;
import me.geso.esmapper.future.LoadMoreFuture;
import me.geso.esmapper.future.PageFuture;
import me.geso.esmapper.iterator.SearchIterator;
import me.geso.esmapper.mapper.Mapper;
import me.geso.esmapper.pager.LoadMore;
import me.geso.esmapper.pager.Page;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Esmapper {
    private final Mapper mapper;

    public Esmapper() {
        this.mapper = new Mapper();
    }

    public Future<Long> count(SearchRequestBuilder searchRequestBuilder) {
        ListenableActionFuture<SearchResponse> actionFuture = searchRequestBuilder
                .setSize(0)
                .execute();
        return new CountFuture(actionFuture);
    }

    public <T> Stream<T> findAll(SearchRequestBuilder searchRequestBuilder, int scrollSize, Class<T> klass) {
        Iterator<T> iterator = new SearchIterator<>(mapper, searchRequestBuilder, scrollSize, klass);
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public <T> Future<Page<T>> findPagination(int page, int entriesPerPage, SearchRequestBuilder searchRequestBuilder, Class<T> klass) {
        ListenableActionFuture<SearchResponse> future = searchRequestBuilder
                .setSize(entriesPerPage)
                .setFrom((page - 1) * entriesPerPage)
                .execute();

        return new PageFuture<>(mapper, future, klass, page, entriesPerPage);
    }

    public <T> Future<LoadMore<T>> findLoadMore(int entriesPerPage, SearchRequestBuilder searchRequestBuilder, Class<T> klass) {
        ListenableActionFuture<SearchResponse> future = searchRequestBuilder
                .setSize(entriesPerPage + 1)
                .execute();

        return new LoadMoreFuture<>(mapper, future, entriesPerPage, klass);
    }

    public <T> Future<Optional<T>> findFirst(SearchRequestBuilder searchRequestBuilder, Class<T> klass) {
        ListenableActionFuture<SearchResponse> actionFuture = searchRequestBuilder
                .setSize(1)
                .execute();
        return new FindFirstFuture<>(mapper, actionFuture, klass);
    }


}
