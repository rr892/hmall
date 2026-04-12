package com.hmall.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;


/**
 * @author 陈荣
 * @ClassName: TradeClient
 * @Description: 交易服务远程调用
 * @date 2026-04-09  16:52
 * @Version: 1
 */
@FeignClient("trade-service")
public interface TradeClient {

    /**
     * 标记订单已支付
     * @param orderId
     */
    @PutMapping("/orders/{orderId}")
    void markOrderPaySuccess(@PathVariable("orderId") Long orderId);
}
