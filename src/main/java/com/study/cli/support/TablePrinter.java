package com.study.cli.support;

import com.study.domain.common.Page;

import java.sql.Blob;
import java.util.List;
import java.util.function.Function;

public class TablePrinter {
    public static class Column<T> {
        public final String header;
        public final int width;
        public final Function<T, String> extractor;
        public Column(String header, int width, Function<T, String>extractor){
            this.header = header; this.width = width; this.extractor = extractor;
        }
    }

    /** 打印分页结果为表格（固定宽度，超出截断） */
    public static <T> void printPage(Page<T> page, List<Column<T>> columns) {
        var rows = page.content().stream()
                .map(t -> columns.stream().map(c -> fit(c.extractor.apply(t), c.width)).toList())
                .toList();

        // 头
        String header = joinCols(columns.stream().map(c -> fit(c.header, c.width)).toList());
        String sep    = header.replaceAll(".", "-");
        System.out.println(header);
        System.out.println(sep);

        // 行
        for (var r : rows) System.out.println(joinCols(r));

        // 脚注
        System.out.printf("%s  第 %d/%d 页，共 %d 条%n",
                rows.isEmpty() ? "(空)" : "", page.page(), page.totalPages(), page.total());
    }

    /** 非分页列表也能重用（content + totals 自己传） */
    public static <T> void printList(List<T> list, List<Column<T>> columns) {
        var page = new Page<>(list, 1, list.size() == 0 ? 1 : list.size(), list.size());
        printPage(page, columns);
    }

    private static String joinCols(List<String> cols) { return String.join("  ", cols); }
    private static String fit(String s, int w) {
        if (s == null) s = "";
        if (s.length() == w) return s;
        if (s.length() < w)  return s + " ".repeat(w - s.length());
        return s.substring(0, Math.max(0, w - 1)) + "…";
    }
}
