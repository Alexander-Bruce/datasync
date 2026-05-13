import axios from 'axios'

export const LOCAL_API_BASE_URL = 'http://127.0.0.1:8092'
export const CLIENT_CONFIG_CACHE_KEY = 'clientConfig'

const service = axios.create({
  baseURL: LOCAL_API_BASE_URL,
  timeout: 10000
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken')
    if (config.needToken !== false && token) {
      config.headers.Authorization = `Bearer ${token}`
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
      if (window.location.hash !== '#/setup') {
        window.location.hash = '#/'
      }
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

  static async post(url, data, config = {}) {
    return service({
      url,
      method: 'post',
      data,
      needToken: true,
      headers: { 'Content-Type': 'application/json' },
      ...config
    })
  }

  static async get(url) {
    return service({ url, method: 'get', needToken: true })
  }

  static async getNoAuth(url) {
    return service({ url, method: 'get', needToken: false })
  }
}

export function getCachedClientConfig() {
  try {
    return JSON.parse(localStorage.getItem(CLIENT_CONFIG_CACHE_KEY) || 'null')
  } catch {
    return null
  }
}

export function setCachedClientConfig(config) {
  if (!config) return
  localStorage.setItem(CLIENT_CONFIG_CACHE_KEY, JSON.stringify(config))
}

export function hasCachedClientConfig() {
  const config = getCachedClientConfig()
  return Boolean(config?.configured && config?.serverBaseUrl && config?.syncHost)
}

export default HttpManager
