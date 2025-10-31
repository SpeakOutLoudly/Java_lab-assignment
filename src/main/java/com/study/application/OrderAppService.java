package com.study.application;

import com.study.domain.model.Order;
import com.study.domain.repository.OrderRepository;
import com.study.domain.repository.ProductRepository;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import com.study.domain.model.*;
import com.study.domain.repository.*;
import java.util.concurrent.atomic.AtomicLong;

public class OrderAppService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final AtomicLong idGen = new AtomicLong(100); // 简单 id 生成
    public OrderAppService(OrderRepository orders, ProductRepository products){
        this.orders=orders; this.products=products;
    }
    public Order create(long buyerId, long productId, int qty){
        var p = products.findById(productId).orElseThrow(()->new RuntimeException("商品不存在"));
        if(qty<=0) throw new RuntimeException("数量必须>0");
        if(p.getStock()<qty) throw new RuntimeException("库存不足");
        var o = Order.createNew(buyerId, p.getSellerId(), p.getId(), qty, p.getPriceCents(), Instant.now());
        // 简化：直接确认（跳过支付/发货）
        // 后面测试麻烦就简化
        orders.updateStatus(o.getId(), Order.Status.CREATED, Order.Status.CREATED, 1);
        //o.updateS() = Order.Status.CONFIRMED;
        // 写入订单与扣库存（这里不考虑并发/事务）
        p.decreaseStock(qty, Instant.now());
        products.save(p);
        return orders.save(o);
    }
}