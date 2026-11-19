/** 维修工端路由：/repairer/* */
export default [
  {
    path: '/repairer/pool',
    name: 'RepairerPool',
    component: () => import('@/views/repairer/GrabPoolView.vue'),
    meta: { title: '抢单池', roles: ['REPAIRER'] }
  },
  {
    path: '/repairer/orders',
    name: 'RepairerOrderList',
    component: () => import('@/views/repairer/MyOrderListView.vue'),
    meta: { title: '我的工单', roles: ['REPAIRER'] }
  },
  {
    path: '/repairer/orders/:id',
    name: 'RepairerOrderDetail',
    component: () => import('@/views/repairer/RepairDetailView.vue'),
    meta: { title: '工单详情', roles: ['REPAIRER'] }
  },
  {
    path: '/repairer/orders/:id/arrive',
    name: 'RepairerArrive',
    component: () => import('@/views/repairer/ArriveConfirmView.vue'),
    meta: { title: '确认到场', roles: ['REPAIRER'] }
  },
  {
    path: '/repairer/orders/:id/repair',
    name: 'RepairerRepairForm',
    component: () => import('@/views/repairer/RepairFormView.vue'),
    meta: { title: '填写维修过程', roles: ['REPAIRER'] }
  },
  {
    path: '/repairer/orders/:id/finish',
    name: 'RepairerFinish',
    component: () => import('@/views/repairer/FinishFormView.vue'),
    meta: { title: '提交完工', roles: ['REPAIRER'] }
  }
]
