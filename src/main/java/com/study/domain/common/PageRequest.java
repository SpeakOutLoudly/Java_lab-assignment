package com.study.domain.common;
// PageRequest.java —— “怎么取”
import java.util.Objects;

public final class PageRequest {
    private final int page;   // 从 1 开始
    private final int size;   // 每页条数
    private final Sort sort;  // 排序（可为空表示不排序）

    public PageRequest(int page, int size, Sort sort) {
        if (page < 1) throw new IllegalArgumentException("page 从 1 开始");
        if (size < 1) throw new IllegalArgumentException("size 必须 >= 1");
        this.page = page;
        this.size = size;
        this.sort = sort;
    }
    public static PageRequest of(int page, int size) { return new PageRequest(page, size, null); }

    public int getPage() { return page; }
    public int getSize() { return size; }
    public Sort getSort() { return sort; }

    /** 便捷: 计算 OFFSET（给实现层用；注意 page 从 1 开始） */
    public int offset() { return (page - 1) * size; }

    @Override public String toString() {
        return "PageRequest{page=" + page + ", size=" + size + ", sort=" + sort + "}";
    }
}
