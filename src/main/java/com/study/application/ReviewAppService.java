package com.study.application;

import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;
import com.study.domain.model.Review;
import com.study.domain.repository.ReviewRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Review 应用服务（基于 ReviewRepository 的用例封装）
 *
 * 职责：
 * - 创建评价（同单唯一）
 * - 按商品/买家分页查看
 * - 编辑评价内容（文本/星级/情感）
 * - 审核与可见性（发布/隐藏/删除）
 *
 * 说明：
 * - 这里不做并发控制；版本使用从仓储读出的 review.getVersion() 传入校验；
 * - 是否需要校验“订单是否可评价”等业务，可在上层调用前完成，或在此处注入 OrderRepository 做校验。
 */
public class ReviewAppService {

    private final ReviewRepository reviews;

    public ReviewAppService(ReviewRepository reviews) {
        this.reviews = Objects.requireNonNull(reviews);
    }

    /* ============ 查询 ============ */

    public Optional<Review> findById(long reviewId) {
        return reviews.findById(reviewId);
    }

    public Optional<Review> findByOrderId(long orderId) {
        return reviews.findByOrderId(orderId);
    }

    public Page<Review> listByProduct(long productId, PageRequest page) {
        return reviews.listByProduct(productId, page);
    }

    public Page<Review> listByBuyer(long buyerId, PageRequest page) {
        return reviews.listByBuyer(buyerId, page);
    }

    /* ============ 创建 ============ */

    /**
     * 创建新评价（默认 PENDING）
     * - 同一订单只允许一条评价
     * - rating 区间与字段合法性由 Review.createNew 校验
     */
    public Review create(long buyerId, long productId, long orderId,
                         int rating, String comment, Review.Sentiment sentiment) {
        if (reviews.existsByOrderId(orderId)) {
            throw new RuntimeException("该订单已评价，无法重复评价");
        }
        var now = Instant.now();
        var r = Review.createNew(buyerId, productId, orderId, rating, comment, sentiment, now);
        return reviews.save(r); // 持久化后获得 id/version
    }

    /* ============ 编辑（文本/星级/情感） ============ */

    public Review editComment(long reviewId, String newComment) {
        var r = reviews.findById(reviewId).orElseThrow(() -> new RuntimeException("评价不存在: " + reviewId));
        long ver = r.getVersion();
        r.editComment(newComment, Instant.now());
        if (!reviews.saveChanges(r, ver)) {
            throw new RuntimeException("保存失败：版本不匹配或评价不存在");
        }
        return reviews.findById(reviewId).orElse(r);
    }

    public Review changeRating(long reviewId, int newRating) {
        var r = reviews.findById(reviewId).orElseThrow(() -> new RuntimeException("评价不存在: " + reviewId));
        long ver = r.getVersion();
        r.changeRating(newRating, Instant.now());
        if (!reviews.saveChanges(r, ver)) {
            throw new RuntimeException("保存失败：版本不匹配或评价不存在");
        }
        return reviews.findById(reviewId).orElse(r);
    }

    public Review setSentiment(long reviewId, Review.Sentiment sentiment) {
        var r = reviews.findById(reviewId).orElseThrow(() -> new RuntimeException("评价不存在: " + reviewId));
        long ver = r.getVersion();
        r.setSentiment(Objects.requireNonNull(sentiment, "sentiment"), Instant.now());
        if (!reviews.saveChanges(r, ver)) {
            throw new RuntimeException("保存失败：版本不匹配或评价不存在");
        }
        return reviews.findById(reviewId).orElse(r);
    }

    /* ============ 审核/可见性 ============ */

    /** 发布（审核通过） */
    public boolean publish(long reviewId) {
        var r = reviews.findById(reviewId).orElseThrow(() -> new RuntimeException("评价不存在: " + reviewId));
        return reviews.updateStatus(reviewId, Review.Status.PUBLISHED, r.getVersion());
    }

    /** 隐藏 */
    public boolean hide(long reviewId) {
        var r = reviews.findById(reviewId).orElseThrow(() -> new RuntimeException("评价不存在: " + reviewId));
        return reviews.updateStatus(reviewId, Review.Status.HIDDEN, r.getVersion());
    }

    /** 逻辑删除（不可恢复展示） */
    public boolean delete(long reviewId) {
        var r = reviews.findById(reviewId).orElseThrow(() -> new RuntimeException("评价不存在: " + reviewId));
        return reviews.updateStatus(reviewId, Review.Status.DELETED, r.getVersion());
    }
}
