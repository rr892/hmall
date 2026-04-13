package com.hmall.api.client;

import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.fallback.ItemClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Collection;
import java.util.List;

/**
 * @author 陈荣
 * @ClassName: FeignClient
 * @Description: 商品服务远程调用
 * @date 2026-04-09  9:52
 * @Version: 1
 */

@FeignClient(value = "item-service", fallbackFactory = ItemClientFallbackFactory.class)
public interface ItemClient {
    /**
     * 根据id批量查询商品
     * @param ids
     * @return
     */
    @GetMapping("/items")
    List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);

    /**
     * 批量扣减库存
     * @param items
     */
    @PutMapping("/items/stock/deduct")
    void deductStock(@RequestBody List<OrderDetailDTO> items);
}
