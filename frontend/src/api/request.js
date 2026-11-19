import axios from 'axios'
import { ElMessage } from '@/utils/feedback'

const service = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/** 清登录态并跳登录页（保留 redirect） */
function redirectToLogin() {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('refreshToken')
  sessionStorage.removeItem('userInfo')
  const path = window.location.pathname + window.location.search
  if (!path.startsWith('/login')) {
    window.location.href = `/login?redirect=${encodeURIComponent(path)}`
  }
}

// 这些业务码由页面自行处理（刷新详情/移除行/幂等），拦截器不再重复弹提示
const SILENT_CODES = new Set([40009, 40010, 40011, 40301, 40401, 40901, 40902, 40903, 40904, 40905])

service.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') return response
    const body = response.data || {}
    const { code, message, data } = body
    if (code === 200 || code === 40905) return data
    if (code === 401) {
      redirectToLogin()
    } else if (!SILENT_CODES.has(code)) {
      ElMessage.error(message || '请求失败')
    }
    const error = new Error(message || '请求失败')
    error.code = code
    return Promise.reject(error)
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data || {}
    const message = body.message
    if (status === 401) {
      redirectToLogin()
    } else if (status === 403) {
      ElMessage.error(message || '无权限操作')
    } else if (status === 429) {
      ElMessage.warning(message || '操作过于频繁，请稍后再试')
    } else if (status === 503) {
      ElMessage.error(message || '图片服务暂不可用，请稍后再试')
    } else if (status >= 500) {
      ElMessage.error(message || '服务异常，请稍后再试')
    } else {
      ElMessage.error(message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default service
