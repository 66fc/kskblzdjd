import axios from 'axios'
import { ElMessage } from 'element-plus'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const http = axios.create({
  baseURL,
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('badminton_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status
    const payload = error?.response?.data
    const message = payload?.message || error?.message || '网络请求失败'
    if (!error.config?.silent) {
      ElMessage.error(message)
    }
    if (status === 401) {
      localStorage.removeItem('badminton_token')
      const isClient = location.pathname.startsWith('/club')
      if (isClient) {
        const redirect = encodeURIComponent(location.pathname + location.search)
        if (!location.pathname.startsWith('/club/login')) {
          location.href = `/club/login?redirect=${redirect}`
        }
      } else if (!location.pathname.startsWith('/login')) {
        location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default http
