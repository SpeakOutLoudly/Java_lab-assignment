package com.study.domain.common;

// Page.java —— “这一页的结果”

import java.util.*;
import java.util.function.Function;

public final class Page<T> {
    private final List<T> content; // 当前页数据
    private final int page;        // 当前页码（从 1）
    private final int size;        // 每页条数
    private final long total;      // 符合条件的总条数

    public Page(List<T> content, int page, int size, long total) {
        this.content = List.copyOf(content);
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public List<T> content(){ return content; }
    public int page(){ return page; }
    public int size(){ return size; }
    public long total(){ return total; }

    /** 总页数（向上取整） */
    public int totalPages() { return (int) Math.ceil(total * 1.0 / size); }

    /** 映射为另一种类型（常用于转换为 View/DTO） */
    public <R> Page<R> map(Function<T,R> mapper){
        var mapped = content.stream().map(mapper).toList();
        return new Page<>(mapped, page, size, total);
    }

    @Override public String toString(){
        return "Page{page=" + page + ", size=" + size + ", total=" + total + ", content=" + content.size() + "}";
    }
}
