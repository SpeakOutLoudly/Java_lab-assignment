package com.study.cli.support;

// 处理输入，分发指令

public final class TextIO {
    private TextIO() {}

    public static java.util.stream.Stream<String> lines(java.nio.file.Path path)
            throws java.io.IOException {
        return java.nio.file.Files.lines(path, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static String readAll(java.io.InputStream in) throws java.io.IOException {
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(4096);
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) != -1) sb.append(buf, 0, n);
            return sb.toString();
        }
    }

    public static void writeString(java.nio.file.Path path, String content) throws java.io.IOException {
        java.nio.file.Files.writeString(path, content, java.nio.charset.StandardCharsets.UTF_8);
    }
}

