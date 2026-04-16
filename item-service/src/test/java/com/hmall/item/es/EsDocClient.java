package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

/**
 * @author 陈荣
 * @ClassName: EsDocClient
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @date 2026-04-16  9:57
 * @Version: 1
 */

@SpringBootTest(properties = "spring.profiles.active=local")
public class EsDocClient {
    private RestHighLevelClient client;

    @Autowired
    private IItemService iItemService;

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.147.136:9200")
        ));
    }

    @Test
    void testConnect() {
        System.out.println(client);
    }

    /**
     * 新增文档
     */
    @Test
    void testIndexDoc() throws IOException {
        // 0.查询数据库获取文档数据
        Item item = iItemService.getById(317578);
        // 0.1转为文档数据实体
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        // 0.2文档数据实体转为JSON
        String itemDocJSON = JSONUtil.toJsonStr(itemDoc);
        // 1.获取请求request
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        // 2.构建文档数据
        request.source(itemDocJSON, XContentType.JSON);
        // 3.发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    /**
     * 查询文档
     */
    @Test
    void testGetDoc() throws IOException {
        // 1.获取请求request
        GetRequest request = new GetRequest("items").id("317578");
        // 2.发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String source = response.getSourceAsString();
        ItemDoc bean = JSONUtil.toBean(source, ItemDoc.class);
        System.out.println(bean);
    }

    /**
     * 删除文档
     */
    @Test
    void testDeleteDoc() throws IOException {
        // 1.获取请求request
        DeleteRequest request = new DeleteRequest("items").id("317578");
        // 2.发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    /**
     * 更新文档
     */
    @Test
    void testUpdateDoc() throws IOException {
        // 1.获取请求request 注意这里不是.id()
        UpdateRequest request = new UpdateRequest("items","317578");
        // 2.构建修改字段
        request.doc(
                "price", 58800,
                "commentCount", 1
        );
        // 3.发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}
