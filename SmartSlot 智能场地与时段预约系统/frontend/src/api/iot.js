import request from './request'

// 获取实时道闸通信报文日志
export function getGateLogs(limit = 20) {
  return request({
    url: '/api/iot/gate/logs',
    method: 'get',
    params: { limit }
  })
}

// 获取各场馆道闸硬件设备实时状态
export function getGateDevices() {
  return request({
    url: '/api/iot/gate/devices',
    method: 'get'
  })
}

// 手动下发远程开闸/常开应急指令
export function manualOpenGate(gateId, action = 'EMERGENCY_OPEN') {
  return request({
    url: '/api/iot/gate/manual-open',
    method: 'post',
    params: { gateId, action }
  })
}
