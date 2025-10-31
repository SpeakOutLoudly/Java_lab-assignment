package com.study.domain.repository;

import com.study.domain.model.Product;
import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;

import java.util.List;
import java.util.Optional;

/**
 * Product 仓储接口（领域契约）
 *
 * 约定（结合 Product 模型）：
 * - 新建：使用 Product.createNew(...) 构造，id=0、version=0；持久化后应分配 id，并把 version 置为 1。
 * - 更新：基于 expectedVersion 做乐观锁（练习版实现可忽略 expectedVersion，直接覆盖，但接口先保留）。
 * - 状态：使用模型中的枚举 {DRAFT, ACTIVATE, INACTIVATE, DELETED}。
 * - 价格/库存：以“分”为单位（int），库存为非负。
 */
public interface ProductRepository {

    /** 主键查询 */
    Optional<Product> findById(long id);

    /** 批量按 id 查询（渲染订单列表、购物车等常用） */
    List<Product> findByIds(List<Long> ids);

    /** 卖家名下的商品分页列表（便于卖家管理） */
    Page<Product> listBySeller(long sellerId, PageRequest page);

    /**
     * 搜索商品（简化版查询条件）：
     * @param keyword           关键字（匹配 name/description，null 或空表示不限）
     * @param status            商品状态（如只展示 ACTIVATE，上层可传 null 表示不限）
     * @param minPriceCents     最低价（单位：分，null 表示不限）
     * @param maxPriceCents     最高价（单位：分，null 表示不限）
     * @param page              分页与排序请求（如按 createdAt desc）
     */
    Page<Product> search(String keyword,
                         Product.Status status,
                         Integer minPriceCents,
                         Integer maxPriceCents,
                         PageRequest page);

    /** 新建或首次持久化商品：返回带 id/version 的实体 */
    Product save(Product product);

    /**
     * 保存字段变更（改名、描述、价格等），带版本比较。
     * @param product           修改后的实体（必须已持久化，id!=0）
     * @param expectedVersion   读取时看到的版本；不匹配时返回 false
     */
    boolean saveChanges(Product product, long expectedVersion);

    /**
     * 改变状态（上/下架、删除），带版本比较；不负责业务校验（由领域方法先检查）。
     * 常见迁移：DRAFT→ACTIVATE、ACTIVATE→INACTIVATE、任意→DELETED（逻辑删除）
     */
    boolean updateStatus(long productId, Product.Status to, long expectedVersion);

    /**
     * 扣减库存（下单/发货），带版本比较。
     * 备注：领域模型已限制 delta>0、且会在降为 0 时自动置 INACTIVATE（若当前为 ACTIVATE）。
     */
    boolean decreaseStock(long productId, int delta, long expectedVersion);

    /** 增加库存（进货/回滚），带版本比较。 */
    boolean increaseStock(long productId, int delta, long expectedVersion);
}
