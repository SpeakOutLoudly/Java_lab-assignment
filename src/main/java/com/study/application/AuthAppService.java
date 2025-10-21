package com.study.application;

import com.study.domain.model.User;
import com.study.domain.repository.UserRepository;
import com.study.security.PasswordHasher;

import java.time.Instant;

// AuthAppService.java
public class AuthAppService {
    private final UserRepository users;
    private final PasswordHasher hasher;
    public AuthAppService(UserRepository users, PasswordHasher hasher){
        this.users = users; this.hasher = hasher;
    }

    public User authenticate(String username, String rawPassword){
        // 认证，等于登录
        var u = users.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!u.isEnabled()) throw new RuntimeException("用户被禁用");
        if (!hasher.matches(rawPassword, u.getPasswordHash()))
            throw new RuntimeException("密码错误");
        return u; // 返回通过认证的用户，由上层决定是否写入会话
    }

    public void changePassword(long userId, String oldPwd, String newPwd, Instant now) {
        User u = users.findById(userId).orElseThrow();
        if (!hasher.matches(oldPwd, u.getPasswordHash()))
            throw new IllegalArgumentException("old password mismatch");
        String newHash = hasher.hash(newPwd);
        u.changePasswordHash(newHash);   // 领域对象只改“hash”
        users.save(u);                // 乐观锁校验 + version++
    }
}
