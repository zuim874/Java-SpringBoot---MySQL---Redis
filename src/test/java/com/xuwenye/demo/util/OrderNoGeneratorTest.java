package com.xuwenye.demo.util;

import com.xuwenye.demo.util.order.OrderNoGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单号生成工具单元测试（纯单元测试，无需外部依赖）
 * 1.格式：25 位纯数字（yyyyMMddHHmmssSSS 17位 + 4位随机 + 4位自增）
 * 2.唯一性：批量生成无重复
 * 3.前缀：以当前日期开头，便于对账
 * <p>
 * @author ZuiM
 */
class OrderNoGeneratorTest {

    /**
     * 订单号格式：25 位纯数字
     * <p>
     * @author ZuiM
     */
    @Test
    void 订单号为二十五位纯数字() {
        String orderNo = OrderNoGenerator.generate();
        assertEquals(25, orderNo.length());
        assertTrue(orderNo.matches("\\d{25}"));
    }

    /**
     * 批量生成 10000 个订单号无重复
     * <p>
     * @author ZuiM
     */
    @Test
    void 批量生成无重复() {
        Set<String> orderNos = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            orderNos.add(OrderNoGenerator.generate());
        }
        assertEquals(10000, orderNos.size());
    }

    /**
     * 订单号以当前年份开头（yyyyMMdd 前缀）
     * <p>
     * @author ZuiM
     */
    @Test
    void 订单号以当前日期开头() {
        String orderNo = OrderNoGenerator.generate();
        String today = java.time.LocalDate.now().toString().replace("-", "");
        assertTrue(orderNo.startsWith(today));
    }
}
