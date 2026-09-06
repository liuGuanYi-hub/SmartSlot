import request from './request'

// 分页查询操作审计日志 (AOP LogRecord)
export function getOperationLogs(params) {
  return request({
    url: '/api/admin/logs',
    method: 'get',
    params
  })
}

// 导出订单流水与对账单 (Alibaba EasyExcel 流式导出)
export async function downloadOrdersExcel(params) {
  const token = localStorage.getItem('smartslot_token')
  const cleanParams = {}
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      if (v !== '' && v !== null && v !== undefined) {
        cleanParams[k] = v
      }
    }
  }
  const query = new URLSearchParams(cleanParams).toString()
  const url = `/api/admin/export/orders${query ? '?' + query : ''}`

  const response = await fetch(url, {
    headers: {
      Authorization: token ? `Bearer ${token}` : ''
    }
  })
  if (!response.ok) {
    throw new Error('导出订单流水对账单失败')
  }
  const blob = await response.blob()
  const downloadUrl = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = downloadUrl
  a.download = `SmartSlot_订单流水对账单_${Date.now()}.xlsx`
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(downloadUrl)
}

// 导出场地排期总表 (Alibaba EasyExcel 流式导出)
export async function downloadVenuesExcel() {
  const token = localStorage.getItem('smartslot_token')
  const response = await fetch('/api/admin/export/venues', {
    headers: {
      Authorization: token ? `Bearer ${token}` : ''
    }
  })
  if (!response.ok) {
    throw new Error('导出场地排期总表失败')
  }
  const blob = await response.blob()
  const downloadUrl = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = downloadUrl
  a.download = `SmartSlot_场地排期总表_${Date.now()}.xlsx`
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(downloadUrl)
}
