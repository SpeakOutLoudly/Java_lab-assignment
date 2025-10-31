package com.study.domain.repository;

import com.study.domain.model.Review;
import com.study.domain.common.Page;
import com.study.domain.common.PageRequest;

import java.util.Optional;

public interface ReviewRepository {
    Optional<Review> findById(long id);

    /** 同一订单通常只允许一条评价 */
    Optional<Review> findByOrderId(long orderId);

    /** 某商品下的评价分页列表（前台通常只展示 PUBLISHED） */
    Page<Review> listByProduct(long productId, PageRequest page);

    /** 某买家的评价分页列表 */
    Page<Review> listByBuyer(long buyerId, PageRequest page);

    /** 创建评价（初始状态 PENDING），返回带 id/version 的评价 */
    Review save(Review review); // 新建或持久化，练习版可不抛并发异常

    /** 修改状态（PENDING/PUBLISHED/HIDDEN/DELETED），带版本比较，失败返回 false */
    boolean updateStatus(long reviewId, Review.Status to, long expectedVersion);

    /** 修改文本/星级/情感等字段，带版本比较，失败返回 false */
    boolean saveChanges(Review review, long expectedVersion);

    /** 是否已对该订单评价（同单唯一约束） */
    boolean existsByOrderId(long orderId);
}
