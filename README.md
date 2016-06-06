# esmapper

[![CircleCI](https://circleci.com/gh/tokuhirom/esmapper.svg?style=svg)](https://circleci.com/gh/tokuhirom/esmapper)

## SYNOPSIS

    Stream<EntryBean> beanStream = elasticsearchMapper.findAll(
        client.prepareSearch("blog")
            .setTypes("entry")
            .addSort("title", SortOrder.ASC),
        3,
        EntryBean.class
    );
    beanStream.map(EntryBean::getTitle).forEach(System.out::println);

## DESCRIPTION

Tiny elasticsearch mapper for Java. Mapping search results to Java objects.

## METHODS

### `Future<Long> count(SearchRequestBuilder searchRequestBuilder)`

        Future<Long> countFuture = elasticsearchMapper.count(
                client.prepareSearch("blog")
                        .setTypes("entry")
        );

Count search results.

This method automatically adds `searchRequestBuilder.setSize(0)`.

Returns the number of total hit rows.

### `public <T> Stream<T> findAll(SearchRequestBuilder searchRequestBuilder, int scrollSize, Class<T> klass) throws JsonParseException, JsonMappingException`

        Stream<EntryBean> beanStream = elasticsearchMapper.findAll(
                client.prepareSearch("blog")
                        .setTypes("entry")
                        .addSort("title", SortOrder.ASC),
                3,
                EntryBean.class
        );

 * _searchRequestBuilder_ is search query.
 * _scrollSize_ is scroll window size for each request.
 * _klass_ is a mapping class type.

Get all results from search query using scrolling.

Throws JsonParseException and JsonMappingException if there's JSON mapping exception.

Returns search result stream.

This method throws RuntimeException if
TODO: We shouldn't throw it.

## `public <T> Future<Page<T>> findPagination(int page, int entriesPerPage, SearchRequestBuilder searchRequestBuilder, Class<T> klass)`

    Page<EntryBean> page = elasticsearchMapper.findPagination(
        1,
        3,
        client.prepareSearch("blog")
              .setTypes("entry")
              .addSort("title", SortOrder.ASC),
        EntryBean.class
    ).get();

Find results with pagination.

 * _page_ is current page number.
 * _entriesPerPage_ is the number of entries per page.
 * _searchRequestBuilder_ is a search request.
 * _klass_ is a mapping class type.

Returns search results. `Page<T>` object contains _total number of hits_ and _rows_ information.

## `public <T> Future<LoadMore<T>> findLoadMore(int entriesPerPage, SearchRequestBuilder searchRequestBuilder, Class<T> klass)`

    LoadMore<EntryBean> page = elasticsearchMapper.findLoadMore(
        3,
        client.prepareSearch("blog")
            .setTypes("entry")
            .addSort("title", SortOrder.ASC)
            .setQuery(QueryBuilders.rangeQuery("i").gt(2)),
        EntryBean.class
    ).get();

Find rows for "Load more" style navigation.

  * _entriesPerPage_ is the number of rows in per page.
  * _searchRequestBuilder_ is query object.
  * _klass_ is mapping type.

Returns search results. You can call `hasNext()` and `getRows()` method via page object.

## `public <T> Future<Optional<T>> findFirst(SearchRequestBuilder searchRequestBuilder, Class<T> klass)`

    Future<Optional<EntryBean>> optionalFuture = elasticsearchMapper.findFirst(
        client.prepareSearch("blog")
            .setTypes("entry")
            .setQuery(QueryBuilders.termQuery("_id", indexResponses.get(4).getId())),
        EntryBean.class
    );

Find first element and map to Optional.

  * _searchRequestBuilder_ is query object.
  * _klass_ is mapping type.

## SEE ALSO

 * [spring-boot-data-elasticsearch](http://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/)
