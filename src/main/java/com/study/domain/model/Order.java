package com.study.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class Order {
    private long    id;
    private long    buyerId;
    private long    sellerId;
    private long    productId;      // 商品ID

    private int     qty;            // 数量
    private int     amount;         // 金额
    private Status  status;

    private Instant createdAt;
    private Instant confirmedAt;
    private Instant finishedAt;
    private Instant cancelledAt;

    private long version;           // 乐观锁

    public enum Status {CREATED, PAID, SHIPPED, CONFIRMED, FINISHED, CANCELLED};    // 订单状态

    private Order(){}

    /* 工场方法 */
    public static Order createNew(long buyerId, long sellerId, long productId,
                                  int qty, int unitPriceCents, Instant now) {

        if (buyerId <= 0 || sellerId <= 0 || productId <= 0)
            throw new IllegalArgumentException("ids must be positive");
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
        if (unitPriceCents < 0) throw new IllegalArgumentException("unit price < 0");
        Objects.requireNonNull(now, "now");

        Order o = new Order();
        o.buyerId = buyerId;
        o.sellerId = sellerId;
        o.productId = productId;
        o.qty = qty;
        o.amount = Math.multiplyExact(qty, unitPriceCents); // 溢出即抛异常
        o.status = Status.CREATED;
        o.createdAt = now;
        o.version = 0;
        return o;
    }
    /* 校验 */
    public void validate() {
        if(buyerId <= 0 || sellerId <= 0 || productId <= 0)
            throw new IllegalArgumentException("ids must be positive");
        if(qty <=0 )throw new IllegalArgumentException("qty must be > 0");
        if(amount <0) throw new IllegalArgumentException("amount < 0");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /* 访问字段 */
    public int getAmount() { return amount; }
    public long getId() { return id; }
    public long getBuyerId() { return buyerId; }
    public long getSellerId() { return sellerId; }
    public long getProductId() { return productId; }
    public int getQty() { return qty; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public long getVersion() { return version; }

    /* 领域行为：主要是订单状态转换 */
    //
    public void paid(Instant now){
        requireStatus(Status.CREATED);
        this.status = Status.PAID;
        this.finishedAt = Objects.requireNonNull(now, "now");
    }

    public void shipped(Instant now){
        requireStatus(Status.PAID);
        this.status = Status.SHIPPED;
        this.finishedAt = Objects.requireNonNull(now, "now");
    }

    // 已经发货了，但未收货
    public void confirm(Instant now) {
        requireStatus(Status.SHIPPED);
        this.status = Status.CONFIRMED;
        this.confirmedAt = Objects.requireNonNull(now, "now");
    }
    // 结束订单
    public void finish(Instant now) {
        requireStatus(Status.CONFIRMED);
        this.status = Status.FINISHED;
        this.finishedAt = Objects.requireNonNull(now, "now");
    }

    // 只要没结束 Finish 就可以取消订单
    public void cancel(Instant now) {
        if (this.status == Status.FINISHED)
            throw new IllegalStateException("cannot cancel a finished order");
        if (this.status == Status.CANCELLED) return; // 幂等
        this.status = Status.CANCELLED;
        this.cancelledAt = Objects.requireNonNull(now, "now");
    }

    private void requireStatus(Status expected) {
        if (this.status != expected) {
            throw new IllegalStateException("status must be " + expected + " but is " + this.status);
        }
    }
    /* 其他 */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        if (this.id != 0 && other.id != 0) return this.id == other.id;
        // 未持久化前的退化判等（不强制）：按买家+卖家+商品+时间戳
        return buyerId == other.buyerId
                && sellerId == other.sellerId
                && productId == other.productId
                && Objects.equals(createdAt, other.createdAt);
    }
    @Override public int hashCode() {
        return (id != 0) ? Long.hashCode(id)
                : Objects.hash(buyerId, sellerId, productId, createdAt);
    }
    @Override public String toString() {
        return "Order{id=" + id + ", buyerId=" + buyerId + ", sellerId=" + sellerId +
                ", productId=" + productId + ", qty=" + qty + ", amount=" + amount +
                ", status=" + status + ", createdAt=" + createdAt + ", version=" + version + "}";
    }

    /* 存储 */
    public void attachPersistedIdentity(long id) {
        if (this.id != 0) throw new IllegalStateException("id already set");
        if (id <= 0) throw new IllegalArgumentException("id must be positive");
        this.id = id;
        if (this.version == 0) this.version = 1;
    }

    public void bumpVersion() { this.version += 1; }
}
