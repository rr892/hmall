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
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
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
    //@Transactional
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
