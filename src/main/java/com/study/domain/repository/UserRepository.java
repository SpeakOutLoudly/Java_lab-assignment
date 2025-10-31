package com.study.domain.repository;

import com.study.domain.model.User;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface UserRepository {
    // —— 读取 ——
    Optional<User> findById(long id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    // —— 保存/删除 ——
    /**
     * 保存用户：
     * - user.getId()==0 视为新建：分配 id，并把 version 从 0 → 1
     * - user.getId()!=0 视为更新：做乐观锁（比较 version），成功后 version+1
     */
    User save(User user);

    void deleteById(long id);
    // 将原来的修改密码放在service层
    // —— 列表/查询（用于测试/演示）——
    List<User> list(int page, int size, Comparator<User> sort);
    List<User> query(Predicate<User> filter, int page, int size, Comparator<User> sort);
}
