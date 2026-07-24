package com.agentcache.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 时间工具类，统一使用北京时间。
 */
public class TimeUtil {

    private TimeUtil() {}

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    /**
     * 获取当前北京时间。
     *
     * @return 北京时间的 LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(BEIJING);
    }

    /**
     * 获取北京时区。
     *
     * @return Asia/Shanghai ZoneId
     */
    public static ZoneId zone() {
        return BEIJING;
    }
}
