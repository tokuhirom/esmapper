package me.geso.esmapper.pager;

import org.elasticsearch.action.search.SearchResponse;

import java.util.Collections;
import java.util.List;

public class Page<T> {
    private final int currentPage;

    private final int entriesPerPage;
    private final SearchResponse searchResponse;
    private final List<T> rows;

    public Page(int currentPage, int entriesPerPage, SearchResponse searchResponse, List<T> rows) {
        this.currentPage = currentPage;
        this.entriesPerPage = entriesPerPage;
        this.searchResponse = searchResponse;
        this.rows = rows;
    }

    public long getTotalHits() {
        return searchResponse.getHits().getTotalHits();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getEntriesPerPage() {
        return entriesPerPage;
    }

    public List<T> getRows() {
        return Collections.unmodifiableList(rows);
    }
}
