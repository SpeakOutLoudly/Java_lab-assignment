package com.study.cli;

// 处理输入的关于product命令的类
import com.study.domain.common.PageRequest;
import com.study.domain.common.Sort;
import com.study.domain.repository.ProductRepository;
import com.study.security.Session;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import com.study.domain.model.Product;

import javax.naming.Context;

@Command(name = "product", description = "关于商品的指令",
        subcommands = {ProductCommands.Create.class, ProductCommands.ProductSearch.class})
public class ProductCommands implements Runnable{
    private final Session session;
    private final Product product;
    private final ProductRepository productRepository;

    public ProductCommands(Session session, Product product, ProductRepository productRepository){
        this.product = product; this.session = session; this.productRepository = productRepository;
    }
    public void run(){System.out.println("子命令：create/query");}

    @Command(name = "create", description = "创建商品")
    static class Create implements Runnable{
        private final Session session;
        private final Product product;

        public Create(Session session, Product product){
            this.product = product;
            this.session = session;
        }
        @CommandLine.Option(names = "--n", description = "商品名称") String name;
        @CommandLine.Option(names = "--p", description = "商品价格") double price;

        public void run(){
            if(!session.isLogin()) {System.out.println("未登录");}
            if(!session.ensureSeller()) {System.out.println("非卖家，无法创建");}
            // TODO 这个 ensureSeller() 的逻辑还要改改
            // 调用 ProductAppService 的 create
        }
    }

    @Command(name="search", description="搜索商品")
    static class ProductSearch implements Runnable {
        private final Context ctx;
        public ProductSearch(Context ctx){ this.ctx = ctx; } // 由 App.java 的 ContextAwareFactory 注入

        @CommandLine.Option(names="--kw")   String kw;
        @CommandLine.Option(names="--min")  Integer minPrice;   // 分
        @CommandLine.Option(names="--max")  Integer maxPrice;
        @CommandLine.Option(names="--page") int page = 1;
        @CommandLine.Option(names="--size") int size = 10;
        @CommandLine.Option(names="--sort") String sort = "createdAt,desc"; // 示例：price,asc

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
}
