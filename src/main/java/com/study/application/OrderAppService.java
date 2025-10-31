package com.study.application;

import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;
import com.study.domain.model.Order;
import com.study.domain.model.Product;
import com.study.domain.repository.OrderRepository;
import com.study.domain.repository.ProductRepository;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class OrderAppService {

    private final OrderRepository orders;
    private final ProductRepository products;
    // 如果订单ID由仓储生成，这个 idGen 可删除；这里保留以防你需要在别处用
    private final AtomicLong idGen = new AtomicLong(100);

    public OrderAppService(OrderRepository orders, ProductRepository products) {
        this.orders = orders;
        this.products = products;
    }

    /**
     * 下单（演示版）：
     * 1) 校验商品与库存；
     * 2) 创建订单（初始状态 CREATED，金额=单价*数量）；
     * 3) 先保存订单拿到 orderId；
     * 4) 扣减库存并保存商品；
     * 5) （可选）直接把订单推进到 CONFIRMED 以便测试。
     */
    public Order create(long buyerId, long productId, int qty) {
        Product p = products.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        if (qty <= 0) throw new RuntimeException("数量必须>0");
        if (p.getStock() < qty) throw new RuntimeException("库存不足");

        int amount = Math.multiplyExact(p.getPriceCents(), qty);
        Order o = Order.createNew(buyerId, p.getSellerId(), p.getId(), qty, amount, Instant.now());

        // 先保存订单，确保有 ID（有些实现会在 save 时分配 id/version）
        o = orders.save(o);

        // 扣库存（练习版：直接改并保存；若你实现了 decreaseStock(...), 也可调用仓储方法）
        p.decreaseStock(qty, Instant.now());
        products.save(p);

        // 演示/测试方便：直接把订单推进到 CONFIRMED（跳过支付/发货）
        // expectedVersion 这里传 0 表示不做版本校验（看你的仓储实现约定）
        orders.updateStatus(o.getId(), Order.Status.CREATED, Order.Status.CONFIRMED, 0);

        return orders.findById(o.getId()).orElse(o);
    }

    /** 买家查看自己的订单（分页） */
    public Page<Order> listByBuyer(long buyerId, PageRequest page) {
        return orders.listByBuyer(buyerId, page);
    }

    /** 卖家查看自己的订单（分页） */
    public Page<Order> listBySeller(long sellerId, PageRequest page) {
        return orders.listBySeller(sellerId, page);
    }

    /** （可选）取消订单：仅在 CREATED 时允许 */
    public boolean cancel(long orderId) {
        return orders.updateStatus(orderId, Order.Status.CREATED, Order.Status.CANCELLED, 0);
    }

    /** （可选）确认收货：从 SHIPPED → CONFIRMED */
    public boolean confirm(long orderId) {
        return orders.updateStatus(orderId, Order.Status.SHIPPED, Order.Status.CONFIRMED, 0);
    }

    /** （可选）更新其它信息（如收货信息/金额调整等），演示版直接覆盖保存 */
    public boolean saveChanges(Order changed, long expectedVersion) {
        return orders.saveChanges(changed, expectedVersion);
    }
}
