import { createRouter, createWebHashHistory } from 'vue-router'
import Login from './views/Login.vue'
import Register from './views/Register.vue'
import Dashboard from './views/DashBoard.vue'
import FileExplorer from './views/FileExplorer.vue'
import LogPage from './views/LogPage.vue'
import GroupPage from './views/GroupPage.vue'
import GroupExplorer from './views/GroupExplorer.vue'
import HostConfig from './views/HostConfig.vue'
import HttpManager from './utils/request'

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

async function loadConfigState() {
  if (configState !== null) {
    return configState
  }

  try {
    const res = await HttpManager.getNoAuth('/client/config')
    configState = Boolean((res?.data ?? res)?.configured)
  } catch {
    configState = false
  }

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

  if ((to.path === '/' || to.path === '/register') && token) {
    return '/dashboard'
  }

  if (to.meta.requiresAuth && !token) {
    return '/'
  }

  return true
})

export default router
