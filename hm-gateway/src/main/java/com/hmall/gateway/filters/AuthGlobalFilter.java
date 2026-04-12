package com.hmall.gateway.filters;

import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author 陈荣
 * @ClassName: AuthGlobalFilter
 * @Description: 用户校验过滤器
 * @date 2026-04-10  13:05
 * @Version: 1
 */

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTool jwtTool;
    private final AuthProperties authProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.获取请求request
        ServerHttpRequest request = exchange.getRequest();

        // 2.判断路径是否需要拦截
        if (isExclude(request.getPath().toString())){
            // 不需要拦截 放行
            return chain.filter(exchange);
        }

        // 3.获取token
        String token = null;
        List<String> header = request.getHeaders().get("authorization");
        if (header != null && !header.isEmpty()){
            token = header.get(0);
        }

        // 4.解析并校验token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException unauthorizedException){
            // token有问题 拦截
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 5.传递用户信息
        String userInfo = String.valueOf(userId);
        ServerWebExchange swe = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo))
                .build();

        // 6.放行
        return chain.filter(swe);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private boolean isExclude(String path){
        for (String pathPattern : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern, path)){
                return true;
            }
        }
        return false;
    }
}
