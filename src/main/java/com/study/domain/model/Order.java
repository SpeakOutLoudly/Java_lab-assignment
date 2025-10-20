package com.study.domain.model;

public class Order {
    private long id;
    private String buyerId;
    private String sellerId;
    private String productId;
    private oStatus Status;
    private String createTime;
    private int amount;

    public enum oStatus {onsell, soldout, finished};

    public int getAmount() { return amount; }

    public oStatus getStatus() {return this.Status;}
    public long getId() { return this.id; }
}
