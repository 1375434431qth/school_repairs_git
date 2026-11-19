/** 管理员端路由：/admin/* */
export default [
  {
    path: '/admin/dispatch',
    name: 'AdminDispatch',
    component: () => import('@/views/admin/DispatchView.vue'),
    meta: { title: '派单管理', roles: ['MANAGER'] }
  },
  {
    path: '/admin/board',
    name: 'AdminBoard',
    component: () => import('@/views/admin/BoardView.vue'),
    meta: { title: '工单看板', roles: ['MANAGER'] }
  },
  {
    path: '/admin/timeout',
    name: 'AdminTimeout',
    component: () => import('@/views/admin/TimeoutView.vue'),
    meta: { title: '超时管理', roles: ['MANAGER'] }
  },
  {
    path: '/admin/stat',
    name: 'AdminStat',
    component: () => import('@/views/admin/StatView.vue'),
    meta: { title: '数据统计', roles: ['MANAGER'] }
  },
  {
    path: '/admin/repairer-load',
    name: 'AdminRepairerLoad',
    component: () => import('@/views/admin/RepairerLoadView.vue'),
    meta: { title: '维修工负载', roles: ['MANAGER'] }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/admin/UserAdminView.vue'),
    meta: { title: '用户管理', roles: ['MANAGER'] }
  },
  {
    path: '/admin/base-data',
    name: 'AdminBaseData',
    component: () => import('@/views/admin/BaseDataView.vue'),
    meta: { title: '基础数据', roles: ['MANAGER'] }
  },
  {
    path: '/admin/carousel',
    name: 'AdminCarousel',
    component: () => import('@/views/admin/CarouselView.vue'),
    meta: { title: '首页轮播', roles: ['MANAGER'] }
  }
]
