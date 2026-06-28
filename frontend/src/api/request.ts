import axios from 'axios'
import { ElMessage } from 'element-plus'

// 扩展 axios 配置，支持跳过错误提示
declare module 'axios' {
  interface AxiosRequestConfig {
    skipErrorToast?: boolean
  }
}

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 用于401跳转的去重防抖
let isRedirecting = false

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (!(response.config as any).skipErrorToast) {
        ElMessage.error(res.message || '请求失败')
      }
      if (res.code === 401 && !isRedirecting) {
        handleUnauthorized()
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    if (!(error.config as any)?.skipErrorToast) {
      const msg = error.response?.data?.message || error.message || '网络错误'
      ElMessage.error(msg)
    }
    if (error.response?.status === 401 && !isRedirecting) {
      handleUnauthorized()
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  isRedirecting = true
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  // 如果当前已经在公开页面（/, /login, /register），不重定向
  const path = window.location.pathname
  if (path === '/' || path === '/login' || path === '/register') {
    isRedirecting = false
    return
  }
  window.location.href = '/'
}

export default request
