package com.study.infra;

import com.study.domain.model.User;
import com.study.domain.repository.UserRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InMemUserRepository implements UserRepository {
    private final Map<Long, User> table = new HashMap<>();
    private final Map<String, Long> usernameIndex = new HashMap<>();
    private final AtomicLong seq = new AtomicLong(0);

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(table.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Long id = usernameIndex.get(username);
        return id == null ? Optional.empty() : Optional.ofNullable(table.get(id));
    }

    @Override
    public boolean existsByUsername(String username) {
        return usernameIndex.containsKey(username);
    }

    @Override
    public User save(User user) {
        user.validate();

        if (user.getId() == 0) { // 新建
            if (existsByUsername(user.getUserName()))
                throw new IllegalStateException("username exists: " + user.getUserName());
            long id = seq.incrementAndGet();
            user.attachPersistedIdentity(id); // version: 0 -> 1
            table.put(id, user);
            usernameIndex.put(user.getUserName(), id);
            return user;
        } else { // 更新 + 乐观锁
            User current = table.get(user.getId());
            if (current == null) throw new NoSuchElementException("user not found: id=" + user.getId());
            if (current.getVersion() != user.getVersion())
                throw new IllegalStateException("optimistic lock failed, expect v="
                        + current.getVersion() + " but got " + user.getVersion());

            // 若允许修改用户名，需要维护唯一索引
            if (!Objects.equals(current.getUserName(), user.getUserName())) {
                if (existsByUsername(user.getUserName()))
                    throw new IllegalStateException("username exists: " + user.getUserName());
                usernameIndex.remove(current.getUserName());
                usernameIndex.put(user.getUserName(), user.getId());
            }

            user.bumpVersion(); // 写库成功后递增版本
            table.put(user.getId(), user);
            return user;
        }
    }

    @Override
    public void deleteById(long id) {
        User removed = table.remove(id);
        if (removed != null) {
            usernameIndex.remove(removed.getUserName());
        }
    }

    @Override
    public List<User> list(int page, int size, Comparator<User> sort) {
        return table.values().stream()
                .sorted(sort != null ? sort : Comparator.comparing(User::getId))
                .skip(Math.max(0L, (long) (page - 1) * size))
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> query(Predicate<User> filter, int page, int size, Comparator<User> sort) {
        Predicate<User> f = (filter != null) ? filter : u -> true;
        return table.values().stream()
                .filter(f)
                .sorted(sort != null ? sort : Comparator.comparing(User::getId))
                .skip(Math.max(0L, (long) (page - 1) * size))
                .limit(size)
                .collect(Collectors.toList());
    }
}
