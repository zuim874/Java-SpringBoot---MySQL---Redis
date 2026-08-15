package com.xuwenye.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Order;
import com.xuwenye.demo.Entity.OrderItem;
import com.xuwenye.demo.Entity.Product;
import com.xuwenye.demo.Mapper.OrderItemMapper;
import com.xuwenye.demo.Mapper.OrderMapper;
import com.xuwenye.demo.util.order.OrderNoGenerator;
import com.xuwenye.demo.util.redis.RedisLockHelper;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单业务层
 * 1.创建订单：分布式锁扣库存，生成订单号，写入订单与订单项
 * 2.支付/取消/退款：状态流转 + 缓存维护
 * 3.查询：用户订单列表/详情，含 Redis 缓存（Cache-Aside）
 * 4.管理员操作：发货/完成/订单列表
 * <p>
 * @author ZuiM
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductService productService;
    private final RedisUtil redisUtil;
    private final RedisLockHelper redisLockHelper;
    private final UserService userService;
    private final MQProducer mqProducer;
    private final CouponService couponService;

    // Redis 缓存 Key 前缀
    private static final String ORDER_CACHE_PREFIX = "demo:order:";
    private static final String ORDER_DETAIL_CACHE_PREFIX = "demo:order:detail:";
    private static final String ORDER_USER_LIST_CACHE_PREFIX = "demo:order:user:";
    private static final String ORDER_ADMIN_LIST_CACHE_PREFIX = "demo:order:admin:list:";

    public OrderService(OrderMapper orderMapper,
                        OrderItemMapper orderItemMapper,
                        ProductService productService,
                        RedisUtil redisUtil,
                        RedisLockHelper redisLockHelper,
                        UserService userService,
                        MQProducer mqProducer,
                        CouponService couponService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productService = productService;
        this.redisUtil = redisUtil;
        this.redisLockHelper = redisLockHelper;
        this.userService = userService;
        this.mqProducer = mqProducer;
        this.couponService = couponService;
    }

    /**
     * 创建订单（分布式锁保护库存扣减）
     * 1.校验商品库存
     * 2.扣减库存（分布式锁）
     * 3.生成订单号
     * 4.保存订单与订单项
     * 5.清除相关缓存
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param orderItems 订单项信息（商品ID、数量）
     * @param receiverName 收货人姓名
     * @param receiverPhone 收货人电话
     * @param receiverAddress 收货地址
     * @param remark 订单备注
     * @return Order 创建成功的订单（含ID和订单号）
     */
    @Transactional
    public Order createOrder(Long userId,
                             List<OrderItemRequest> orderItems,
                             String receiverName,
                             String receiverPhone,
                             String receiverAddress,
                             String remark) {
        return createOrder(userId, orderItems, receiverName, receiverPhone,
                receiverAddress, remark, null);
    }

    /**
     * 创建订单（支持优惠券，分布式锁保护库存扣减）
     * 1.校验商品库存、扣减库存（分布式锁）
     * 2.若携带优惠券：校验归属/状态/过期/门槛，计算优惠金额，下单后原子核销（失败则整体回滚）
     * 3.生成订单号，写入订单（含优惠后的应付金额）与订单项
     * 4.异步刷新缓存
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param orderItems 订单项信息（商品ID、数量）
     * @param receiverName 收货人姓名
     * @param receiverPhone 收货人电话
     * @param receiverAddress 收货地址
     * @param remark 订单备注
     * @param userCouponId 用户优惠券ID（可空，不使用优惠券）
     * @return Order 创建成功的订单（含ID和订单号）
     */
    @Transactional
    public Order createOrder(Long userId,
                             List<OrderItemRequest> orderItems,
                             String receiverName,
                             String receiverPhone,
                             String receiverAddress,
                             String remark,
                             Long userCouponId) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("订单项不能为空");
        }

        // 1. 计算总金额并扣减库存
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 缓存本轮已查询的商品，供第 4 步写入订单项时复用，避免同一商品重复查库/查缓存
        List<Product> products = new ArrayList<>(orderItems.size());

        for (OrderItemRequest item : orderItems) {
            // 查询商品
            Product product = productService.getProductById(item.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("商品不存在，ID：" + item.getProductId());
            }
            if (product.getStatus() != 1) {
                throw new IllegalArgumentException("商品已下架：" + product.getProductName());
            }
            products.add(product);

            // 扣减库存（分布式锁保护）
            boolean deducted = productService.deductStock(item.getProductId(), item.getQuantity());
            if (!deducted) {
                throw new IllegalArgumentException("商品库存不足：" + product.getProductName());
            }

            // 计算小计
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        // 2. 优惠券计算（先校验，下单后核销）
        BigDecimal discount = BigDecimal.ZERO;
        if (userCouponId != null) {
            discount = couponService.validateAndCalcDiscount(userCouponId, userId, totalAmount);
        }
        BigDecimal payable = totalAmount.subtract(discount);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            payable = BigDecimal.ZERO;
        }

        // 3. 生成订单号并保存订单
        String orderNo = OrderNoGenerator.generate();

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setTotalAmount(payable);
        order.setStatus(0); // 待支付
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        if (remark != null) {
            // 附带优惠信息到备注（便于对账）
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                order.setRemark(remark + " | 优惠券减免 ¥" + discount.stripTrailingZeros().toPlainString());
            } else {
                order.setRemark(remark);
            }
        }

        int orderResult = orderMapper.insert(order);
        if (orderResult <= 0) {
            throw new RuntimeException("订单创建失败");
        }

        // 4. 保存订单项（复用第 1 步已缓存的商品对象，避免重复查询）
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItemRequest item = orderItems.get(i);
            Product product = products.get(i);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getProductName());
            orderItem.setProductImage(product.getMainImageUrl());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

            orderItemMapper.insert(orderItem);
        }

        // 5. 核销优惠券（原子操作；失败则抛异常回滚整个订单）
        if (userCouponId != null) {
            couponService.markUsed(userCouponId, userId, order.getId());
        }

        // 6. 异步发送缓存刷新任务和订单创建消息
        sendOrderCacheRefreshTask(order.getId(), userId);
        mqProducer.sendOrderTask(order.getId(), "PROCESS");

        return order;
    }

    /**
     * 支付订单（模拟支付）
     * 1.校验订单状态为待支付
     * 2.更新状态为已支付
     * 3.清除缓存
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @return boolean true=支付成功
     */
    @Transactional
    public boolean payOrder(Long orderId) {
        Order order = orderMapper.findOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new IllegalArgumentException("订单状态不允许支付，当前状态：" + order.getStatus());
        }

        // 真实扣减余额（分布式锁 + 原子 SQL 防超扣）
        boolean deducted = userService.deductBalance(order.getUserId(), order.getTotalAmount());
        if (!deducted) {
            throw new IllegalArgumentException("余额不足，请先充值");
        }

        Order update = new Order();        update.setId(orderId);
        update.setStatus(1); // 已支付
        update.setPayTime(LocalDateTime.now());
        boolean result = orderMapper.updateById(update) > 0;

        if (result) {
            // 异步发送缓存刷新任务和订单支付消息
            sendOrderCacheRefreshTask(orderId, order.getUserId());
            mqProducer.sendOrderTask(orderId, "PAYMENT");
        }
        return result;
    }

    /**
     * 取消订单（恢复库存）
     * 1.校验订单状态（待支付/已支付可取消）
     * 2.恢复商品库存
     * 3.更新订单状态为已取消
     * 4.清除缓存
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @return boolean true=取消成功
     */
    @Transactional
    public boolean cancelOrder(Long orderId) {
        Order order = orderMapper.findOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        // 待支付(0)和已支付(1)可以取消
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            throw new IllegalArgumentException("订单状态不允许取消，当前状态：" + order.getStatus());
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.findItemsByOrderId(orderId);
        for (OrderItem item : items) {
            boolean restored = productService.restoreStock(item.getProductId(), item.getQuantity());
            if (!restored) {
                throw new RuntimeException("恢复库存失败，商品ID：" + item.getProductId());
            }
        }

        // 已支付订单取消时退回余额
        if (order.getStatus() == 1) {
            userService.chargeBalance(order.getUserId(), order.getTotalAmount());
        }

        // 更新订单状态
        Order update = new Order();
        update.setId(orderId);
        update.setStatus(4); // 已取消
        update.setCancelTime(LocalDateTime.now());
        boolean result = orderMapper.updateById(update) > 0;

        if (result) {
            // 异步发送缓存刷新任务和订单取消消息
            sendOrderCacheRefreshTask(orderId, order.getUserId());
            mqProducer.sendOrderTask(orderId, "CANCEL");
        }
        return result;
    }

    /**
     * 申请退款
     * 1.校验订单状态为已支付
     * 2.恢复库存
     * 3.更新状态为已退款
     * 4.清除缓存
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @return boolean true=退款成功
     */
    @Transactional
    public boolean requestRefund(Long orderId) {
        Order order = orderMapper.findOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        // 已支付(1)或已发货(2)可申请退款
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new IllegalArgumentException("订单状态不允许退款，当前状态：" + order.getStatus());
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.findItemsByOrderId(orderId);
        for (OrderItem item : items) {
            boolean restored = productService.restoreStock(item.getProductId(), item.getQuantity());
            if (!restored) {
                throw new RuntimeException("恢复库存失败，商品ID：" + item.getProductId());
            }
        }

        // 退款退回余额（1/2 状态均已支付）
        userService.chargeBalance(order.getUserId(), order.getTotalAmount());

        // 更新订单状态
        Order update = new Order();
        update.setId(orderId);
        update.setStatus(5); // 已退款
        update.setCompleteTime(LocalDateTime.now());
        boolean result = orderMapper.updateById(update) > 0;

        if (result) {
            // 异步发送缓存刷新任务和订单退款消息
            sendOrderCacheRefreshTask(orderId, order.getUserId());
            mqProducer.sendOrderTask(orderId, "REFUND");
        }
        return result;
    }

    /**
     * 获取用户订单列表（分页，含 Redis 缓存）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页条数
     * @param status 订单状态筛选（可选，null=不筛选）
     * @return Page<Order> 分页订单列表
     */
    @SuppressWarnings("unchecked")
    public Page<Order> getUserOrders(Long userId, int page, int size, Integer status) {
        // 带状态筛选时不走缓存
        if (status != null) {
            Page<Order> pageObj = new Page<>(page, size);
            QueryWrapper<Order> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                    .eq("is_deleted", 0)
                    .eq("status", status)
                    .orderByDesc("create_time");
            Page<Order> result = orderMapper.selectPage(pageObj, wrapper);
            return result;
        }

        // 无状态筛选时走缓存
        String cacheKey = ORDER_USER_LIST_CACHE_PREFIX + userId + ":page:" + page + ":" + size;

        // 第 1 步：先从 Redis 查
        Page<Order> cached = (Page<Order>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        Page<Order> pageObj = new Page<>(page, size);
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("create_time");
        Page<Order> result = orderMapper.selectPage(pageObj, wrapper);

        // 第 3 步：写入 Redis
        if (result != null && !result.getRecords().isEmpty()) {
            redisUtil.set(cacheKey, result);
        }
        return result;
    }

    /**
     * 获取订单详情（含订单项，含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @return Order 订单（含订单项列表）
     */
    public Order getOrderDetail(Long orderId) {
        String cacheKey = ORDER_DETAIL_CACHE_PREFIX + orderId;

        // 第 1 步：先从 Redis 查
        Order cached = (Order) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        Order order = orderMapper.findOrderById(orderId);
        if (order != null) {
            // 加载订单项
            List<OrderItem> items = orderItemMapper.findItemsByOrderId(orderId);
            // 由于 Order 实体没有 items 字段，使用临时方式传递
            // 此处通过缓存额外存储订单项
            String itemsCacheKey = ORDER_DETAIL_CACHE_PREFIX + "items:" + orderId;
            redisUtil.set(itemsCacheKey, items);

            // 第 3 步：写入 Redis
            redisUtil.set(cacheKey, order);
        }
        return order;
    }

    /**
     * 获取订单的订单项列表
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @return List<OrderItem> 订单项列表
     */
    @SuppressWarnings("unchecked")
    public List<OrderItem> getOrderItems(Long orderId) {
        String itemsCacheKey = ORDER_DETAIL_CACHE_PREFIX + "items:" + orderId;

        // 第 1 步：先从 Redis 查
        List<OrderItem> cached = (List<OrderItem>) redisUtil.get(itemsCacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        List<OrderItem> items = orderItemMapper.findItemsByOrderId(orderId);

        // 第 3 步：写入 Redis
        if (items != null && !items.isEmpty()) {
            redisUtil.set(itemsCacheKey, items);
        }
        return items;
    }

    /**
     * 发货（管理员操作）
     * 1.校验订单状态为已支付
     * 2.更新状态为已发货
     * 3.清除缓存
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @return boolean true=发货成功
     */
    @Transactional
    public boolean shipOrder(Long orderId) {
        Order order = orderMapper.findOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new IllegalArgumentException("订单状态不允许发货，当前状态：" + order.getStatus());
        }

        Order update = new Order();
        update.setId(orderId);
        update.setStatus(2); // 已发货
        update.setShipTime(LocalDateTime.now());
        boolean result = orderMapper.updateById(update) > 0;

        if (result) {
            // 异步发送缓存刷新任务和订单发货消息
            sendOrderCacheRefreshTask(orderId, order.getUserId());
            mqProducer.sendOrderTask(orderId, "SHIP");
        }
        return result;
    }

    /**
     * 完成订单（管理员操作）
     * 1.校验订单状态为已发货
     * 2.更新状态为已完成
     * 3.清除缓存
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @return boolean true=完成成功
     */
    @Transactional
    public boolean completeOrder(Long orderId) {
        Order order = orderMapper.findOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != 2) {
            throw new IllegalArgumentException("订单状态不允许完成，当前状态：" + order.getStatus());
        }

        Order update = new Order();
        update.setId(orderId);
        update.setStatus(3); // 已完成
        update.setCompleteTime(LocalDateTime.now());
        boolean result = orderMapper.updateById(update) > 0;

        if (result) {
            // 异步发送缓存刷新任务和订单完成消息
            sendOrderCacheRefreshTask(orderId, order.getUserId());
            mqProducer.sendOrderTask(orderId, "COMPLETE");
        }
        return result;
    }

    /**
     * 管理员获取订单列表（分页，可按状态筛选）
     * <p>
     * @author ZuiM
     * @param page 页码
     * @param size 每页条数
     * @param status 订单状态（为null则查询全部）
     * @return Page<Order> 分页订单列表
     */
    @SuppressWarnings("unchecked")
    public Page<Order> adminGetOrders(int page, int size, Integer status) {
        String cacheKey = ORDER_ADMIN_LIST_CACHE_PREFIX + "status:" + status + ":page:" + page + ":" + size;

        // 第 1 步：先从 Redis 查
        Page<Order> cached = (Page<Order>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 第 2 步：Redis 没有，查 MySQL
        Page<Order> pageObj = new Page<>(page, size);
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        Page<Order> result = orderMapper.selectPage(pageObj, wrapper);

        // 第 3 步：写入 Redis
        if (result != null && !result.getRecords().isEmpty()) {
            redisUtil.set(cacheKey, result);
        }
        return result;
    }

    /**
     * 分页查询某卖家的订单（含 Redis 缓存）
     * <p>
     * @author ZuiM
     * @param sellerId 卖家ID
     * @param page 页码
     * @param size 每页条数
     * @param status 订单状态（为null则查询全部）
     * @return Page<Order> 分页订单列表
     */
    @SuppressWarnings("unchecked")
    public Page<Order> getSellerOrders(Long sellerId, int page, int size, Integer status) {
        // 带状态筛选时不走缓存
        if (status != null) {
            Page<Order> pageObj = new Page<>(page, size);
            return (Page<Order>) orderMapper.findSellerOrdersPage(pageObj, sellerId, status);
        }
        String cacheKey = "demo:order:seller:" + sellerId + ":page:" + page + ":" + size;
        Page<Order> cached = (Page<Order>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Page<Order> pageObj = new Page<>(page, size);
        Page<Order> result = (Page<Order>) orderMapper.findSellerOrdersPage(pageObj, sellerId, null);
        if (result != null && !result.getRecords().isEmpty()) {
            redisUtil.set(cacheKey, result);
        }
        return result;
    }

    /**
     * 判断订单是否包含指定卖家的商品（卖家操作权限校验）
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @param sellerId 卖家ID
     * @return boolean true=包含该卖家的商品
     */
    public boolean orderContainsSeller(Long orderId, Long sellerId) {
        return orderMapper.countOrderBySeller(orderId, sellerId) > 0;
    }

    /**
     * 发送订单缓存刷新任务（异步，通过 MQ）
     * 1.清除订单详情缓存
     * 2.清除订单项缓存
     * 3.清除用户订单列表缓存
     * 4.清除管理员订单列表缓存
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @param userId 用户ID
     */
    private void sendOrderCacheRefreshTask(Long orderId, Long userId) {
        List<String> keys = new ArrayList<>();
        keys.add(ORDER_DETAIL_CACHE_PREFIX + orderId);
        keys.add(ORDER_DETAIL_CACHE_PREFIX + "items:" + orderId);
        // 用户订单列表使用模糊匹配
        keys.add(ORDER_USER_LIST_CACHE_PREFIX + userId + ":page:*");
        // 管理员订单列表使用模糊匹配
        keys.add(ORDER_ADMIN_LIST_CACHE_PREFIX + "*");
        // 卖家订单列表使用模糊匹配
        keys.add("demo:order:seller:*");
        mqProducer.sendCacheRefreshTask("order", "clear", keys);
    }

    /**
     * 订单项请求参数（内部类）
     * 用于创建订单时传递商品ID和数量
     * <p>
     * @author ZuiM
     */
    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;

        public OrderItemRequest() {}

        public OrderItemRequest(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}