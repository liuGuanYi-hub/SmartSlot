import request from './request'

// 领券中心：获取全部上架发放中优惠券
export function getClaimableCoupons() {
  return request({
    url: '/coupons/list',
    method: 'get'
  })
}

// 领取指定优惠券
export function claimCoupon(id) {
  return request({
    url: `/coupons/claim/${id}`,
    method: 'post'
  })
}

// 查询当前用户卡券包 (status: 0-未使用, 1-已使用, 2-已过期, null-全部)
export function getMyCoupons(status) {
  return request({
    url: '/coupons/my',
    method: 'get',
    params: status !== undefined ? { status } : {}
  })
}

// 订单结算智能优选抵扣试算
export function calculateOptimalCoupon(data) {
  return request({
    url: '/coupons/optimal',
    method: 'post',
    data
  })
}
