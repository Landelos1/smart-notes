package com.ldd.initialization.service;

import com.ldd.initialization.vo.MemberPlanVO;
import com.ldd.initialization.vo.OrderVO;
import com.ldd.initialization.vo.PaymentVO;

import java.util.List;
import java.util.Map;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 获取所有上架的会员套餐
     */
    List<MemberPlanVO> listMemberPlans();

    /**
     * 创建订单并获取支付表单
     *
     * @param userId 用户ID
     * @param planId 套餐ID
     * @return 支付信息
     */
    PaymentVO createOrder(Long userId, Long planId);

    /**
     * 处理支付宝异步通知
     *
     * @param params 支付宝回调参数
     * @return 处理结果
     */
    String handleAlipayNotify(Map<String, String> params);

    /**
     * 查询订单状态
     *
     * @param orderNo 订单编号
     * @param requesterId 当前查询用户
     * @param admin 是否管理员
     * @return 订单信息
     */
    OrderVO queryOrderStatus(String orderNo, Long requesterId, boolean admin);

    /**
     * 获取用户订单列表
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    List<OrderVO> listUserOrders(Long userId);

    /**
     * 管理员分页查询所有订单（支持搜索）
     *
     * @param dto 查询条件
     * @return 分页订单列表
     */
    com.ldd.initialization.result.PageResult<OrderVO> listAllOrders(com.ldd.initialization.dto.AdminOrderQueryDTO dto);

    /**
     * 管理员取消订单
     *
     * @param orderNo 订单编号
     */
    void cancelOrder(String orderNo);

    /**
     * 管理员删除订单
     *
     * @param orderNo 订单编号
     */
    void deleteOrder(String orderNo);
}
