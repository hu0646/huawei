import axios from 'axios'

const TOKEN_KEY = 'campus_food_token'

const http = axios.create({
  baseURL: '/api',
  timeout: 90000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result && typeof result.code === 'number') {
      if (result.code === 401) {
        localStorage.removeItem(TOKEN_KEY)
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
        return Promise.reject(new Error(result.message || '登录已失效'))
      }
      if (result.code !== 200) {
        return Promise.reject(new Error(result.message || '请求失败'))
      }
      return result.data
    }
    return result
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常，请稍后再试'
    return Promise.reject(new Error(message))
  }
)

export default http