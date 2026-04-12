package com.hmall.gateway.route;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * @author 陈荣
 * @ClassName: DynamicRouteLoader
 * @Description: 监听nacos获取最新路由配置
 * @date 2026-04-11  18:05
 * @Version: 1
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {

    private final String dataId = "gateway-routes.json";
    private final String group = "DEFAULT_GROUP";
    private final NacosConfigManager nacosConfigManager;
    private final RouteDefinitionWriter writer;
    private final Set<String> routeIds = new HashSet<>();

    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        // 1.注册监听器并首次加载配置信息
        ConfigService configService = nacosConfigManager.getConfigService();
        String configInfo = configService.getConfigAndSignListener(dataId, group, 5000, new Listener(){
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                // 更新配置信息
                updateConfigInfo(configInfo);
            }
        });
        // 2.首次启动，更新配置信息
        updateConfigInfo(configInfo);
    }

    private void updateConfigInfo(String configInfo) {
        log.debug("监听到路由配置变更，{}", configInfo);
        // 1.json格式转化为RouteDefinition对象
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        // 2.删除旧的路由表
        for (String routeId : routeIds) {
            writer.delete(Mono.just(routeId)).subscribe();
        }
        routeIds.clear();
        // 3.更新路由表
        for (RouteDefinition routeDefinition : routeDefinitions) {
            writer.save(Mono.just(routeDefinition)).subscribe();
            // 保存路由表id
            routeIds.add(routeDefinition.getId());
        }
    }
}
