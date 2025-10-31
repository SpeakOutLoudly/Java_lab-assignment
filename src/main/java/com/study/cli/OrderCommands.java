package com.study.cli;

import java.util.List;

import com.study.domain.common.*;
import com.study.application.OrderAppService;
import com.study.domain.model.Order;
import com.study.domain.repository.OrderRepository;
import com.study.security.Session;
import picocli.CommandLine.*;

@Command(name="order", description="订单相关", subcommands = {
        OrderCommands.Create.class, OrderCommands.ListMine.class, OrderCommands.Detail.class
})
public class OrderCommands implements Runnable {
    public void run(){ System.out.println("子命令：create / list"); }

    private final Page<Order> pages;
    private final PageRequest pageRequests;
    private final Session session;
    private final OrderAppService order;
    private final OrderRepository orderRepo;
    public OrderCommands(Session s, OrderAppService o, OrderRepository repo, Page<Order> pages, PageRequest pageRequests){
        this.session=s; this.order=o; this.orderRepo=repo; this.pages = pages;
        this.pageRequests = pageRequests;
    }

    @Command(name="create", description="创建订单（买家）")
    static class Create implements Runnable {
        @Option(names="--product", required=true) long productId;
        @Option(names="--qty", required=true) int qty;

        private final Session session;
        private final OrderAppService order;
        public Create(Session s, OrderAppService o){ this.session=s; this.order=o; }

        public void run() {
            session.ensureBuyer();
            Order o = order.create(session.requireLogin().getId(), productId, qty);
            System.out.printf("下单成功：订单ID=%d 金额=%.2f 状态=%s%n", o.getId(), o.getAmount()/100.0, o.getStatus());
        }
    }

    @Command(name="list", description="我的订单（买家）")
    static class ListMine implements Runnable {
        private final Session session;
        private final OrderRepository orderRepo;
        public ListMine(Session s, OrderRepository r){ this.session=s; this.orderRepo=r; }

        public void run() {
            if(!session.isLogin()){ System.out.println("未登录"); return; }
            var list = orderRepo.listByBuyer(session.requireLogin().getId(), );
            if(list.isEmpty()){ System.out.println("暂无订单"); return; }
            System.out.printf("%-6s %-8s %-6s %-8s %-10s%n","订单ID","商品ID","数量","金额(元)","状态");
            for(var o: list){
                System.out.printf("%-6d %-8d %-6d %-8.2f %-10s%n",
                        o.id, o.productId, o.qty, o.amount/100.0, o.getStatus());
            }
        }
    }

    @Command(name = "detail", description = "查看订单详细")
    static class Detail implements Runnable{
        @Option(names="--id", required=true) long id;
        private final Session session; private final OrderRepository orderRepo;
        public Detail(Session s, OrderRepository repo){ this.session=s; this.orderRepo=repo; }
        public void run(){
            // TODO 确定查看订单的人的权限
            if(!session.isLogin()){System.out.println("未登录");}

        }
    }


}
