package com.hmall.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Collection;

/**
 * @author 陈荣
 * @ClassName: CartClient
 * @Description: 购物车服务远程调用
 * @date 2026-04-09  16:25
 * @Version: 1
 */
@FeignClient("cart-service")
public interface CartClient {
    /**
     * 批量删除购物车中商品
     * @param ids
     */
    @DeleteMapping("/carts")
    void deleteCartItemByIds(@RequestParam("ids") Collection<Long> ids);
}
