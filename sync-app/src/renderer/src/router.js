import { createRouter, createWebHashHistory } from 'vue-router'
import Login from './views/Login.vue'
import Register from './views/Register.vue'
import Dashboard from './views/DashBoard.vue'
import FileExplorer from './views/FileExplorer.vue'
import LogPage from './views/LogPage.vue'
import GroupPage from './views/GroupPage.vue'
import GroupExplorer from './views/GroupExplorer.vue'
import HostConfig from './views/HostConfig.vue'
import HttpManager, { hasCachedClientConfig, setCachedClientConfig } from './utils/request'

const routes = [
  { path: '/', component: Login },
  { path: '/setup', component: HostConfig },
  { path: '/register', component: Register },
  { path: '/dashboard', component: Dashboard, meta: { requiresAuth: true } },
  { path: '/files/:fileId', component: FileExplorer, meta: { requiresAuth: true } },
  { path: '/logs', component: LogPage, meta: { requiresAuth: true } },
  { path: '/groups', component: GroupPage, meta: { requiresAuth: true } },
  {
    path: '/group-explorer/:groupId/:scopeName',
    component: GroupExplorer,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

let configState = null
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

function clearAuthState() {
  localStorage.removeItem('authToken')
  localStorage.removeItem('userInfo')
}

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || 'null')
  } catch {
    return null
  }
}

async function loadConfigState() {
  if (configState !== null) {
    return configState
  }

  for (let attempt = 0; attempt < 15; attempt += 1) {
    try {
      const res = await HttpManager.getNoAuth('/client/config')
      const data = res?.data ?? res
      if (data?.configured) {
        setCachedClientConfig(data)
      }
      configState = Boolean(data?.configured)
      return configState
    } catch {
      await sleep(350)
    }
  }

  configState = hasCachedClientConfig()
  return configState
}

export function clearConfigStateCache() {
  configState = null
}

router.beforeEach(async (to) => {
  const token = localStorage.getItem('authToken')

  if (to.path !== '/setup') {
    const configured = await loadConfigState()
    if (!configured) {
      return '/setup'
    }
  }

  if (token) {
    const user = readStoredUser()
    if (!user?.email || !user?.id) {
      clearAuthState()
      if (to.meta.requiresAuth) return '/'
    } else {
      try {
        await HttpManager.postNoAuth('/client/user/session', {
          id: String(user.id),
          email: user.email
        })
      } catch {
        clearAuthState()
        if (to.meta.requiresAuth || to.path === '/' || to.path === '/register') return '/'
      }
    }
  }

  const verifiedToken = localStorage.getItem('authToken')

  if ((to.path === '/' || to.path === '/register') && verifiedToken) {
    return '/dashboard'
  }

  if (to.meta.requiresAuth && !verifiedToken) {
    return '/'
  }

  return true
})

export default router
