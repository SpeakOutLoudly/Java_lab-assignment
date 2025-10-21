package com.study.infra;

import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;
import com.study.domain.model.Order;
import com.study.domain.repository.OrderRepository;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemOrderRepository implements OrderRepository {
    private final Path file;
    private final Map<Long, Order> byId = new LinkedHashMap<>();
    private final AtomicLong idGen = new AtomicLong(100);

    public InMemOrderRepository(Path dataDir){
        this.file = dataDir.resolve("orders.jsonl");
        var all = JsonLines.readAll(file, Order.class);
        for (var o: all) { byId.put(o.id, o); idGen.set(Math.max(idGen.get(), o.id+1)); }
    }

    @Override public Optional<Order> findById(long id){ return Optional.ofNullable(byId.get(id)); }
    @Override public Order save(Order o){
        if (o.id == 0) o.id = idGen.getAndIncrement();
        byId.put(o.id, o);
        flush();
        return o;
    }

    @Override public Page<Order> listByBuyer(long buyerId, PageRequest page){

    }

    @Override public List<Order> listByBuyer(long buyerId){
        var out = new ArrayList<Order>();
        for (var o: byId.values()) if (o.buyerId == buyerId) out.add(o);
        return out;
    }

    private void flush(){ JsonLines.writeAll(file, byId.values()); }
}
