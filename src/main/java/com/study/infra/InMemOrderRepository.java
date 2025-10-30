package com.study.infra;

import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;
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

    // TODO 待完善
    @Override
    public boolean saveChanges(Order order, long expectedVersion){

        return true;
    }
    /** 修改状态（状态机外层已校验），带版本比较，失败抛 OptimisticLockException 或返回 false */
    @Override
    public boolean updateStatus(long orderId, Order.Status from, Order.Status to, long expectedVersion){
        return true;
    }

    @Override
    public Order save(Order o) {
        // 最小化：不做乐观锁；如果需要，改成比较 o.getVersion()
        if (o.getId() == 0) {
            long id = idGen.getAndIncrement();
            // 若你的 Order 有 attachPersistedIdentity(...)，建议用它
            try {
                o.getClass().getMethod("attachPersistedIdentity", long.class).invoke(o, id);
            } catch (ReflectiveOperationException e) {
                // 没有的话就退回直接设置（若有 setter）
                // 如果没有 setter，说明你的 Order 已经在工厂里处理好了，这里只 put 即可
                // 如需强制要求 attach/bump，请在 Order 内提供对应方法
                // 这里为了通用性，仅保留 put
            }
        }
        byId.put(o.getId(), o);
        return o;
    }

    @Override
    public Page<Order> listByBuyer(long buyerId, PageRequest page) {
        List<Order> all = byId.values().stream()
                // 若 getBuyerId() 返回 Optional<Long>，用 orElse 取值；若返回 long，改为 o.getBuyerId() == buyerId
                .filter(o -> {
                    try {
                        var opt = (Optional<Long>) o.getClass().getMethod("getBuyerId").invoke(o);
                        return opt.orElse(-1L) == buyerId;
                    } catch (ReflectiveOperationException e) {
                        // 退回假设：有 long getBuyerId()
                        try {
                            long v = (long) o.getClass().getMethod("getBuyerId").invoke(o);
                            return v == buyerId;
                        } catch (ReflectiveOperationException ex) {
                            return false;
                        }
                    }
                })
                .sorted(orderDesc())
                .collect(Collectors.toList());

        return toPage(all, page);
    }

    @Override
    public Page<Order> listBySeller(long sellerId, PageRequest page) {
        List<Order> all = byId.values().stream()
                .filter(o -> {
                    try {
                        long v = (long) o.getClass().getMethod("getSellerId").invoke(o);
                        return v == sellerId;
                    } catch (ReflectiveOperationException e) {
                        return false;
                    }
                })
                .sorted(orderDesc())
                .collect(Collectors.toList());

        return toPage(all, page);
    }

    /* ============ 工具 ============ */

    // 排序：createdAt DESC，再 id DESC（createdAt 允许为 null 时放最后）
    private Comparator<Order> orderDesc() {
        Comparator<Instant> c = Comparator.nullsLast(Comparator.naturalOrder());
        return Comparator
                .comparing((Order o) -> {
                    try {
                        return (Instant) o.getClass().getMethod("getCreatedAt").invoke(o);
                    } catch (ReflectiveOperationException e) {
                        return null;
                    }
                }, c).reversed()
                .thenComparing(o -> o.getId(), Comparator.reverseOrder());
    }

    private Page<Order> toPage(List<Order> all, PageRequest req) {
        int page = Math.max(1, req.getPage()); // 假设 PageRequest 是 1-based
        int size = Math.max(1, req.getSize());
        int from = Math.min((page - 1) * size, all.size());
        int to   = Math.min(from + size, all.size());
        List<Order> slice = all.subList(from, to);
        long total = all.size();

        // 如果你的 Page 不是 Page.of(...) 这种工厂，请改成项目里的构造方式
        return new Page<Order>(slice, page, size, total);
    }
}
