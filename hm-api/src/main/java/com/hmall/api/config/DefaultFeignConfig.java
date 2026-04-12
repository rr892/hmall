package com.hmall.api.config;

import com.hmall.common.utils.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

/**
 * @author 陈荣
 * @ClassName: DefaultFeignConfig
 * @Description: 远程调用拦截器
 * @date 2026-04-11  11:20
 * @Version: 1
 */
public class DefaultFeignConfig {

    @Bean
    public RequestInterceptor userInfoRequestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                Long userId = UserContext.getUser();
                if (userId != null){
                    requestTemplate.header("user-info", userId.toString());
                }
;            }
        };
    }
}
