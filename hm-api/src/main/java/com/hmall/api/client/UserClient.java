package com.hmall.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author 陈荣
 * @ClassName: UserClient
 * @Description: 用户服务远程调用
 * @date 2026-04-09  17:36
 * @Version: 1
 */

@FeignClient("user-service")
public interface UserClient {
    /**
     * 扣减余额
     * @param pw
     * @param amount
     */
    @PutMapping("/users/money/deduct")
    void deductMoney(@RequestParam("pw") String pw, @RequestParam("amount") Integer amount);
}
