package me.geso.esmapper;

import com.google.common.io.Files;
import me.geso.esmapper.pager.LoadMore;
import me.geso.esmapper.pager.Page;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


// Note. I want to use ESIntegTest... But it causes jar hell. I can't resolve that.
// As a result, I wrote test suite by myself.
public class ElasticsearchMapperTest {
    private Client client;
    private Node node;

    @Before
    public void beforeTest() throws Exception {
        File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();

        Settings settings = Settings.builder()
                .put("path.home", tempDir.toString())
                .build();
        this.node = NodeBuilder.nodeBuilder()
                .settings(settings)
                .node();
        this.client = node.client();
    }


    @After
    public void teardown() {
        if (this.client != null) {
            this.client.close();
        }
        if (node != null) {
            this.node.close();
        }
    }

    @Test
    public void count() throws Exception {
        IndexResponse indexResponse = this.client.prepareIndex("blog", "entry")
                .setSource("title", "bbb")
                .get();
        assertThat(indexResponse.isCreated(), is(true));

        client.admin().indices().prepareRefresh()
                .get();

        ElasticsearchMapper elasticsearchMapper = new ElasticsearchMapper();
        Future<Long> countFuture = elasticsearchMapper.count(
                client.prepareSearch("blog")
                        .setTypes("entry")
                        .setSize(0)
        );
        assertThat(countFuture.get(), is(1L));
    }

    @Test
    public void findAll() throws Exception {
        for (int i = 0; i < 10; ++i) {
            IndexResponse indexResponse = this.client.prepareIndex("blog", "entry")
                    .setSource("title", "bbb " + i)
                    .get();
            assertThat(indexResponse.isCreated(), is(true));
        }

        client.admin().indices().prepareRefresh()
                .get();

        ElasticsearchMapper elasticsearchMapper = new ElasticsearchMapper();
        Stream<EntryBean> beanStream = elasticsearchMapper.findAll(
                client.prepareSearch("blog")
                        .setTypes("entry")
                        .addSort("title", SortOrder.ASC),
                3,
                EntryBean.class
        );
        List<EntryBean> beans = beanStream.collect(Collectors.toList());
        assertThat(beans.size(), is(10));
        assertThat(beans.get(0).getTitle(), is("bbb 0"));
        assertThat(beans.get(0).getId(), is(notNullValue()));
        assertThat(beans.get(9).getTitle(), is("bbb 9"));
    }

    @Test
    public void findPagination() throws Exception {
        for (int i = 0; i < 5; ++i) {
            IndexResponse indexResponse = this.client.prepareIndex("blog", "entry")
                    .setSource("title", "bbb " + i)
                    .get();
            assertThat(indexResponse.isCreated(), is(true));
        }

        client.admin().indices().prepareRefresh()
                .get();

        ElasticsearchMapper elasticsearchMapper = new ElasticsearchMapper();
        // page 1
        {
            Page<EntryBean> page = elasticsearchMapper.findPagination(
                    1,
                    3,
                    client.prepareSearch("blog")
                            .setTypes("entry")
                            .addSort("title", SortOrder.ASC),
                    EntryBean.class
            ).get();
            assertThat(page.getRows().size(), is(3));
            assertThat(page.getTotalHits(), is(5L));
            assertThat(page.getCurrentPage(), is(1));
            assertThat(page.getEntriesPerPage(), is(3));
            assertThat(page.getRows()
                    .stream()
                    .map(EntryBean::getTitle)
                    .collect(Collectors.joining(",")), is("bbb 0,bbb 1,bbb 2"));
        }
        // page 2
        {
            Page<EntryBean> page = elasticsearchMapper.findPagination(
                    2,
                    3,
                    client.prepareSearch("blog")
                            .setTypes("entry")
                            .addSort("title", SortOrder.ASC),
                    EntryBean.class
            ).get();
            assertThat(page.getRows().size(), is(2));
            assertThat(page.getTotalHits(), is(5L));
            assertThat(page.getCurrentPage(), is(2));
            assertThat(page.getEntriesPerPage(), is(3));
            assertThat(page.getRows()
                    .stream()
                    .map(EntryBean::getTitle)
                    .collect(Collectors.joining(",")), is("bbb 3,bbb 4"));
        }
    }

    @Test
    public void findLoadMore() throws Exception {
        for (int i = 0; i < 5; ++i) {
            IndexResponse indexResponse = this.client.prepareIndex("blog", "entry")
                    .setSource(
                            "title", "bbb " + i,
                            "i", i
                    )
                    .get();
            assertThat(indexResponse.isCreated(), is(true));
        }

        client.admin().indices().prepareRefresh()
                .get();

        ElasticsearchMapper elasticsearchMapper = new ElasticsearchMapper();
        // page 1
        {
            LoadMore<EntryBean> page = elasticsearchMapper.findLoadMore(
                    3,
                    client.prepareSearch("blog")
                            .setTypes("entry")
                            .addSort("title", SortOrder.ASC),
                    EntryBean.class
            ).get();
            assertThat(page.getRows().size(), is(3));
            assertThat(page.hasNext(), is(true));
            assertThat(page.getEntriesPerPage(), is(3));
            assertThat(page.getRows()
                    .stream()
                    .map(EntryBean::getTitle)
                    .collect(Collectors.joining(",")), is("bbb 0,bbb 1,bbb 2"));
        }
        // page 2
        {
            LoadMore<EntryBean> page = elasticsearchMapper.findLoadMore(
                    3,
                    client.prepareSearch("blog")
                            .setTypes("entry")
                            .addSort("title", SortOrder.ASC)
                            .setQuery(QueryBuilders.rangeQuery("i")
                                    .gt(2)),
                    EntryBean.class
            ).get();
            assertThat(page.getRows().size(), is(2));
            assertThat(page.hasNext(), is(false));
            assertThat(page.getEntriesPerPage(), is(3));
            assertThat(page.getRows()
                    .stream()
                    .map(EntryBean::getTitle)
                    .collect(Collectors.joining(",")), is("bbb 3,bbb 4"));
        }
    }

    @Test
    public void findFirst() throws Exception {
        ArrayList<IndexResponse> indexResponses = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            IndexResponse indexResponse = this.client.prepareIndex("blog", "entry")
                    .setSource("title", "bbb " + i)
                    .get();
            assertThat(indexResponse.isCreated(), is(true));
            indexResponses.add(indexResponse);
        }

        client.admin().indices().prepareRefresh()
                .get();

        ElasticsearchMapper elasticsearchMapper = new ElasticsearchMapper();
        Future<Optional<EntryBean>> optionalFuture = elasticsearchMapper.findFirst(
                client.prepareSearch("blog")
                        .setTypes("entry")
                        .setQuery(QueryBuilders.termQuery("_id", indexResponses.get(4).getId())),
                EntryBean.class
        );
        Optional<EntryBean> entryBeanOptional = optionalFuture.get();
        assertThat(entryBeanOptional.isPresent(), is(true));
        assertThat(entryBeanOptional.get().getTitle(), is("bbb 4"));
        assertThat(entryBeanOptional.get().getId(), is(notNullValue()));
        assertThat(entryBeanOptional.get().getScore(), is(1.0F));
    }

}