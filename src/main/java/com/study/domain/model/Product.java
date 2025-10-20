package com.study.domain.model;

import java.time.Instant;
import java.util.Objects;
import com.study.common.Utils;

public final class Product {
    private long id;                // 商品ID
    private long sellerId;
    private String name;            // 商品名
    private String description;

    private int stock;              // 当前库存
    private int price;
    private Status status;

    private Instant createdAt;
    private Instant updatedAt;
    private long version;           // 乐观锁
    private Product(){};

    public enum Status { DRAFT, ACTIVATE, INACTIVATE, DELETED }

    /* 工厂方法 */
    public static Product createNew(long sellerId, String name, int price, int initStock, Instant now) {
        if (sellerId <= 0) throw new IllegalArgumentException("sellerId must be positive");

        if (price < 0) throw new IllegalArgumentException("price < 0");
        if (initStock < 0) throw new IllegalArgumentException("stock < 0");
        Objects.requireNonNull(now, "now");
        Utils.checkNotBlank(name, "name empty");
        Product p = new Product();
        p.sellerId = sellerId;
        p.name = name.trim();
        p.description = "";
        p.price = price;
        p.stock = initStock;
        p.status = Status.DRAFT;   // 新建为草稿，审核/完善后上架
        p.createdAt = now;
        p.updatedAt = now;
        p.version = 0;
        return p;
    }
    /* 校验 */
    public void validate() {
        if (sellerId <= 0) throw new IllegalArgumentException("sellerId must be positive");
        Utils.checkNotBlank(name, "name empty");
        if (price < 0) throw new IllegalArgumentException("price < 0");
        if (stock < 0) throw new IllegalArgumentException("stock < 0");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
    /* 领域方法 */

    public void rename(String newName, Instant now) {
        Utils.checkNotBlank(newName, "name empty");
        this.name = newName.trim();
        touch(now);
    }

    public void changeDescription(String newDesc, Instant now) {
        this.description = (newDesc == null) ? "" : newDesc.trim();
        touch(now);
    }

    public void changePrice(int newPriceCents, Instant now) {
        if (newPriceCents < 0) throw new IllegalArgumentException("price < 0");
        this.price = newPriceCents;
        touch(now);
    }

    /** 上架：需有非负库存 */
    public void activate(Instant now) {
        if (this.status == Status.DELETED) throw new IllegalStateException("deleted product cannot be activated");
        if (this.stock <= 0) throw new IllegalStateException("cannot activate with zero stock");
        this.status = Status.ACTIVATE;
        touch(now);
    }

    /** 下架（可用于临时不可售） */
    public void deactivate(Instant now) {
        if (this.status == Status.DELETED) return;
        this.status = Status.INACTIVATE;
        touch(now);
    }

    /** 逻辑删除：不可再上架 */
    public void delete(Instant now) {
        this.status = Status.DELETED;
        touch(now);
    }

    /** 增加库存（进货/回滚）*/
    public void increaseStock(int delta, Instant now) {
        if (delta <= 0) throw new IllegalArgumentException("delta must be > 0");
        this.stock = Math.addExact(this.stock, delta);
        touch(now);
    }

    /** 扣减库存（下单占用或发货扣减）*/
    public void decreaseStock(int delta, Instant now) {
        if (delta <= 0) throw new IllegalArgumentException("delta must be > 0");
        if (delta > this.stock) throw new IllegalStateException("insufficient stock");
        this.stock -= delta;
        touch(now);
        // 若需要：库存降到 0 可自动下架
        if (this.stock == 0 && this.status == Status.ACTIVATE) {
            this.status = Status.INACTIVATE;
            touch(now);
        }
    }

    private void touch(Instant now) {
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /* 仓储 */
    public void attachPersistedIdentity(long id) {
        if (this.id != 0) throw new IllegalStateException("id already set");
        if (id <= 0) throw new IllegalArgumentException("id must be positive");
        this.id = id;
        if (this.version == 0) this.version = 1;
    }

    public void bumpVersion() { this.version++; }

    /* 访问字段 */
    public long getId() { return id; }
    public long getSellerId() { return sellerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPriceCents() { return price; }
    public int getStock() { return stock; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    /* 其他 */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        if (this.id != 0 && other.id != 0) return this.id == other.id;
        return sellerId == other.sellerId && Objects.equals(name, other.name) && Objects.equals(createdAt, other.createdAt);
    }

    @Override public int hashCode() {
        return (id != 0) ? Long.hashCode(id) : Objects.hash(sellerId, name, createdAt);
    }

    @Override public String toString() {
        return "Product{id=" + id +
                ", sellerId=" + sellerId +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", version=" + version +
                '}';
    }
}
