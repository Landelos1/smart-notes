package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.ldd.initialization.dto.AdminOrderQueryDTO;
import com.ldd.initialization.dto.CreateOrderDTO;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.PaymentService;
import com.ldd.initialization.vo.MemberPlanVO;
import com.ldd.initialization.vo.OrderVO;
import com.ldd.initialization.vo.PaymentVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付接口
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 获取会员套餐列表
     */
    @SaIgnore
    @GetMapping("/plans")
    public Result<List<MemberPlanVO>> listPlans() {
        List<MemberPlanVO> plans = paymentService.listMemberPlans();
        return Result.success(plans);
    }

    /**
     * 创建订单并获取支付表单
     */
    @SaCheckLogin
    @PostMapping("/create-order")
    public Result<PaymentVO> createOrder(@Valid @RequestBody CreateOrderDTO dto) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        PaymentVO payment = paymentService.createOrder(userId, dto.getPlanId());
        return Result.success(payment);
    }

    /**
     * 支付宝异步通知回调
     */
    @SaIgnore
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append((i == values.length - 1) ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }
        
        log.info("收到支付宝异步通知: {}", params);
        return paymentService.handleAlipayNotify(params);
    }

    /**
     * 支付宝同步回调（前端跳转）
     */
    @SaIgnore
    @GetMapping("/alipay/return")
    public Result<OrderVO> alipayReturn(@RequestParam("out_trade_no") String orderNo) {
        // 同步回调只做查询，实际支付结果以异步通知为准
        OrderVO order = paymentService.queryOrderStatus(orderNo);
        return Result.success(order);
    }

    /**
     * 查询订单状态
     */
    @SaCheckLogin
    @GetMapping("/order/{orderNo}")
    public Result<OrderVO> queryOrder(@PathVariable String orderNo) {
        OrderVO order = paymentService.queryOrderStatus(orderNo);
        return Result.success(order);
    }

    /**
     * 获取用户订单列表（用户端）
     */
    @SaCheckLogin
    @GetMapping("/orders")
    public Result<List<OrderVO>> listOrders() {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        List<OrderVO> orders = paymentService.listUserOrders(userId);
        return Result.success(orders);
    }

    /**
     * 管理员分页查询所有订单（支持搜索）
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/admin/orders")
    public Result<PageResult<OrderVO>> listAllOrders(AdminOrderQueryDTO dto) {
        PageResult<OrderVO> result = paymentService.listAllOrders(dto);
        return Result.success(result);
    }

    /**
     * 管理员取消订单
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/admin/order/{orderNo}/cancel")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        paymentService.cancelOrder(orderNo);
        return Result.success(null);
    }

    /**
     * 管理员删除订单
     */
    @SaCheckRole("ADMIN")
    @DeleteMapping("/admin/order/{orderNo}")
    public Result<Void> deleteOrder(@PathVariable String orderNo) {
        paymentService.deleteOrder(orderNo);
        return Result.success(null);
    }
}
