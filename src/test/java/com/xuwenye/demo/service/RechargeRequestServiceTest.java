package com.xuwenye.demo.service;

import com.xuwenye.demo.Entity.RechargeRequest;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Mapper.RechargeRequestMapper;
import com.xuwenye.demo.Service.RechargeRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 充值申请业务层集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.提交申请：创建待审核记录、重复申请拦截
 * 2.审核通过：增加用户余额并更新状态
 * 3.审核拒绝：更新状态与拒绝原因
 * 4.记录查询：用户维度与管理员分页
 * <p>
 * @author ZuiM
 */
class RechargeRequestServiceTest extends AbstractServiceTest {

    @Autowired
    private RechargeRequestService rechargeRequestService;

    @Autowired
    private RechargeRequestMapper rechargeRequestMapper;

    // ========== 测试数据构造 ==========

    /**
     * 构造测试用户（初始余额 100）
     * <p>
     * @author ZuiM
     * @return User 用户
     */
    private User buildUser() {
        User user = new User();
        user.setUsername(uniqueName("recharge_user"));
        user.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa");
        user.setNickname("充值测试用户" + SUFFIX);
        user.setEmail(uniqueName("recharge") + "@test.com");
        user.setStatus(1);
        user.setUserRole("ROLE_USER");
        user.setBalance(new BigDecimal("100.00"));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    /**
     * 清理测试数据（申请记录 + 用户）
     * <p>
     * @author ZuiM
     * @param user 用户
     * @param requestId 申请ID（可为null）
     */
    private void cleanData(User user, Long requestId) {
        if (requestId != null) {
            rechargeRequestMapper.deleteById(requestId);
        }
        if (user != null) {
            trackCacheKey("demo:user:active:" + user.getUsername());
            trackCacheKey("demo:user:all:" + user.getUsername());
            trackCacheKey("demo:user:id:" + user.getId());
            userMapper.deleteById(user.getId());
        }
    }

    // ========== 1. 提交申请 ==========

    /**
     * 提交充值申请成功，状态为待审核
     * <p>
     * @author ZuiM
     */
    @Test
    void 提交充值申请() {
        User user = buildUser();
        RechargeRequest request = rechargeRequestService.submitRequest(user.getId(), user.getUsername(), new BigDecimal("50.00"));
        assertNotNull(request.getId());
        assertEquals(0, request.getStatus());
        assertEquals(0, new BigDecimal("50.00").compareTo(request.getAmount()));

        cleanData(user, request.getId());
    }

    /**
     * 存在待审核申请时重复提交被拦截
     * <p>
     * @author ZuiM
     */
    @Test
    void 存在待审核申请时重复提交被拦截() {
        User user = buildUser();
        RechargeRequest request = rechargeRequestService.submitRequest(user.getId(), user.getUsername(), new BigDecimal("50.00"));

        assertThrows(IllegalStateException.class,
                () -> rechargeRequestService.submitRequest(user.getId(), user.getUsername(), new BigDecimal("30.00")));

        cleanData(user, request.getId());
    }

    // ========== 2. 审核通过 ==========

    /**
     * 审核通过后申请状态更新且用户余额增加
     * <p>
     * @author ZuiM
     */
    @Test
    void 审核通过后用户余额增加() {
        User user = buildUser();
        RechargeRequest request = rechargeRequestService.submitRequest(user.getId(), user.getUsername(), new BigDecimal("100.00"));

        rechargeRequestService.approveRequest(request.getId());

        // 状态更新为已通过
        assertEquals(1, rechargeRequestMapper.selectById(request.getId()).getStatus());
        // 余额 100 + 100 = 200
        assertEquals(0, new BigDecimal("200.00").compareTo(userMapper.selectById(user.getId()).getBalance()));

        cleanData(user, request.getId());
    }

    /**
     * 审核通过后再次操作（通过/拒绝）被拦截
     * <p>
     * @author ZuiM
     */
    @Test
    void 审核通过后重复操作被拦截() {
        User user = buildUser();
        RechargeRequest request = rechargeRequestService.submitRequest(user.getId(), user.getUsername(), new BigDecimal("100.00"));
        rechargeRequestService.approveRequest(request.getId());

        assertThrows(IllegalArgumentException.class,
                () -> rechargeRequestService.approveRequest(request.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> rechargeRequestService.rejectRequest(request.getId(), "重复处理"));

        cleanData(user, request.getId());
    }

    // ========== 3. 审核拒绝 ==========

    /**
     * 审核拒绝后状态更新为已拒绝且余额不变
     * <p>
     * @author ZuiM
     */
    @Test
    void 审核拒绝后状态更新余额不变() {
        User user = buildUser();
        RechargeRequest request = rechargeRequestService.submitRequest(user.getId(), user.getUsername(), new BigDecimal("100.00"));

        rechargeRequestService.rejectRequest(request.getId(), "金额异常，请重新提交");

        RechargeRequest rejected = rechargeRequestMapper.selectById(request.getId());
        assertEquals(2, rejected.getStatus());
        assertEquals("金额异常，请重新提交", rejected.getRemark());
        // 余额保持 100 不变
        assertEquals(0, new BigDecimal("100.00").compareTo(userMapper.selectById(user.getId()).getBalance()));

        cleanData(user, request.getId());
    }

    // ========== 4. 记录查询 ==========

    /**
     * 用户申请记录与管理员分页查询
     * <p>
     * @author ZuiM
     */
    @Test
    void 申请记录与分页查询() {
        User user = buildUser();
        RechargeRequest request = rechargeRequestService.submitRequest(user.getId(), user.getUsername(), new BigDecimal("50.00"));

        assertTrue(rechargeRequestService.getUserRequests(user.getId()).stream()
                .anyMatch(r -> r.getId().equals(request.getId())));
        assertNotNull(rechargeRequestService.getRequestPage(1, 10));

        cleanData(user, request.getId());
    }
}