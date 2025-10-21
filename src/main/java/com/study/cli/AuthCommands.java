package com.study.cli;

import com.study.application.AuthAppService;
import picocli.CommandLine.*;
import com.study.domain.model.User;
import com.study.security.Session;
import com.study.domain.model.User;

// 主要使用 session 的方法，负责处理本次会话
// 用于登录
@Command(name = "auth", description = "用户登录",
        subcommands = {User.class, AuthCommands.Login.class,
        AuthCommands.Whoami.class, AuthCommands.Logout.class})
public class AuthCommands implements Runnable {
    public void run(){ System.out.println("子命令：login / whoami / logout"); }

    private final Session session;
    private final AuthAppService auth;
    public AuthCommands(Session s, AuthAppService a){ this.session=s; this.auth=a; }

    @Command(name="login", description="登录（演示版只凭用户名）")
    static class Login implements Runnable {
        @Option(names="--u", required=true, description="用户名（alice/bob/sam/tom）")
        String username;

        @Option(names = "--p", required = true, description = "密码", interactive = true)
        String password;
        private final Session session; private final AuthAppService auth;
        public Login(Session s, AuthAppService a){ this.session=s; this.auth=a; }

        public void run() {
            var u = auth.authenticate(username, password).orElse(null);
            if (u == null) System.out.println("登录失败/用户不存在或被禁用");
            else { session.login(u); System.out.printf("欢迎 %s [%s]%n", u.getUserName(), u.getRole()); }
        }
    }

    @Command(name="whoami", description="当前登录用户")
    static class Whoami implements Runnable {
        private final Session session;
        public Whoami(Session s){ this.session = s; }
        public void run() {
            var u = session.requireLogin();
            System.out.println(u==null ? "未登录" : (u.getUserName() + " [" + u.getRole() + "]"));
        }
    }

    @Command(name="logout", description="退出登录")
    static class Logout implements Runnable {
        private final Session session;
        public Logout(Session s){ this.session=s; }
        public void run() { session.logout(); System.out.println("已退出登录"); }
    }
}