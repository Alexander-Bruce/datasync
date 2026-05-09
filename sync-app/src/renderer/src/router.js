import { createRouter, createWebHashHistory } from 'vue-router'
import Login from './views/Login.vue'
import Register from './views/Register.vue'
import Dashboard from './views/Dashboard.vue'
import FileExplorer from './views/FileExplorer.vue'
import LogPage from './views/LogPage.vue'
import GroupPage from './views/GroupPage.vue'
import GroupExplorer from './views/GroupExplorer.vue'

const routes = [
  { path: '/', component: Login },
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

// ▼▼▼ 新增：路由守卫 ▼▼▼
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('authToken')

  // 1. 如果用户要去 Dashboard，但没有 Token -> 踢回登录页
  if (to.path === '/dashboard' && !token) {
    next('/')
  }
  // 2. 如果用户要去登录页，但其实已经有 Token 了 -> 直接去 Dashboard (自动登录)
  else if (to.path === '/' && token) {
    next('/dashboard')
  }
  // 3. 其他情况正常放行
  else {
    next()
  }
})

export default router
