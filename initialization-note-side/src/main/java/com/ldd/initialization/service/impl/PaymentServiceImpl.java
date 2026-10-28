package com.ldd.initialization.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.config.AlipayConfig;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.MemberPlan;
import com.ldd.initialization.domain.Order;
import com.ldd.initialization.domain.UserMembership;
import com.ldd.initialization.dto.AdminOrderQueryDTO;
import com.ldd.initialization.mapper.MemberPlanMapper;
import com.ldd.initialization.mapper.OrderMapper;
import com.ldd.initialization.mapper.UserMembershipMapper;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.service.PaymentService;
import com.ldd.initialization.vo.MemberPlanVO;
import com.ldd.initialization.vo.OrderVO;
import com.ldd.initialization.vo.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 支付服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final MemberPlanMapper memberPlanMapper;
    private final OrderMapper orderMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;

    @Override
    public List<MemberPlanVO> listMemberPlans() {
        LambdaQueryWrapper<MemberPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberPlan::getStatus, 1)
               .orderByAsc(MemberPlan::getDurationMonths);
        
        List<MemberPlan> plans = memberPlanMapper.selectList(wrapper);
        return plans.stream().map(plan -> {
            MemberPlanVO vo = new MemberPlanVO();
            BeanUtils.copyProperties(plan, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentVO createOrder(Long userId, Long planId) {
        // 1. 查询套餐信息
        MemberPlan plan = memberPlanMapper.selectById(planId);
        if (plan == null || plan.getStatus() != 1) {
            throw new BizException("套餐不存在或已下架");
        }

        // 2. 生成订单编号
        String orderNo = generateOrderNo();

        // 3. 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPlanId(planId);
        order.setPlanName(plan.getPlanName());
        order.setAmount(plan.getPrice());
        order.setPayMethod(2); // 支付宝
        order.setStatus(0); // 待支付
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        orderMapper.insert(order);

        // 4. 构建支付宝支付请求
        String payForm = buildAlipayForm(orderNo, plan.getPrice().toString(), plan.getPlanName());

        // 5. 返回支付信息
        PaymentVO vo = new PaymentVO();
        vo.setOrderNo(orderNo);
        vo.setPayForm(payForm);
        vo.setAmount(plan.getPrice().toString());
        vo.setPlanName(plan.getPlanName());
        return vo;
    }

    @Override
    @Transactional
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            // 1. 验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );

            if (!signVerified) {
                log.error("支付宝回调签名验证失败");
                return "failure";
            }

            // 2. 获取订单信息
            String orderNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String tradeNo = params.get("trade_no");

            log.info("支付宝回调 - 订单号: {}, 交易状态: {}, 支付宝交易号: {}", orderNo, tradeStatus, tradeNo);

            // 3. 查询订单
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getOrderNo, orderNo);
            Order order = orderMapper.selectOne(wrapper);

            if (order == null) {
                log.error("订单不存在: {}", orderNo);
                return "failure";
            }

            // 4. 判断交易状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 支付成功，更新订单状态
                if (order.getStatus() == 0) {
                    Date payTime = new Date();
                    LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
                    uw.eq(Order::getId, order.getId())
                      .eq(Order::getStatus, 0)
                      .set(Order::getStatus, 1)
                      .set(Order::getPayTime, payTime)
                      .set(Order::getUpdateTime, payTime);
                    int updated = orderMapper.update(null, uw);
                    if (updated == 1) {
                        order.setStatus(1);
                        order.setPayTime(payTime);
                        order.setUpdateTime(payTime);
                        applyMembership(order, payTime);
                        log.info("订单支付成功: {}", orderNo);
                    } else {
                        log.info("订单已处理，无需重复更新: {}", orderNo);
                    }
                }
            }

            return "success";

        } catch (AlipayApiException e) {
            log.error("处理支付宝回调异常", e);
            return "failure";
        }
    }

    @Override
    public OrderVO queryOrderStatus(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            throw new BizException("订单不存在");
        }

        return convertToOrderVO(order);
    }

    private void applyMembership(Order order, Date payTime) {
        if (order == null || order.getUserId() == null || order.getPlanId() == null) return;

        MemberPlan plan = memberPlanMapper.selectById(order.getPlanId());
        if (plan == null || plan.getDurationMonths() == null || plan.getDurationMonths() <= 0) return;

        LambdaQueryWrapper<UserMembership> mw = new LambdaQueryWrapper<>();
        mw.eq(UserMembership::getUserId, order.getUserId())
          .orderByDesc(UserMembership::getExpireTime)
          .last("limit 1");
        UserMembership membership = userMembershipMapper.selectOne(mw);

        Date base = payTime;
        if (membership != null && membership.getExpireTime() != null && membership.getExpireTime().after(payTime)) {
            base = membership.getExpireTime();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.MONTH, plan.getDurationMonths());
        Date newExpire = cal.getTime();

        if (membership == null) {
            membership = new UserMembership();
            membership.setUserId(order.getUserId());
            membership.setCreateTime(payTime);
        }
        membership.setExpireTime(newExpire);
        membership.setIsActive(newExpire.after(new Date()));
        membership.setUpdateTime(payTime);

        if (membership.getId() == null) {
            userMembershipMapper.insert(membership);
        } else {
            userMembershipMapper.updateById(membership);
        }
    }

    @Override
    public List<OrderVO> listUserOrders(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .orderByDesc(Order::getCreateTime);
        
        List<Order> orders = orderMapper.selectList(wrapper);
        return orders.stream()
                .map(this::convertToOrderVO)
                .collect(Collectors.toList());
    }

    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        String random = String.format("%04d", new Random().nextInt(10000));
        return "ORD" + timestamp + random;
    }

    /**
     * 构建支付宝支付表单
     */
    private String buildAlipayForm(String orderNo, String amount, String subject) {
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setReturnUrl(alipayConfig.getReturnUrl());
            request.setNotifyUrl(alipayConfig.getNotifyUrl());

            // 构建业务参数
            String bizContent = String.format(
                    "{\"out_trade_no\":\"%s\"," +
                    "\"total_amount\":\"%s\"," +
                    "\"subject\":\"%s\"," +
                    "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}",
                    orderNo, amount, subject
            );
            request.setBizContent(bizContent);

            // 生成支付表单
            String form = alipayClient.pageExecute(request).getBody();
            log.info("生成支付表单 - 订单号: {}", orderNo);
            return form;

        } catch (AlipayApiException e) {
            log.error("生成支付宝支付表单异常", e);
            throw new BizException("生成支付表单失败");
        }
    }

    @Override
    public PageResult<OrderVO> listAllOrders(AdminOrderQueryDTO dto) {
        // 构建分页对象
        Page<Order> page = new Page<>(dto.getCurrent(), dto.getSize());
        
        // 构建查询条件
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        
        // 订单号模糊查询
        if (StringUtils.hasText(dto.getOrderNo())) {
            wrapper.like(Order::getOrderNo, dto.getOrderNo());
        }
        
        // 套餐名称模糊查询
        if (StringUtils.hasText(dto.getPlanName())) {
            wrapper.like(Order::getPlanName, dto.getPlanName());
        }
        
        // 支付方式
        if (dto.getPayMethod() != null) {
            wrapper.eq(Order::getPayMethod, dto.getPayMethod());
        }
        
        // 状态
        if (dto.getStatus() != null) {
            wrapper.eq(Order::getStatus, dto.getStatus());
        }
        
        // 时间范围查询
        if (StringUtils.hasText(dto.getStartTime())) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime startDateTime = LocalDateTime.parse(dto.getStartTime() + " 00:00:00", 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                wrapper.ge(Order::getCreateTime, startDateTime);
            } catch (Exception e) {
                log.warn("开始时间解析失败: {}", dto.getStartTime(), e);
            }
        }
        
        if (StringUtils.hasText(dto.getEndTime())) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime endDateTime = LocalDateTime.parse(dto.getEndTime() + " 23:59:59", 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                wrapper.le(Order::getCreateTime, endDateTime);
            } catch (Exception e) {
                log.warn("结束时间解析失败: {}", dto.getEndTime(), e);
            }
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Order::getCreateTime);
        
        // 执行分页查询
        IPage<Order> orderPage = orderMapper.selectPage(page, wrapper);
        
        // 转换为VO
        List<OrderVO> voList = orderPage.getRecords().stream()
                .map(this::convertToOrderVO)
                .collect(Collectors.toList());
        
        // 构建分页结果
        PageResult<OrderVO> result = new PageResult<>();
        result.setData(voList);
        result.setTotal(orderPage.getTotal());
        
        return result;
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        // 1. 查询订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(wrapper);
        
        if (order == null) {
            throw new BizException("订单不存在");
        }
        
        // 2. 只有待支付状态的订单可以取消
        if (order.getStatus() != 0) {
            throw new BizException("只有待支付状态的订单可以取消");
        }
        
        // 3. 更新订单状态为已取消
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                     .eq(Order::getStatus, 0)
                     .set(Order::getStatus, 2)
                     .set(Order::getUpdateTime, new Date());
        
        int updated = orderMapper.update(null, updateWrapper);
        if (updated == 0) {
            throw new BizException("订单状态已变更，取消失败");
        }
        
        log.info("订单已取消 - 订单号: {}", orderNo);
    }

    @Override
    @Transactional
    public void deleteOrder(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        int deleted = orderMapper.delete(wrapper);
        if (deleted == 0) {
            throw new BizException("订单不存在或已删除");
        }

        log.info("订单已删除 - 订单号: {}", orderNo);
    }

    /**
     * 转换为OrderVO
     */
    private OrderVO convertToOrderVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        
        // 设置支付方式名称
        if (order.getPayMethod() != null) {
            vo.setPayMethodName(order.getPayMethod() == 1 ? "微信支付" : "支付宝");
        }
        
        // 设置状态名称
        if (order.getStatus() != null) {
            switch (order.getStatus()) {
                case 0 -> vo.setStatusName("待支付");
                case 1 -> vo.setStatusName("已支付");
                case 2 -> vo.setStatusName("已取消");
                default -> vo.setStatusName("未知");
            }
        }
        
        return vo;
    }
}
