package com.study.cli;

// 处理输入的关于product命令的类
import com.study.domain.repository.ProductRepository;
import com.study.security.Session;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import com.study.domain.model.Product;
import picocli.CommandLine.Command;
import com.study.domain.model.Product;

@Command(name = "product", subcommands = {Product.class})
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

        public void run{
            if(!session.isLogin()) {System.out.println("未登录");}
            if(!session.ensureSeller()) {System.out.println("非卖家，无法创建");}
        }
    }
}
