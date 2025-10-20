package com.study.application;

import com.study.domain.model.User;
import com.study.domain.repository.UserRepository;
import com.study.security.PasswordHasher;

// AuthAppService.java
public class AuthAppService {
    private final UserRepository users;
    private final PasswordHasher hasher;
    public AuthAppService(UserRepository users, PasswordHasher hasher){
        this.users = users; this.hasher = hasher;
    }

    public User authenticate(String username, String rawPassword){
        // 认证，等于登录
        var u = users.findByName(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!u.isEnabled()) throw new RuntimeException("用户被禁用");
        if (!hasher.matches(rawPassword, u.getPasswordHash()))
            throw new RuntimeException("密码错误");
        return u; // 返回通过认证的用户，由上层决定是否写入会话
    }
}
