package com.study.domain.model;

import java.time.Instant;
import java.util.Objects;

/** 领域模型：商品评价 */
public final class Review {

    /** 情感标签（可选，便于检索/统计） */
    public enum Sentiment { GOOD, BAD, NORMAL }

    /** 审核/可见性状态 */
    public enum Status { PENDING, PUBLISHED, HIDDEN, DELETED }

    private long id;              // 仓储生成（0 表示未持久化）
    private long productId;       // 被评价的商品
    private long orderId;         // 关联订单（通常需已完成）
    private long buyerId;         // 评价人（买家）

    private int rating;           // 星级：1..5
    private String comment;       // 文本内容（可为空字符串但不为 null）
    private Sentiment sentiment;  // GOOD/BAD/NORMAL（可由前台选择或后台计算）
    private Status status;        // 审核与展示状态

    private int helpfulCount;     // “有用”/点赞计数（>=0）

    private Instant createdAt;
    private Instant updatedAt;

    private long version;         // 乐观锁

    private Review() {}

    /* 工厂 */
    public static Review createNew(long buyerId, long productId, long orderId,
                                   int rating, String comment,
                                   Sentiment sentiment, Instant now) {
        if (buyerId <= 0 || productId <= 0 || orderId <= 0)
            throw new IllegalArgumentException("ids must be positive");
        checkRating(rating);
        Objects.requireNonNull(now, "now");
        if (sentiment == null) sentiment = Sentiment.NORMAL;

        Review r = new Review();
        r.buyerId = buyerId;
        r.productId = productId;
        r.orderId = orderId;
        r.rating = rating;
        r.comment = (comment == null) ? "" : comment.trim();
        r.sentiment = sentiment;
        r.status = Status.PENDING;       // 新建待审核
        r.helpfulCount = 0;
        r.createdAt = now;
        r.updatedAt = now;
        r.version = 0;
        return r;
    }

    /* 校验 */
    public void validate() {
        if (buyerId <= 0 || productId <= 0 || orderId <= 0)
            throw new IllegalArgumentException("ids must be positive");
        checkRating(rating);
        Objects.requireNonNull(sentiment, "sentiment");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (helpfulCount < 0) throw new IllegalArgumentException("helpfulCount < 0");
        if (comment == null) throw new IllegalArgumentException("comment null");
    }

    /* 领域 */
    /** 审核通过并发布 */
    public void publish(Instant now) {
        if (status == Status.DELETED) throw new IllegalStateException("deleted review cannot be published");
        this.status = Status.PUBLISHED;
        touch(now);
    }

    /** 隐藏（违规或纠纷处理中） */
    public void hide(Instant now) {
        if (status == Status.DELETED) return;
        this.status = Status.HIDDEN;
        touch(now);
    }

    /** 逻辑删除（不可恢复展示） */
    public void delete(Instant now) {
        this.status = Status.DELETED;
        touch(now);
    }

    /** 买家在“可编辑期”内修改文本（是否允许由应用服务控制时机） */
    public void editComment(String newComment, Instant now) {
        if (status == Status.DELETED) throw new IllegalStateException("deleted review cannot be edited");
        this.comment = (newComment == null) ? "" : newComment.trim();
        touch(now);
    }

    /** 修改星级（是否允许修改由业务策略决定） */
    public void changeRating(int newRating, Instant now) {
        if (status == Status.DELETED) throw new IllegalStateException("deleted review cannot be edited");
        checkRating(newRating);
        this.rating = newRating;
        touch(now);
    }

    /** 设置/更新情感标签（可由算法或运营修改） */
    public void setSentiment(Sentiment newSentiment, Instant now) {
        this.sentiment = Objects.requireNonNull(newSentiment, "sentiment");
        touch(now);
    }

    /** 点赞/有用计数（幂等性与防刷在上层处理） */
    public void increaseHelpful(int delta, Instant now) {
        if (delta <= 0) throw new IllegalArgumentException("delta must be > 0");
        this.helpfulCount = Math.addExact(this.helpfulCount, delta);
        touch(now);
    }

    private static void checkRating(int r) {
        if (r < 1 || r > 5) throw new IllegalArgumentException("rating must be in [1,5]");
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

    /* 访问 */
    public long getId() { return id; }
    public long getProductId() { return productId; }
    public long getOrderId() { return orderId; }
    public long getBuyerId() { return buyerId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Sentiment getSentiment() { return sentiment; }
    public Status getStatus() { return status; }
    public int getHelpfulCount() { return helpfulCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    /* 相等性 */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review other)) return false;
        if (this.id != 0 && other.id != 0) return this.id == other.id;
        // 未持久化前的退化判等：同买家+订单+商品同一时间创建
        return buyerId == other.buyerId
                && orderId == other.orderId
                && productId == other.productId
                && Objects.equals(createdAt, other.createdAt);
    }

    @Override public int hashCode() {
        return (id != 0) ? Long.hashCode(id) : Objects.hash(buyerId, orderId, productId, createdAt);
    }

    @Override public String toString() {
        return "Review{id=" + id +
                ", productId=" + productId +
                ", orderId=" + orderId +
                ", buyerId=" + buyerId +
                ", rating=" + rating +
                ", sentiment=" + sentiment +
                ", status=" + status +
                ", helpfulCount=" + helpfulCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", version=" + version +
                '}';
    }
}
