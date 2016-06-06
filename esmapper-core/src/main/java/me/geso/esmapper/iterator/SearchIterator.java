package me.geso.esmapper.iterator;

import me.geso.esmapper.exception.EsmapperJsonMappingException;
import me.geso.esmapper.exception.EsmapperRuntimeException;
import me.geso.esmapper.mapper.Mapper;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class SearchIterator<T> implements Iterator<T> {

    private final Mapper mapper;
    private final SearchRequestBuilder searchRequestBuilder;
    private final int scrollSize;
    private final Class<T> klass;
    private int page;
    private final LinkedList<T> buffer;

    public SearchIterator(Mapper mapper, SearchRequestBuilder searchRequestBuilder, int scrollSize, Class<T> klass) {
        this.mapper = mapper;
        this.searchRequestBuilder = searchRequestBuilder;
        this.scrollSize = scrollSize;
        this.klass = klass;
        page = 0;
        buffer = new LinkedList<>();
    }

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
                buffer.addLast(mapper.inflateHit(hit, klass));
            } catch (EsmapperJsonMappingException e) {
                throw new EsmapperRuntimeException(e);
            }
        }

        page++;
    }

    @Override
    public T next() {
        if (buffer.isEmpty()) {
            fill();
        }
        if (buffer.isEmpty()) {
            throw new NoSuchElementException();
        }
        return buffer.removeFirst();
    }
}
