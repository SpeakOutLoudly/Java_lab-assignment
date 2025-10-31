package com.study.infra;

import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;
import com.study.domain.common.Sort;
import com.study.domain.model.Order;
import com.study.domain.repository.OrderRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemOrderRepository implements OrderRepository {

    private final Map<Long, Order> byId = new LinkedHashMap<>();
    private final AtomicLong idGen = new AtomicLong(100);

    @Override
    public Optional<Order> findById(long id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** 简化实现：忽略 expectedVersion，存在则覆盖保存，不存在返回 false */
    @Override
    public boolean saveChanges(Order order, long expectedVersion) {
        if (order == null || order.getId() == 0) return false;
        if (!byId.containsKey(order.getId())) return false;
        byId.put(order.getId(), order);
        return true;
    }

    /** 简化实现：校验 from 状态，更新为 to；忽略 expectedVersion */
    @Override
    public boolean updateStatus(long orderId, Order.Status from, Order.Status to, long expectedVersion) {
        var cur = byId.get(orderId);
        if (cur == null) return false;
        if (cur.getStatus() != from) return false;
        switch (to) {
            case PAID ->    cur.paid(Instant.now());
            case SHIPPED -> cur.shipped(Instant.now());
            case CONFIRMED -> cur.confirm(Instant.now());
            case FINISHED -> cur.finish(Instant.now());
            case CANCELLED -> cur.cancel(Instant.now());
            default -> throw new IllegalArgumentException("非法");
        }
        byId.put(orderId, cur);
        return true;
    }

    @Override
    public Order save(Order o) {
        if (o.getId() == 0) {
            o.attachPersistedIdentity(idGen.getAndIncrement());   // 练习版：直接赋 id
        }
        byId.put(o.getId(), o);
        return o;
    }

    @Override
    public Page<Order> listByBuyer(long buyerId, PageRequest req) {
        var cmp = resolveComparator(req);
        List<Order> all = byId.values().stream()
                .filter(o -> o.getBuyerId() == buyerId)
                .sorted(cmp)
                .collect(Collectors.toList());
        return toPage(all, req);
    }

    @Override
    public Page<Order> listBySeller(long sellerId, PageRequest req) {
        var cmp = resolveComparator(req);
        List<Order> all = byId.values().stream()
                .filter(o -> o.getSellerId() == sellerId)
                .sorted(cmp)
                .collect(Collectors.toList());
        return toPage(all, req);
    }

    /* ============ 工具 ============ */

    // 默认排序：createdAt DESC，再 id DESC
    private Comparator<Order> defaultOrderDesc() {
        return Comparator
                .comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(Order::getId, Comparator.reverseOrder());
    }

    // 根据 PageRequest.sort 解析排序；不识别的字段回退默认
    private Comparator<Order> resolveComparator(PageRequest req) {
        if (req == null || req.getSort() == null || req.getSort().isUnsorted()) {
            return defaultOrderDesc();
        }
        // 练习版：只看第一个排序字段；可按需扩展为多字段
        Sort.Order first = req.getSort().orders().get(0);
        Comparator<Order> c = switch (first.property()) {
            case "id" -> Comparator.comparing(Order::getId);
            case "createdAt" -> Comparator.comparing(
                    Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "amount" -> Comparator.comparing(Order::getAmount);
            default -> defaultOrderDesc(); // 未识别字段回退默认
        };
        if (first.direction() == Sort.Direction.DESC) c = c.reversed();
        // 次序稳定：再按 id DESC 兜底
        return c.thenComparing(Order::getId, Comparator.reverseOrder());
    }

    private Page<Order> toPage(List<Order> all, PageRequest req) {
        int page = Math.max(1, req.getPage()); // 1-based
        int size = Math.max(1, req.getSize());
        int from = Math.min((page - 1) * size, all.size());
        int to   = Math.min(from + size, all.size());
        List<Order> slice = all.subList(from, to);
        long total = all.size();
        return new Page<>(slice, page, size, total);
    }
}
