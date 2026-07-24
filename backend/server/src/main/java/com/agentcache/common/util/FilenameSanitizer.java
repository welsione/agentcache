package com.agentcache.common.util;

import java.util.regex.Pattern;

/**
 * 文件名消毒工具：去除路径分隔符、控制字符与引号，避免路径遍历与响应头注入。
 */
public final class FilenameSanitizer {

    private static final Pattern UNSAFE = Pattern.compile("[\\\\/\\u0000-\\u001F\\u007F\"<>|:*?\\u202E]");

    private FilenameSanitizer() {
    }

    /**
     * 返回安全的文件名。空字符串或仅含非法字符时回退为 {@code download.bin}。
     *
     * @param raw 原始文件名
     * @return 安全的文件名
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "download.bin";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "download.bin";
        }
        String cleaned = UNSAFE.matcher(trimmed).replaceAll("_");
        // 防止全部字符被替换为空字符串。
        if (cleaned.isBlank() || cleaned.equals(".") || cleaned.equals("..")) {
            return "download.bin";
        }
        return cleaned;
    }
}