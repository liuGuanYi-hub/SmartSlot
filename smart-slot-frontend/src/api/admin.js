import request from './request'

// 分页查询操作审计日志 (AOP LogRecord)
export function getOperationLogs(params) {
  return request({
    url: '/api/admin/logs',
    method: 'get',
    params
  })
}
