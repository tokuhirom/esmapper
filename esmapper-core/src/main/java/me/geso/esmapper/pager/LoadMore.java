package me.geso.esmapper.pager;

import org.elasticsearch.action.search.SearchResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadMore<T> {
    private final int entriesPerPage;
    private final boolean hasNext;
    private final List<T> rows;

    public LoadMore(int entriesPerPage, boolean hasNext, SearchResponse searchResponse, ArrayList<T> rows) {
        this.entriesPerPage = entriesPerPage;
        this.hasNext = hasNext;
        this.rows = rows;
    }

    public int getEntriesPerPage() {
        return entriesPerPage;
    }

    public boolean hasNext() {
        return hasNext;
    }

    public List<T> getRows() {
        return Collections.unmodifiableList(rows);
    }
}
