package com.hmall.gateway.filters;


import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author 陈荣
 * @ClassName: MyGlobalFilter
 * @Description: 全局过滤器
 * @date 2026-04-10  12:28
 * @Version: 1
 */

@Component
public class MyGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        // 3.放行
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 执行顺序，值越小，优先级越高
        return 0;
    }
}
