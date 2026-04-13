package com.hmall.api.fallback;

import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import java.util.Collection;
import java.util.List;

/**
 * @author 陈荣
 * @ClassName: UserClientFallbackFactory
 * @Description: 商品服务fallback
 * @date 2026-04-13  21:39
 * @Version: 1
 */
@Slf4j
public class ItemClientFallbackFactory implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            // 查询失败时的逻辑
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                log.error("查询商品失败！", cause);
                return CollUtils.emptyList();
            }

            // 扣减库存失败时的逻辑
            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                log.error("扣减库存失败失败！", cause);
                throw new RuntimeException(cause);
            }
        };
    }
}
