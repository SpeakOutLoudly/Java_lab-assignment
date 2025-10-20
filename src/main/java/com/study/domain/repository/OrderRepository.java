package com.study.domain.repository;

import com.study.domain.model.Order;
import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;

import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(long id);

    Page<Order> listByBuyer(long buyerId, PageRequest page);
    Page<Order> listBySeller(long sellerId, PageRequest page);

    /** 创建订单（初始状态 CREATED），返回带 id/version 的订单 */
    Order create(Order order);

    /** 修改状态（状态机外层已校验），带版本比较，失败抛 OptimisticLockException 或返回 false */
    boolean updateStatus(long orderId, Order.Status from, Order.Status to, long expectedVersion);

    /** 修改金额/收货信息等（如做售后），带版本比较 */
    boolean saveChanges(Order order, long expectedVersion);
}
