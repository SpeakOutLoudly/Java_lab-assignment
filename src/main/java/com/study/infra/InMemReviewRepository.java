package com.study.infra;

import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;
import com.study.domain.common.Sort;
import com.study.domain.model.Review;
import com.study.domain.repository.ReviewRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 练习版内存实现：
 * - 用 Map<Long, Review> 存数据；
 * - 忽略并发与真正的乐观锁，只做“版本匹配则更新、不匹配返回 false”的最小校验；
 * - 分页：内存中过滤→排序→切片；
 * - 排序：默认 createdAt DESC, id DESC；若 PageRequest 提供 sort，则尝试应用第一个字段。
 */
public class InMemReviewRepository implements ReviewRepository {

    private final Map<Long, Review> byId = new LinkedHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1000);

    @Override
    public Optional<Review> findById(long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Review> findByOrderId(long orderId) {
        // 这里不强制过滤状态；如需只看未删除，可加 && r.getStatus()!=DELETED
        return byId.values().stream()
                .filter(r -> r.getOrderId() == orderId)
                .findFirst();
    }

    @Override
    public Page<Review> listByProduct(long productId, PageRequest page) {
        var cmp = resolveComparator(page);
        // 前台通常只展示 PUBLISHED；这里演示版保留“未删除”的都列出
        List<Review> all = byId.values().stream()
                .filter(r -> r.getProductId() == productId)
                .filter(r -> r.getStatus() != Review.Status.DELETED)
                .sorted(cmp)
                .collect(Collectors.toList());
        return toPage(all, page);
    }

    @Override
    public Page<Review> listByBuyer(long buyerId, PageRequest page) {
        var cmp = resolveComparator(page);
        List<Review> all = byId.values().stream()
                .filter(r -> r.getBuyerId() == buyerId)
                .sorted(cmp)
                .collect(Collectors.toList());
        return toPage(all, page);
    }

    @Override
    public boolean existsByOrderId(long orderId) {
        return byId.values().stream().anyMatch(r -> r.getOrderId() == orderId);
    }

    @Override
    public Review save(Review r) {
        if (r.getId() == 0) {
            long id = idGen.getAndIncrement();
            // 用领域提供的方法附着持久化身份；若你不想用，可直接 setId/version（但当前模型未暴露 setter）
            r.attachPersistedIdentity(id);
        } else {
            // 已持久化对象，模拟一次版本 +1（可选）
            r.bumpVersion();
        }
        byId.put(r.getId(), r);
        return r;
    }

    @Override
    public boolean updateStatus(long reviewId, Review.Status to, long expectedVersion) {
        var cur = byId.get(reviewId);
        if (cur == null) return false;
        if (!versionMatch(cur, expectedVersion)) return false;

        Instant now = Instant.now();
        switch (to) {
            case PUBLISHED -> cur.publish(now);
            case HIDDEN    -> cur.hide(now);
            case DELETED   -> cur.delete(now);
            case PENDING   -> {
                // 模型未提供回退到 PENDING 的领域方法；演示版不支持该迁移
                return false;
            }
        }
        cur.bumpVersion();
        byId.put(reviewId, cur);
        return true;
    }

    @Override
    public boolean saveChanges(Review r, long expectedVersion) {
        if (r == null || r.getId() == 0) return false;
        var cur = byId.get(r.getId());
        if (cur == null) return false;
        if (!versionMatch(cur, expectedVersion)) return false;

        // 简化：直接用传入的对象覆盖（演示版不做字段级合并）
        r.bumpVersion();
        byId.put(r.getId(), r);
        return true;
    }

    /* ================= 工具方法 ================= */

    private boolean versionMatch(Review cur, long expectedVersion) {
        // 练习版策略：expectedVersion==0 时不校验；否则要求相等
        return expectedVersion == 0 || cur.getVersion() == expectedVersion;
    }

    private Comparator<Review> defaultOrder() {
        return Comparator
                .comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(Review::getId, Comparator.reverseOrder());
    }

    private Comparator<Review> resolveComparator(PageRequest req) {
        if (req == null || req.getSort() == null || req.getSort().isUnsorted()) return defaultOrder();

        Sort.Order first = req.getSort().orders().get(0);
        Comparator<Review> c = switch (first.property()) {
            case "id"        -> Comparator.comparing(Review::getId);
            case "createdAt" -> Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "rating"    -> Comparator.comparing(Review::getRating);
            default          -> defaultOrder();
        };
        if (first.direction() == Sort.Direction.DESC) c = c.reversed();
        return c.thenComparing(Review::getId, Comparator.reverseOrder());
    }

    private Page<Review> toPage(List<Review> all, PageRequest req) {
        int page = Math.max(1, req.getPage());
        int size = Math.max(1, req.getSize());
        int from = Math.min((page - 1) * size, all.size());
        int to   = Math.min(from + size, all.size());
        List<Review> slice = all.subList(from, to);
        long total = all.size();
        return new Page<>(slice, page, size, total);
    }
}
