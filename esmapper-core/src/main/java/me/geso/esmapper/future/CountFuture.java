package me.geso.esmapper.future;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CountFuture implements Future<Long> {
    private final ListenableActionFuture<SearchResponse> actionFuture;

    public CountFuture(ListenableActionFuture<SearchResponse> actionFuture) {
        this.actionFuture = actionFuture;
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
    public Long get() throws InterruptedException, ExecutionException {
        return actionFuture.get().getHits().totalHits();
    }

    @Override
    public Long get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return actionFuture.get(timeout, unit).getHits().totalHits();
    }
}
