/** 学生端路由：/student/* */
export default [
  {
    path: '/student/orders/create',
    name: 'StudentOrderCreate',
    component: () => import('@/views/student/OrderCreateView.vue'),
    meta: { title: '我要报修', roles: ['STUDENT'] }
  },
  {
    path: '/student/orders',
    name: 'StudentOrderList',
    component: () => import('@/views/student/OrderListView.vue'),
    meta: { title: '我的工单', roles: ['STUDENT'] }
  },
  {
    path: '/student/orders/:id',
    name: 'StudentOrderDetail',
    component: () => import('@/views/student/OrderDetailView.vue'),
    meta: { title: '工单详情', roles: ['STUDENT'] }
  },
  {
    path: '/student/orders/:id/evaluate',
    name: 'StudentEvaluate',
    component: () => import('@/views/student/EvaluateView.vue'),
    meta: { title: '评价维修', roles: ['STUDENT'] }
  }
]
