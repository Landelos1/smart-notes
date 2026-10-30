import apiClient from './apiClient'

/**
 * 获取会员套餐列表
 */
export function getMemberPlans() {
  return apiClient.get('/api/payment/plans')
}

/**
 * 创建订单
 * @param {number} planId - 套餐ID
 */
export function createOrder(planId) {
  return apiClient.post('/api/payment/create-order', { planId })
}

/**
 * 查询订单状态
 * @param {string} orderNo - 订单编号
 */
export function queryOrderStatus(orderNo) {
  return apiClient.get(`/api/payment/order/${orderNo}`)
}

/**
 * 刷新订单状态，支付结果由后端验签的异步通知更新。
 * @param {string} orderNo - 订单编号
 */
export function syncOrderStatus(orderNo) {
  return queryOrderStatus(orderNo)
}

/**
 * 获取用户订单列表
 */
export function getUserOrders() {
  return apiClient.get('/api/payment/orders')
}
