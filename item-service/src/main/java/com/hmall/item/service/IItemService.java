package com.hmall.item.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.query.ItemPageQuery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
public interface IItemService extends IService<Item> {

    void deductStock(List<OrderDetailDTO> items);

    List<ItemDTO> queryItemByIds(Collection<Long> ids);

    PageDTO<ItemDTO> searchByEs(ItemPageQuery query);

    /**
     * 新增商品
     */
    void saveItem(ItemDTO itemDTO);

    /**
     * 更新商品状态
     * @param id
     * @param status
     */
    void updateItemStatus(Long id, Integer status);

    /**
     * 更新商品
     * @param item
     */
    void updateItem(ItemDTO item);

    /**
     * 删除商品
     * @param id
     */
    void deleteItemById(Long id);
}
