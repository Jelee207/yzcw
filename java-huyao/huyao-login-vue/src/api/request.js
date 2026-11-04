import axios from 'axios'

/**
 * axios 请求封装
 * 统一 baseURL /api，通过 Vite 代理转发到后端 http://localhost:8081
 * 统一处理响应拦截：code === 200 成功，否则抛出错误提示
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 响应拦截器：解包统一 Result 结构 { code, message, data }
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data // 成功，返回 data
    }
    // 业务失败，弹出提示
    alert(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    alert('网络异常，请检查后端服务是否启动～')
    return Promise.reject(error)
  }
)

export default request
