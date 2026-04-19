package com.hmall.item.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈荣
 * @ClassName: EsClient
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @date 2026-04-19  17:13
 * @Version: 1
 */

@Configuration
public class EsClient {
    @Bean
    public RestHighLevelClient restHighLevelClient(){
        return new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.147.136:9200")));
    }
}
