package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.item.domain.po.ItemDoc;
import org.apache.http.HttpHost;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.matrix.stats.MatrixStats;
import org.elasticsearch.search.aggregations.metrics.Stats;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 陈荣
 * @ClassName: EsClient
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @date 2026-04-15  21:50
 * @Version: 1
 */
public class EsSearchClient {
    private RestHighLevelClient client;

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.147.136:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    /**
     * 全文搜索
     */
    @Test
    void testMatchAll() throws IOException {
        // 1.构建request
        SearchRequest request = new SearchRequest("items");
        // 2.构建查询条件
        request.source().query(QueryBuilders.matchAllQuery());
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析结果
        extracted(response);
    }

    /**
     * 复杂条件搜索
     */
    @Test
    void testMatch() throws IOException {
        // 1.构建request
        SearchRequest request = new SearchRequest("items");
        // 2.构建查询条件
        request.source().query(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("name", "huawei"))
                .filter(QueryBuilders.termQuery("brand","华为"))

                .filter(QueryBuilders.rangeQuery("price").lt(50000)));
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析结果
        extracted(response);
    }

    /**
     * 分页排序搜索
     */
    @Test
    void testPageOrder() throws IOException {
        int pageNo = 1, pageSize = 5;
        // 1.构建request
        SearchRequest request = new SearchRequest("items");
        // 2.构建查询条件
        // 2.1查全部
        request.source().query(QueryBuilders.matchAllQuery());
        // 2.2分页
        request.source().from((pageNo - 1) * pageSize).size(pageSize);
        // 2.3排序
        request.source().sort("price", SortOrder.ASC)
                .sort("sold", SortOrder.DESC);
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析结果
        extracted(response);
    }

    /**
     * 高亮搜索
     */
    @Test
    void testHighLight() throws IOException {
        // 1.构建请求
        SearchRequest request = new SearchRequest("items");
        // 2.构建查询条件
        request.source().query(QueryBuilders.matchQuery("name", "脱脂牛奶"));
        // 高亮
        request.source().highlighter(SearchSourceBuilder.highlight().field("name"));
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析结果
        extracted(response);
    }

    /**
     * DSL聚合：桶、度量、管道
     * 三要素：聚合的名称、类型、字段
     */
    @Test
    void testAgg() throws IOException {
        // 1.聚合的名称
        String name = "agg_brand", name2 = "agg_price";
        // 2.构建请求
        SearchRequest request = new SearchRequest("items");
        // 3.复合查询条件
        request.source().query(QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("category", "手机"))
                .filter(QueryBuilders.rangeQuery("price").lt("100000")));
        // 4.返回的文档数
        request.source().size(0);
        // 5.聚合的条件
        request.source().aggregation(AggregationBuilders.terms(name).field("brand")
                .subAggregation(AggregationBuilders.stats(name2).field("price")));
        // 6.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 7.解析结果
        Aggregations aggregations = response.getAggregations();
        Terms buk = aggregations.get(name);
        List<? extends Terms.Bucket> buckets = buk.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            Aggregations aggregations1 = bucket.getAggregations();
            Stats stats = aggregations1.get(name2);
            System.out.println(bucket.getKeyAsString() + ": " + bucket.getDocCount() + " 大" + stats.getMax());
        }
    }

    private static void extracted(SearchResponse response) {
        // 1.整体的结果
        SearchHits hits = response.getHits();
        // 2.总条数
        long total = hits.getTotalHits().value;
        System.out.println("total:" + total);
        // 3.查询到的数据内容
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit searchHit : searchHits) {
            // 4.解析
            String json = searchHit.getSourceAsString();
            // 5.原始对象
            ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
            // 6.解析高亮
            Map<String, HighlightField> map = searchHit.getHighlightFields();
            if (map != null && !map.isEmpty()){
                HighlightField hf = map.get("name");
                String hfName = hf.getFragments()[0].string();
                itemDoc.setName(hfName);
            }
            System.out.println("doc:" + itemDoc);
        }
    }
}
