package com.study.security;

import com.study.domain.model.User;

public class Session {
    private User current;

    /** 登录：记录当前用户 */
    public void login(User user) {
        if (user == null || !user.isEnabled()) {
            throw new RuntimeException("用户不存在或已被禁用");
        }
        this.current = user;
    }

    /** 退出登录 */
    public void logout() { this.current = null; }

    /** 是否已登录 */
    public boolean isLogin() { return current != null; }

    /** 获取当前用户；未登录抛错（命令里常用） */
    public User requireLogin() {
        if (!isLogin()) throw new RuntimeException("未登录，请先执行 auth login");
        return current;
    }

    /** 便捷鉴权 */
    public boolean ensureBuyer() {
        var u = requireLogin();
        if (u.getRole() != User.Role.BUYER) throw new RuntimeException("需要买家身份");
        return true;
    }
    public boolean ensureSeller() {
        var u = requireLogin();
        if (u.getRole() != User.Role.SELLER) throw new RuntimeException("需要卖家身份");
        return true;
    }
    public boolean ensureAdmin() {
        var u = requireLogin();
        if (u.getRole() != User.Role.ADMIN) throw new RuntimeException("需要管理员身份");
        return true;
    }

    /** 工具：当前用户ID（常用于用例调用） */
    public long currentUserId() { return requireLogin().getId(); }

    /** whoami 展示用 */
    public String who() {
        return isLogin() ? (current.getUserName() + " [" + current.getRole() + "]") : "未登录";
    }
}
