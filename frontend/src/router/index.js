import { createRouter, createWebHistory } from 'vue-router'
import AppShell from '@/layout/AppShell.vue'
import LoginView from '@/views/LoginView.vue'
import HomeView from '@/views/HomeView.vue'
import NotFoundView from '@/views/NotFoundView.vue'
import studentRoutes from './routes/student'
import repairerRoutes from './routes/repairer'
import adminRoutes from './routes/admin'
import { setupGuards } from './guards'
import { ROLE_HOME } from './roles'

export { ROLE_HOME }

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: AppShell,
    children: [
      { path: '', name: 'Home', component: HomeView, meta: { title: '首页' } },
      {
        path: 'service-notices',
        name: 'ServiceNotice',
        component: () => import('@/views/ServiceNoticeView.vue'),
        meta: { title: '服务公告' }
      },
      ...studentRoutes,
      ...repairerRoutes,
      ...adminRoutes
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFoundView,
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

setupGuards(router)

export default router
