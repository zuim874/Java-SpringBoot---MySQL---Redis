package com.xuwenye.demo.util.order;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单号生成工具
 * 1.生成规则：yyyyMMddHHmmss + 6位随机数 + 4位自增序列
 * 2.自增序列基于 JVM 进程内 AtomicInteger，多实例部署时随机数提供区分度
 * 3.长度：26位（20位时间戳 + 6位随机数）
 * <p>
 * @author ZuiM
 */
public class OrderNoGenerator {

    /** 时间戳格式：yyyyMMddHHmmssSSS */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 进程内自增序列（保证同一毫秒内生成的订单号不重复） */
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    /** 自增序列最大值（4位数字：0000~9999） */
    private static final int MAX_SEQUENCE = 9999;

    /**
     * 生成唯一订单号
     * 规则：yyyyMMddHHmmssSSS + 4位随机数 + 4位自增序列
     * 示例：2026081114302512345678
     * <p>
     * @author ZuiM
     * @return String 26位唯一订单号
     */
    public static String generate() {
        // 1. 当前时间戳（精确到毫秒）
        String timestamp = LocalDateTime.now().format(FORMATTER);

        // 2. 4位随机数（1000~9999）
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);

        // 3. 4位自增序列（循环使用 0~9999）
        int seq = SEQUENCE.getAndIncrement();
        if (seq > MAX_SEQUENCE) {
            SEQUENCE.compareAndSet(MAX_SEQUENCE + 1, 0);
            seq = SEQUENCE.getAndIncrement();
        }

        // 4. 拼接：时间戳(17位) + 随机数(4位) + 序列(4位) = 25位
        return timestamp + String.format("%04d", random) + String.format("%04d", seq);
    }
}