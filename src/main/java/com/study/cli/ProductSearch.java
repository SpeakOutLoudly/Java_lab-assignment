package com.study.cli;

import com.study.App.Context;
import com.study.domain.model.Product;
import com.study.domain.query.ProductQuery;
import com.study.domain.common.PageRequest;
import com.study.domain.common.Sort;
import com.study.cli.support.TablePrinter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name="search", description="搜索商品")
public class ProductSearch implements Runnable {
    private final Context ctx;
    public ProductSearch(Context ctx){ this.ctx = ctx; } // 由 App.java 的 ContextAwareFactory 注入

    @Option(names="--kw")   String kw;
    @Option(names="--min")  Integer minPrice;   // 分
    @Option(names="--max")  Integer maxPrice;
    @Option(names="--page") int page = 1;
    @Option(names="--size") int size = 10;
    @Option(names="--sort") String sort = "createdAt,desc"; // 示例：price,asc

    @Override public void run() {
        var query   = new ProductQuery(kw, null, minPrice, maxPrice, Product.Status.ON);
        var sortObj = parseSort(sort);                         // 解析 --sort
        var pr      = new PageRequest(page, size, sortObj);
        var pageRes = ctx.products.search(query, pr);          // 调用应用层（注意用 ctx，而不是静态 Application）

        TablePrinter.print(pageRes.content());
        int totalPages = (int)Math.ceil(pageRes.total()*1.0 / pageRes.size());
        System.out.printf("第 %d/%d 页，共 %d 条%n", pageRes.page(), totalPages, pageRes.total());
    }

    private Sort parseSort(String s){
        // 极简解析：field,dir
        if (s == null || s.isBlank()) return Sort.by("createdAt");
        var parts = s.split(",", 2);
        var field = parts[0].trim();
        var dir   = (parts.length>1 ? parts[1].trim() : "asc").equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return new Sort(java.util.List.of(new Sort.Order(field, dir)));
    }
}
