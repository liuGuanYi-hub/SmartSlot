import request from './request'

// 发起收银台预下单 (生成支付宝/微信动态二维码或处理余额直接支付)
export function createPrepay(data) {
  return request({
    url: '/pay/prepay',
    method: 'post',
    data
  })
}

// 模拟沙箱手机扫码支付成功 (触发网关异步通知出票)
export function mockSandboxCallback(orderNo, channel = 'ALIPAY') {
  return request({
    url: '/pay/mock-sandbox-callback',
    method: 'post',
    data: {
      orderNo,
      channel
    }
  })
}

// 查询财务网关流水对账单
export function getPaymentReconciliation() {
  return request({
    url: '/pay/reconciliation',
    method: 'get'
  })
}
