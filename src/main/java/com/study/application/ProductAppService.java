package com.study.application;

import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;
import com.study.domain.model.Product;
import com.study.domain.repository.ProductRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class ProductAppService {

    private final ProductRepository products;

    public ProductAppService(ProductRepository products) {
        this.products = Objects.requireNonNull(products);
    }

    /* ============ 查询 ============ */

    /** 卖家后台：按卖家分页查看自己的商品 */
    public Page<Product> listBySeller(long sellerId, PageRequest page) {
        return products.listBySeller(sellerId, page);
    }

    /** 前台/通用搜索：关键字 + 状态 + 价格区间（单位：分） */
    public Page<Product> search(String keyword,
                                Product.Status status,
                                Integer minPriceCents,
                                Integer maxPriceCents,
                                PageRequest page) {
        return products.search(keyword, status, minPriceCents, maxPriceCents, page);
    }

    /** 批量按 ID 查询（详情渲染/订单回显常用） */
    public List<Product> findByIds(List<Long> ids) {
        return products.findByIds(ids);
    }

    /** 单个查询 */
    public Product get(long productId) {
        return require(productId);
    }

    /* ============ 新建 ============ */

    /** 卖家发布新商品（初始为 DRAFT） */
    public Product create(long sellerId, String name, int priceCents, int initStock) {
        var now = Instant.now();
        var p = Product.createNew(sellerId, name, priceCents, initStock, now);
        return products.save(p); // 仓储分配 id / version=1
    }

    /* ============ 基本信息维护（名称/描述/价格） ============ */

    public Product rename(long productId, String newName) {
        var p = require(productId);
        var ver = p.getVersion();
        p.rename(newName, Instant.now());
        if (!products.saveChanges(p, ver)) {
            throw new RuntimeException("保存失败：版本不匹配或商品不存在");
        }
        return products.findById(productId).orElse(p);
    }

    public Product changeDescription(long productId, String newDesc) {
        var p = require(productId);
        var ver = p.getVersion();
        p.changeDescription(newDesc, Instant.now());
        if (!products.saveChanges(p, ver)) {
            throw new RuntimeException("保存失败：版本不匹配或商品不存在");
        }
        return products.findById(productId).orElse(p);
    }

    public Product changePrice(long productId, int newPriceCents) {
        var p = require(productId);
        var ver = p.getVersion();
        p.changePrice(newPriceCents, Instant.now());
        if (!products.saveChanges(p, ver)) {
            throw new RuntimeException("保存失败：版本不匹配或商品不存在");
        }
        return products.findById(productId).orElse(p);
    }

    /* ============ 上/下架/删除 ============ */

    /** 上架（需要库存>0） */
    public boolean activate(long productId) {
        var p = require(productId);
        // 由仓储负责落库与版本递增；领域规则在 Product.activate 内
        // 这里直接请求状态变更，仓储应在实现中调用领域方法或直接设置并校验
        return products.updateStatus(productId, Product.Status.ACTIVATE, p.getVersion());
    }

    /** 下架 */
    public boolean deactivate(long productId) {
        var p = require(productId);
        return products.updateStatus(productId, Product.Status.INACTIVATE, p.getVersion());
    }

    /** 逻辑删除（不可恢复上架） */
    public boolean delete(long productId) {
        var p = require(productId);
        return products.updateStatus(productId, Product.Status.DELETED, p.getVersion());
    }

    /* ============ 库存调整 ============ */

    /** 进货/回滚：增加库存 */
    public boolean increaseStock(long productId, int delta) {
        var p = require(productId);
        return products.increaseStock(productId, delta, p.getVersion());
    }

    /** 扣减库存（下单/发货） */
    public boolean decreaseStock(long productId, int delta) {
        var p = require(productId);
        return products.decreaseStock(productId, delta, p.getVersion());
    }

    /* ============ 私有工具 ============ */

    private Product require(long id) {
        return products.findById(id).orElseThrow(() -> new RuntimeException("商品不存在: " + id));
    }
}
