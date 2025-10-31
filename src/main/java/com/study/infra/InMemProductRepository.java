package com.study.infra;

import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;
import com.study.domain.common.Sort;
import com.study.domain.model.Product;
import com.study.domain.repository.ProductRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存版 Product 仓储实现（练习用）：
 * - 用 Map<Long, Product> 存放数据；
 * - 分页：内存中过滤→排序→切片；
 * - 版本：expectedVersion==0 时忽略校验；否则要求与当前版本相等；
 * - 状态流转：调用领域方法 activate/deactivate/delete。
 */
public class InMemProductRepository implements ProductRepository {

    private final Map<Long, Product> byId = new LinkedHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Product> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        var out = new ArrayList<Product>(ids.size());
        for (Long id : ids) {
            var p = byId.get(id);
            if (p != null) out.add(p);
        }
        return out;
    }

    @Override
    public Page<Product> listBySeller(long sellerId, PageRequest page) {
        var cmp = resolveComparator(page);
        List<Product> all = byId.values().stream()
                .filter(p -> p.getSellerId() == sellerId)
                .sorted(cmp)
                .collect(Collectors.toList());
        return toPage(all, page);
    }

    @Override
    public Page<Product> search(String keyword,
                                Product.Status status,
                                Integer minPriceCents,
                                Integer maxPriceCents,
                                PageRequest page) {
        var cmp = resolveComparator(page);
        final String kw = (keyword == null) ? null : keyword.trim().toLowerCase();

        List<Product> all = byId.values().stream()
                .filter(p -> kw == null
                        || (safeLower(p.getProductName()).contains(kw)
                        ||  safeLower(p.getDescription()).contains(kw)))
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> minPriceCents == null || p.getPriceCents() >= minPriceCents)
                .filter(p -> maxPriceCents == null || p.getPriceCents() <= maxPriceCents)
                .sorted(cmp)
                .collect(Collectors.toList());

        return toPage(all, page);
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == 0) {
            long id = idGen.getAndIncrement();
            product.attachPersistedIdentity(id); // 将 version 置为 1
        } else {
            // 如果你希望 save 仅用于“新建”，可以在这里抛错；练习版容忍覆盖
            product.bumpVersion();
        }
        byId.put(product.getId(), product);
        return product;
    }

    @Override
    public boolean saveChanges(Product product, long expectedVersion) {
        if (product == null || product.getId() == 0) return false;
        var cur = byId.get(product.getId());
        if (cur == null) return false;
        if (!versionMatch(cur, expectedVersion)) return false;

        product.bumpVersion();
        byId.put(product.getId(), product);
        return true;
    }

    @Override
    public boolean updateStatus(long productId, Product.Status to, long expectedVersion) {
        var cur = byId.get(productId);
        if (cur == null) return false;
        if (!versionMatch(cur, expectedVersion)) return false;

        Instant now = Instant.now();
        switch (to) {
            case ACTIVATE -> {
                try { cur.activate(now); } catch (IllegalStateException e) { return false; }
            }
            case INACTIVATE -> cur.deactivate(now);
            case DELETED -> cur.delete(now);
            case DRAFT -> { return false; } // 练习版不支持退回草稿
            default -> throw new IllegalArgumentException("非法状态");
        }
        cur.bumpVersion();
        byId.put(productId, cur);
        return true;
    }

    @Override
    public boolean decreaseStock(long productId, int delta, long expectedVersion) {
        var cur = byId.get(productId);
        if (cur == null) return false;
        if (!versionMatch(cur, expectedVersion)) return false;

        try {
            cur.decreaseStock(delta, Instant.now());
        } catch (RuntimeException e) {
            return false; // 非法 delta 或库存不足
        }
        cur.bumpVersion();
        byId.put(productId, cur);
        return true;
    }

    @Override
    public boolean increaseStock(long productId, int delta, long expectedVersion) {
        var cur = byId.get(productId);
        if (cur == null) return false;
        if (!versionMatch(cur, expectedVersion)) return false;

        try {
            cur.increaseStock(delta, Instant.now());
        } catch (RuntimeException e) {
            return false;
        }
        cur.bumpVersion();
        byId.put(productId, cur);
        return true;
    }

    /* ================= 工具 ================= */

    private boolean versionMatch(Product cur, long expectedVersion) {
        // 练习版：expectedVersion==0 时不校验；否则必须相等
        return expectedVersion == 0 || cur.getVersion() == expectedVersion;
    }

    private Comparator<Product> defaultOrder() {
        return Comparator
                .comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(Product::getId, Comparator.reverseOrder());
    }

    private Comparator<Product> resolveComparator(PageRequest req) {
        if (req == null || req.getSort() == null || req.getSort().isUnsorted())
            return defaultOrder();

        Sort.Order first = req.getSort().orders().get(0);
        Comparator<Product> c = switch (first.property()) {
            case "id"        -> Comparator.comparing(Product::getId);
            case "createdAt" -> Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "price"     -> Comparator.comparing(Product::getPriceCents);
            case "stock"     -> Comparator.comparing(Product::getStock);
            case "name"      -> Comparator.comparing(p -> safeLower(p.getProductName()));
            default          -> defaultOrder();
        };
        if (first.direction() == Sort.Direction.DESC) c = c.reversed();
        return c.thenComparing(Product::getId, Comparator.reverseOrder());
    }

    private Page<Product> toPage(List<Product> all, PageRequest req) {
        int page = Math.max(1, req.getPage());
        int size = Math.max(1, req.getSize());
        int from = Math.min((page - 1) * size, all.size());
        int to   = Math.min(from + size, all.size());
        List<Product> slice = all.subList(from, to);
        long total = all.size();
        return new Page<>(slice, page, size, total);
    }

    private static String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase();
    }
}
