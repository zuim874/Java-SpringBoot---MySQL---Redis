package com.xuwenye.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuwenye.demo.Entity.Coupon;
import com.xuwenye.demo.Entity.Order;
import com.xuwenye.demo.Entity.OrderItem;
import com.xuwenye.demo.Entity.Product;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Entity.UserCoupon;
import com.xuwenye.demo.Mapper.CouponMapper;
import com.xuwenye.demo.Mapper.OrderItemMapper;
import com.xuwenye.demo.Mapper.OrderMapper;
import com.xuwenye.demo.Mapper.ProductMapper;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.Mapper.UserCouponMapper;
import com.xuwenye.demo.Service.CouponService;
import com.xuwenye.demo.Service.OrderService;
import com.xuwenye.demo.Service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单业务层集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.创建订单：库存扣减、订单号生成、金额计算
 * 2.支付/取消/发货/完成/退款：状态流转与余额变动
 * 3.优惠券订单：核销与优惠金额计算
 * 4.非法状态流转拦截
 * <p>
 * @author ZuiM
 */
class OrderServiceTest extends AbstractServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    // ========== 测试数据构造 ==========

    /**
     * 构造测试用户（含充足余额）
     * <p>
     * @author ZuiM
     * @return User 用户
     */
    private User buildUser() {
        User user = new User();
        user.setUsername(uniqueName("ut_order_user"));
        user.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa");
        user.setNickname("订单测试用户" + SUFFIX);
        user.setEmail(uniqueName("uto") + "@test.com");
        user.setStatus(1);
        user.setUserRole("ROLE_USER");
        user.setBalance(new BigDecimal("1000.00"));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    /**
     * 构造测试卖家与商品
     * <p>
     * @author ZuiM
     * @return Product 商品（库存10，单价100）
     */
    private Product buildProduct() {
        Seller seller = new Seller();
        seller.setSellerName("订单测试卖家" + SUFFIX);
        seller.setAddress("测试地址");
        seller.setSellerContact("13800000001");
        seller.setCreateTime(LocalDateTime.now());
        sellerMapper.insert(seller);

        Product product = new Product();
        product.setSellerId(seller.getId());
        product.setProductName("订单测试商品" + SUFFIX);
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(10);
        product.setSold(0);
        product.setStatus(1);
        product.setDescription("订单单元测试");
        product.setCategory("其他");
        product.setMainImageUrl("http://test.com/order_" + SUFFIX + ".png");
        product.setRecommend(0);
        product.setCreateTime(LocalDateTime.now());
        productService.saveProduct(product);
        return product;
    }

    /**
     * 清理订单相关缓存
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @param userId 用户ID
     */
    private void cleanOrderCache(Long orderId, Long userId) {
        trackCacheKey("demo:order:detail:" + orderId);
        trackCacheKey("demo:order:detail:items:" + orderId);
        trackCacheKey("demo:order:user:" + userId + ":page:*");
        trackCacheKey("demo:order:admin:list:*");
        trackCacheKey("demo:order:seller:*");
        trackCacheKey("demo:user:id:" + userId);
        trackCacheKey("demo:user:active:*");
    }

    /**
     * 清理测试数据（订单项/订单/商品/卖家/用户/优惠券）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param product Product 商品
     */
    private void cleanData(Long userId, Long orderId, Product product) {
        if (orderId != null) {
            orderItemMapper.delete(new QueryWrapper<OrderItem>().eq("order_id", orderId));
            orderMapper.deleteById(orderId);
        }
        if (product != null) {
            trackCacheKey("demo:product:detail:" + product.getId());
            productService.deleteProduct(product.getId());
            sellerMapper.deleteById(product.getSellerId());
        }
        if (userId != null) {
            userCouponMapper.delete(new QueryWrapper<UserCoupon>().eq("user_id", userId));
            userMapper.deleteById(userId);
        }
    }

    // ========== 1. 创建订单 ==========

    /**
     * 创建订单：库存扣减、订单金额与状态正确
     * <p>
     * @author ZuiM
     */
    @Test
    void 创建订单并扣减库存() {
        User user = buildUser();
        Product product = buildProduct();

        Order order = orderService.createOrder(
                user.getId(),
                List.of(new OrderService.OrderItemRequest(product.getId(), 2)),
                "张三", "13800000002", "广东省深圳市", "测试订单");
        assertNotNull(order.getId());
        assertNotNull(order.getOrderNo());
        assertEquals(0, order.getStatus());
        assertEquals(0, new BigDecimal("200.00").compareTo(order.getTotalAmount()));

        // 库存已扣减：10 - 2 = 8（直接查库，避免命中旧缓存）
        assertEquals(8, productMapper.findProductById(product.getId()).getStock());

        cleanData(user.getId(), order.getId(), product);
    }

    /**
     * 商品库存不足时下单失败
     * <p>
     * @author ZuiM
     */
    @Test
    void 库存不足下单失败() {
        User user = buildUser();
        Product product = buildProduct();

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(
                user.getId(),
                List.of(new OrderService.OrderItemRequest(product.getId(), 999)),
                "张三", "13800000002", "地址", "测试"));

        cleanData(user.getId(), null, product);
    }

    // ========== 2. 状态流转 ==========

    /**
     * 订单全流程：支付 → 发货 → 完成
     * <p>
     * @author ZuiM
     */
    @Test
    void 支付发货完成全流程() {
        User user = buildUser();
        Product product = buildProduct();
        Order order = orderService.createOrder(
                user.getId(), List.of(new OrderService.OrderItemRequest(product.getId(), 2)),
                "张三", "13800000002", "地址", "测试");

        // 支付：余额 1000 → 800（订单状态直查库，避免命中旧缓存）
        assertTrue(orderService.payOrder(order.getId()));
        assertEquals(1, orderMapper.findOrderById(order.getId()).getStatus());
        assertEquals(0, new BigDecimal("800.00").compareTo(userMapper.selectById(user.getId()).getBalance()));

        // 发货
        assertTrue(orderService.shipOrder(order.getId()));
        assertEquals(2, orderMapper.findOrderById(order.getId()).getStatus());

        // 完成
        assertTrue(orderService.completeOrder(order.getId()));
        assertEquals(3, orderMapper.findOrderById(order.getId()).getStatus());

        cleanData(user.getId(), order.getId(), product);
    }

    /**
     * 取消订单：状态变为已取消，库存恢复
     * <p>
     * @author ZuiM
     */
    @Test
    void 取消订单恢复库存() {
        User user = buildUser();
        Product product = buildProduct();
        Order order = orderService.createOrder(
                user.getId(), List.of(new OrderService.OrderItemRequest(product.getId(), 2)),
                "张三", "13800000002", "地址", "测试");

        assertTrue(orderService.cancelOrder(order.getId()));
        assertEquals(4, orderMapper.findOrderById(order.getId()).getStatus());
        // 库存恢复：10（直接查库，避免命中旧缓存）
        assertEquals(10, productMapper.findProductById(product.getId()).getStock());

        cleanData(user.getId(), order.getId(), product);
    }

    /**
     * 已支付订单取消：退回余额
     * <p>
     * @author ZuiM
     */
    @Test
    void 已支付订单取消退回余额() {
        User user = buildUser();
        Product product = buildProduct();
        Order order = orderService.createOrder(
                user.getId(), List.of(new OrderService.OrderItemRequest(product.getId(), 1)),
                "张三", "13800000002", "地址", "测试");

        orderService.payOrder(order.getId());
        // 支付后余额 900
        assertEquals(0, new BigDecimal("900.00").compareTo(userMapper.selectById(user.getId()).getBalance()));

        orderService.cancelOrder(order.getId());
        // 取消后退回余额 1000
        assertEquals(0, new BigDecimal("1000.00").compareTo(userMapper.selectById(user.getId()).getBalance()));

        cleanData(user.getId(), order.getId(), product);
    }

    /**
     * 申请退款：状态变为已退款，余额退回
     * <p>
     * @author ZuiM
     */
    @Test
    void 已支付订单申请退款() {
        User user = buildUser();
        Product product = buildProduct();
        Order order = orderService.createOrder(
                user.getId(), List.of(new OrderService.OrderItemRequest(product.getId(), 1)),
                "张三", "13800000002", "地址", "测试");

        orderService.payOrder(order.getId());
        assertTrue(orderService.requestRefund(order.getId()));
        assertEquals(5, orderMapper.findOrderById(order.getId()).getStatus());
        // 退款后余额回到 1000
        assertEquals(0, new BigDecimal("1000.00").compareTo(userMapper.selectById(user.getId()).getBalance()));

        cleanData(user.getId(), order.getId(), product);
    }

    // ========== 3. 非法状态流转 ==========

    /**
     * 已完成订单不可取消
     * <p>
     * @author ZuiM
     */
    @Test
    void 已完成订单不可取消() {
        User user = buildUser();
        Product product = buildProduct();
        Order order = orderService.createOrder(
                user.getId(), List.of(new OrderService.OrderItemRequest(product.getId(), 1)),
                "张三", "13800000002", "地址", "测试");
        orderService.payOrder(order.getId());
        orderService.shipOrder(order.getId());
        orderService.completeOrder(order.getId());

        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(order.getId()));

        cleanData(user.getId(), order.getId(), product);
    }

    /**
     * 待支付订单不可发货
     * <p>
     * @author ZuiM
     */
    @Test
    void 待支付订单不可发货() {
        User user = buildUser();
        Product product = buildProduct();
        Order order = orderService.createOrder(
                user.getId(), List.of(new OrderService.OrderItemRequest(product.getId(), 1)),
                "张三", "13800000002", "地址", "测试");

        assertThrows(IllegalArgumentException.class, () -> orderService.shipOrder(order.getId()));

        cleanData(user.getId(), order.getId(), product);
    }

    // ========== 4. 优惠券订单 ==========

    /**
     * 使用满减优惠券下单：金额优惠并核销优惠券
     * <p>
     * @author ZuiM
     */
    @Test
    void 使用优惠券下单() {
        User user = buildUser();
        Product product = buildProduct();

        // 创建满 100 减 20 优惠券并发放
        Coupon coupon = new Coupon();
        coupon.setName("订单满减券" + SUFFIX);
        coupon.setType(1);
        coupon.setDiscountValue(new BigDecimal("20.00"));
        coupon.setMinAmount(new BigDecimal("100.00"));
        coupon.setTotalCount(100);
        coupon.setCreateTime(LocalDateTime.now());
        couponService.createCoupon(coupon);
        couponService.grantToUser(user.getId(), coupon.getId(), 30);
        List<UserCoupon> list = couponService.getAvailableCoupons(user.getId());
        Long ucId = list.stream().filter(uc -> uc.getCouponId().equals(coupon.getId()))
                .findFirst().orElseThrow().getId();

        // 订单原价 200，优惠 20 → 应付 180
        Order order = orderService.createOrder(
                user.getId(), List.of(new OrderService.OrderItemRequest(product.getId(), 2)),
                "张三", "13800000002", "地址", "测试", ucId);
        assertEquals(0, new BigDecimal("180.00").compareTo(order.getTotalAmount()));

        // 优惠券已被核销（状态=1）
        UserCoupon used = userCouponMapper.selectById(ucId);
        assertEquals(1, used.getStatus());
        assertEquals(order.getId(), used.getOrderId());

        couponMapper.deleteById(coupon.getId());
        cleanData(user.getId(), order.getId(), product);
    }

    // ========== 5. 查询 ==========

    /**
     * 用户订单列表与详情查询
     * <p>
     * @author ZuiM
     */
    @Test
    void 用户订单查询() {
        User user = buildUser();
        Product product = buildProduct();
        Order order = orderService.createOrder(
                user.getId(), List.of(new OrderService.OrderItemRequest(product.getId(), 1)),
                "张三", "13800000002", "地址", "测试");

        var page = orderService.getUserOrders(user.getId(), 1, 10, null);
        assertNotNull(page);
        assertTrue(page.getRecords().size() >= 1);

        Order detail = orderService.getOrderDetail(order.getId());
        assertNotNull(detail);
        assertEquals(order.getId(), detail.getId());

        cleanData(user.getId(), order.getId(), product);
    }
}
