package com.xuwenye.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.RechargeRequest;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Mapper.RechargeRequestMapper;
import com.xuwenye.demo.Service.MQProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充值申请业务层
 * 1.用户提交申请 → 创建待审核记录
 * 2.管理员审核通过 → 增加用户余额
 * 3.管理员审核拒绝 → 更新状态
 * <p>
 * @author ZuiM
 */
@Service
public class RechargeRequestService {

    private final RechargeRequestMapper rechargeRequestMapper;
    private final UserService userService;
    private final MQProducer mqProducer;

    public RechargeRequestService(RechargeRequestMapper rechargeRequestMapper,
                                  UserService userService,
                                  MQProducer mqProducer) {
        this.rechargeRequestMapper = rechargeRequestMapper;
        this.userService = userService;
        this.mqProducer = mqProducer;
    }

    /**
     * 用户提交充值申请
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param username 用户名
     * @param amount 充值金额
     * @return RechargeRequest 创建的申请记录
     */
    @Transactional
    public RechargeRequest submitRequest(Long userId, String username, BigDecimal amount) {
        // 检查是否有未处理的申请
        LambdaQueryWrapper<RechargeRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRequest::getUserId, userId)
               .eq(RechargeRequest::getStatus, 0);
        long pendingCount = rechargeRequestMapper.selectCount(wrapper);
        if (pendingCount > 0) {
            throw new IllegalStateException("您已有待审核的充值申请，请等待管理员处理");
        }

        RechargeRequest request = new RechargeRequest();
        request.setUserId(userId);
        request.setUsername(username);
        request.setAmount(amount);
        request.setStatus(0);
        rechargeRequestMapper.insert(request);
        return request;
    }

    /**
     * 管理员审核通过充值申请
     * 1.更新申请状态为已通过
     * 2.为用户增加余额
     * 3.异步刷新缓存
     * <p>
     * @author ZuiM
     * @param requestId 申请ID
     */
    @Transactional
    public void approveRequest(Long requestId) {
        RechargeRequest request = rechargeRequestMapper.selectById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("充值申请不存在");
        }
        if (request.getStatus() != 0) {
            throw new IllegalArgumentException("该申请已处理，不能重复操作");
        }

        // 增加用户余额
        boolean success = userService.chargeBalance(request.getUserId(), request.getAmount());
        if (!success) {
            throw new RuntimeException("增加余额失败");
        }

        // 更新状态
        request.setStatus(1);
        rechargeRequestMapper.updateById(request);

        // 异步刷新用户缓存
        User user = userService.getUserById(request.getUserId());
        if (user != null) {
            mqProducer.sendCacheRefreshTask("user", "clear", List.of("demo:user:login:" + user.getUsername()));
        }
    }

    /**
     * 管理员拒绝充值申请
     * <p>
     * @author ZuiM
     * @param requestId 申请ID
     * @param reason 拒绝原因
     */
    public void rejectRequest(Long requestId, String reason) {
        RechargeRequest request = rechargeRequestMapper.selectById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("充值申请不存在");
        }
        if (request.getStatus() != 0) {
            throw new IllegalArgumentException("该申请已处理，不能重复操作");
        }

        request.setStatus(2);
        request.setRemark(reason);
        rechargeRequestMapper.updateById(request);
    }

    /**
     * 获取用户的充值申请记录
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<RechargeRequest>
     */
    public List<RechargeRequest> getUserRequests(Long userId) {
        return rechargeRequestMapper.findByUserId(userId);
    }

    /**
     * 分页获取所有充值申请（管理员用）
     * <p>
     * @author ZuiM
     * @param page 页码
     * @param size 每页条数
     * @return IPage<RechargeRequest>
     */
    public IPage<RechargeRequest> getRequestPage(int page, int size) {
        Page<RechargeRequest> pageObj = new Page<>(page, size);
        return rechargeRequestMapper.findPage(pageObj);
    }
}