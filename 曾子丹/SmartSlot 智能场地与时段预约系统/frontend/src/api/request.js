import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('smart_slot_token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    // 如果返回的是二进制或非标准格式直接放行
    if (res.code === undefined) {
      return res
    }
    if (res.code === 200) {
      return res.data
    } else {
      ElMessage.error(res.message || '系统异常')
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  error => {
    if (error.response) {
      if (error.response.status === 401) {
        ElMessage.warning('登录状态已失效，请重新登录')
        localStorage.removeItem('smart_slot_token')
        localStorage.removeItem('smart_slot_user')
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login'
        }
      } else if (error.response.status === 429) {
        ElMessage.warning('请求过于频繁，请稍后再试')
      } else {
        const msg = error.response.data?.message || '网络连接异常'
        ElMessage.error(msg)
      }
    } else {
      ElMessage.error('网络服务异常，请检查后端连接')
    }
    return Promise.reject(error)
  }
)

export default service
