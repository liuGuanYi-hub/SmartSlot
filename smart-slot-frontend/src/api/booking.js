import request from './request'

// 获取一次性接口防重放幂等 Token
export function getIdempotentToken() {
  return request({
    url: '/common/idempotent-token',
    method: 'get'
  })
}

// 核心锁定并创建订单 (携带幂等 Token 防重复提交)
export function lockAndCreateOrder(data, idempotentToken) {
  return request({
    url: '/orders/lock-and-create',
    method: 'post',
    headers: idempotentToken ? { 'Idempotent-Token': idempotentToken } : {},
    data
  })
}

// 模拟支付 (携带幂等 Token 防重复扣款)
export function payOrder(orderNo, idempotentToken) {
  return request({
    url: `/orders/pay/${orderNo}`,
    method: 'post',
    headers: idempotentToken ? { 'Idempotent-Token': idempotentToken } : {}
  })
}

// 取消订单
export function cancelOrder(id, reason) {
  return request({
    url: `/orders/${id}/cancel`,
    method: 'post',
    params: { reason }
  })
}

// 用户端分页查询我的预约
export function getMyOrdersPage(params) {
  return request({
    url: '/orders/my',
    method: 'get',
    params
  })
}

// 提交评价
export function submitReview(data) {
  return request({
    url: '/orders/review',
    method: 'post',
    data
  })
}

// 管理端核销 6 位核销码
export function verifyOrderByCode(verifyCode) {
  return request({
    url: '/admin/orders/verify',
    method: 'post',
    params: { verifyCode }
  })
}

// 管理端分页查询所有订单
export function getAdminOrdersPage(params) {
  return request({
    url: '/admin/orders/page',
    method: 'get',
    params
  })
}
