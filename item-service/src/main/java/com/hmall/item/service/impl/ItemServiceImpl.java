package com.hmall.item.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.domain.query.ItemPageQuery;
import com.hmall.item.mapper.ItemMapper;
import com.hmall.item.service.IItemService;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import io.swagger.models.auth.In;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.MetadataIndexTemplateService;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;


/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
@RequiredArgsConstructor
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    private final RestHighLevelClient restHighLevelClient;

    @Override
    @Transactional
    public void deductStock(List<OrderDetailDTO> items) {
        String sqlStatement = "com.hmall.item.mapper.ItemMapper.updateStock";
        boolean r = false;
        try {
            r = executeBatch(items, (sqlSession, entity) -> sqlSession.update(sqlStatement, entity));
        } catch (Exception e) {
            throw new BizIllegalException("更新库存异常，可能是库存不足!", e);
        }
        if (!r) {
            throw new BizIllegalException("库存不足！");
        }
    }

    @Override
    public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
        return BeanUtils.copyList(listByIds(ids), ItemDTO.class);
    }

    /**
     * 利用es构建复杂条件查询
     * @param query
     * @return
     */
    @Override
    public PageDTO<ItemDTO> searchByEs(ItemPageQuery query){
        // 1.构建查询es的请求
        SearchRequest request = new SearchRequest("items");

        // 2.添加查询条件
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 2.1搜索的关键字
        if (StrUtil.isNotBlank(query.getKey())){
            queryBuilder.must(QueryBuilders.matchQuery("name", query.getKey()));
            // 高亮
            request.source().highlighter(SearchSourceBuilder.highlight().field("name"));
        }
        // 2.2是否分类
        if (StrUtil.isNotBlank(query.getCategory())){
            queryBuilder.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        // 2.3是否选择品牌
        if (StrUtil.isNotBlank(query.getBrand())){
            queryBuilder.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        // 2.4是否选择价格范围
        if (query.getMaxPrice() != null){
            queryBuilder.filter(QueryBuilders.rangeQuery("price")
                    .gte(query.getMinPrice())
                    .lte(query.getMaxPrice()));
        }
        request.source().query(queryBuilder);

        // 2.5是否排序
        String sortName = "updateTime";
        if (StrUtil.isNotBlank(query.getSortBy())) {
            sortName = query.getSortBy();
        }
        request.source().sort(sortName, query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);

        // 2.6页码、页大小
        int pageNo = query.getPageNo(), pageSize = query.getPageSize();
        request.source().from((pageNo - 1) * pageSize).size(pageSize);

        // 3.发送请求
        SearchResponse response = null;
        try {
            response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("发送查询es请求失败：{}", e);
            throw new RuntimeException(e);
        }

        // 4.解析结果并返回
        return PageDTO.of(extracted(response, pageSize));
    }

    /**
     * 新增商品
     * @param itemDTO
     */
    @Override
    @Transactional
    public void saveItem(ItemDTO itemDTO) {
        // 1.写入数据库
        Item item = BeanUtils.copyBean(itemDTO, Item.class);
        save(item);
        // 2.写es
        indexEsItemByDoc(BeanUtil.copyProperties(item, ItemDoc.class));
    }

    /**
     * 更新商品状态
     * @param id
     * @param status
     */
    @Override
    @Transactional
    public void updateItemStatus(Long id, Integer status) {
        // 1.更新数据库
        Item item = getById(id);
        item.setStatus(status);
        updateById(item);
        // 2.更新后的状态
        if (status == 1){
            // 2.1正常添加进es
            indexEsItemByDoc(BeanUtil.copyProperties(item, ItemDoc.class));
        }
        else {
            // 2.2在es中删除
            deleteEsItemById(id);
        }
    }

    /**
     * 更新商品
     * @param itemDTO
     */
    @Override
    @Transactional
    public void updateItem(ItemDTO itemDTO) {
        // 1.不允许修改商品状态，所以强制设置为null，更新时，就会忽略该字段
        itemDTO.setStatus(null);
        // 2.更新数据库
        updateById(BeanUtils.copyBean(itemDTO, Item.class));
        // 3.商品的状态
        Item item = getById(itemDTO.getId());
        // 4.更新es的商品数据
        if (item.getStatus() == 1){
            updateEsItemByDoc(BeanUtil.copyProperties(item, ItemDoc.class));
        }
    }

    /**
     * 删除商品
     * @param id
     */
    @Override
    @Transactional
    public void deleteItemById(Long id) {
        // 1.查询商品的状态
        Integer status = getById(id).getStatus();
        // 2.在数据库中删除商品
        removeById(id);
        // 3.在es中删除商品
        if (status == 1){
            deleteEsItemById(id);
        }
    }

    /**
     * 更新es中的商品数据
     * @param itemDoc
     */
    private void updateEsItemByDoc(ItemDoc itemDoc) {
        // 1。转换为JSON数据
        String jsonStr = JSONUtil.toJsonStr(itemDoc);
        // 2.构建请求
        UpdateRequest request = new UpdateRequest("items", itemDoc.getId());
        // 3.构建请求体商品数据
        request.doc(jsonStr, XContentType.JSON);
        // 4.发送请求
        try {
            restHighLevelClient.update(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("更新es中的商品数据失败！{}", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据id删除es中的商品
     * @param id
     */
    private void deleteEsItemById(Long id) {
        // 1.构建请求
        DeleteRequest request = new DeleteRequest("items").id(id.toString());
        // 2.发送请求
        try {
            restHighLevelClient.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("向es删除数据失败！{}", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 添加商品数据到es
     * @param itemDoc
     */
    private void indexEsItemByDoc(ItemDoc itemDoc) {
        // 1.转换为JSON数据
        String jsonStr = JSONUtil.toJsonStr(itemDoc);
        // 2.构建请求
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        // 3.请求体商品数据
        request.source(jsonStr, XContentType.JSON);
        // 4.发送请求
        try {
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("向es添加数据失败！{}", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析es查询的结果
     * @param response
     * @param pageSize
     * @return
     */
    private static Page<ItemDTO> extracted(SearchResponse response, Integer pageSize) {
        // 1.整体的结果
        SearchHits hits = response.getHits();

        // 2.总条数
        long total = hits.getTotalHits().value;
        List<ItemDTO> itemDTOS = new ArrayList<>();

        // 3.查询到的数据内容
        SearchHit[] searchHits = hits.getHits();
        ItemDTO itemDTO = null;
        for (SearchHit searchHit : searchHits) {
            // 3.1解析
            String json = searchHit.getSourceAsString();
            // 3.2原始对象
            ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
            // 3.3解析高亮
            Map<String, HighlightField> map = searchHit.getHighlightFields();
            if (map != null && !map.isEmpty()){
                HighlightField hf = map.get("name");
                String hfName = hf.getFragments()[0].string();
                itemDoc.setName(hfName);
            }
            // 3.4属性拷贝
            itemDTO = BeanUtil.copyProperties(itemDoc, ItemDTO.class);
            // 3.5加入结果集
            itemDTOS.add(itemDTO);
        }

        // 4.返回
        long pages = total / pageSize;
        Page<ItemDTO> page = new Page<>();
        page.setRecords(itemDTOS);
        page.setTotal(total);
        page.setPages(((total % pageSize) == 0) ? pages : pages + 1);
        return page;
    }
}
