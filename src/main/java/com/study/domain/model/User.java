package com.study.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class User {
    private long id;                  // 由仓储生成
    private String username;          // 唯一
    private String passwordHash;      // 仅存哈希
    private Role role;                // BUYER/SELLER/ADMIN
    private boolean enabled;          // 默认 true
    private Instant createdAt;
    private Instant lastLoginAt;      // 可选
    private long version;             // 乐观锁

    public enum Role { BUYER, SELLER, ADMIN }

    /* 工厂方法：创建新用户 */
    public static User createNew(String username, String passwordHash, Role role, Instant now) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username empty");
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("passwordHash empty");
        Objects.requireNonNull(role, "role");
        User u = new User();
        u.username = username;
        u.passwordHash = passwordHash;
        u.role = role;
        u.enabled = true;
        u.createdAt = now;
        u.version = 0; // 新建时版本 0，持久化后变 1
        return u;
    }

    public void validate(){
        if(username == null || username.isBlank()) throw new IllegalArgumentException("username empty");
        if(passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("password empty");
        Objects.requireNonNull(role, "role");
    }

    /* 访问字段 */
    public long     getId() { return this.id; }
    public String   getName() { return this.username; }
    public Role     getRole() { return this.role; }
    public boolean  isEnabled() { return this.enabled; }
    public String   getPasswordHash() { return this.passwordHash; }
    public Instant  getCreatedAt() { return this.createdAt; }
    public Instant  getLastLogin() { return this.lastLoginAt; }


    /* 领域行为 */
    public void     disable() { this.enabled = false; }
    public void     enable() { this.enabled = true; }
    public void     changePasswordHash(String newHash) { if(newHash != null) newHash = this.passwordHash;}
    public void     touchLastLogin(Instant now) { this.lastLoginAt = now; }
    /* 角色判断便捷方法 */
    public boolean  isBuyer(){ return role == Role.BUYER; }
    public boolean  isSeller(){ return role == Role.SELLER; }
    public boolean  isAdmin(){ return role == Role.ADMIN; }

}
