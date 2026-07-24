package com.agentcache.cli.output;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 控制 CLI 输出格式的人类/JSON 模式。
 *
 * <p>{@link #setJson(boolean)} 由根命令初始化；子命令直接注入即可，无需关心状态来源。</p>
 */
public class Printer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final boolean[] JSON_FLAG = new boolean[]{false};

    private final boolean json;

    public Printer(boolean json) {
        this.json = json;
    }

    /**
     * 设置全局 JSON 模式（供根命令读取 --json 选项时调用）。
     */
    public static void setJson(boolean json) {
        JSON_FLAG[0] = json;
    }

    /**
     * 当前是否为 JSON 模式。
     */
    public static boolean isJson() {
        return JSON_FLAG[0];
    }

    /**
     * 输出单个对象：人类模式 {@code toString}，JSON 模式序列化。
     */
    public void printObject(Object value) {
        if (json) {
            try {
                System.out.println(MAPPER.writeValueAsString(value));
            } catch (Exception ex) {
                System.out.println("null");
            }
        } else if (value != null) {
            System.out.println(value.toString());
        }
    }

    /**
     * 输出二维表：人类模式按列对齐输出；JSON 模式按行作为数组输出。
     *
     * @param rows 第一行作为表头
     */
    public void printTable(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        if (json) {
            try {
                System.out.println(MAPPER.writeValueAsString(rows));
            } catch (Exception ex) {
                System.out.println("[]");
            }
            return;
        }
        int cols = rows.get(0).length;
        int[] widths = new int[cols];
        for (String[] row : rows) {
            for (int i = 0; i < cols && i < row.length; i++) {
                int len = row[i] == null ? 0 : row[i].length();
                if (len > widths[i]) {
                    widths[i] = len;
                }
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                if (c > 0) {
                    sb.append("  ");
                }
                String cell = c < row.length && row[c] != null ? row[c] : "";
                sb.append(pad(cell, widths[c]));
            }
            System.out.println(sb);
            if (i == 0) {
                StringBuilder sep = new StringBuilder();
                for (int c = 0; c < cols; c++) {
                    if (c > 0) {
                        sep.append("  ");
                    }
                    sep.append("-".repeat(widths[c]));
                }
                System.out.println(sep);
            }
        }
    }

    /**
     * 错误输出统一走 stderr，JSON 模式下结构化为 {@code {"error":"..."}}。
     */
    public void printError(String message) {
        if (json) {
            try {
                System.err.println(MAPPER.writeValueAsString(new ErrorPayload(message)));
            } catch (Exception ex) {
                System.err.println("{\"error\":\"" + escape(message) + "\"}");
            }
        } else {
            System.err.println("Error: " + message);
        }
    }

    private static String pad(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s);
        for (int i = s.length(); i < width; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ErrorPayload {
        private String error;
    }
}