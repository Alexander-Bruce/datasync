import axios from 'axios'
import router from '../router'

const service = axios.create({
  baseURL: 'http://127.0.0.1:8092',
  timeout: 10000
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken')
    if (config.needToken !== false && token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && (body.code < 200 || body.code >= 300)) {
      const err = new Error(body.message || 'Request failed')
      err.response = response
      err.data = body
      return Promise.reject(err)
    }
    return body
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('authToken')
      localStorage.removeItem('userInfo')
      router.push('/')
    }
    return Promise.reject(error)
  }
)

class HttpManager {
  static async postNoAuth(url, data) {
    return service({
      url,
      method: 'post',
      data,
      needToken: false,
      headers: { 'Content-Type': 'application/json' }
    })
  }

  static async post(url, data) {
    return service({
      url,
      method: 'post',
      data,
      needToken: true,
      headers: { 'Content-Type': 'application/json' }
    })
  }

  static async get(url) {
    return service({ url, method: 'get', needToken: true })
  }
}

export default HttpManager
