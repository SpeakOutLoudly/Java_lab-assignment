package com.study.common;

public class Utils {
    // 检验 name 等字段为非空
    // 写成全静态方法类
    private Utils(){};
    public static void checkNotBlank(String s, String msg) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(msg);
    }
}
