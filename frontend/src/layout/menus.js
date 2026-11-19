/** 三端顶部导航配置（按角色过滤），与路由 meta.roles 共用同一份角色定义 */
export const MENUS = {
  STUDENT: [
    { label: '首页', path: '/' },
    { label: '我要报修', path: '/student/orders/create' },
    { label: '我的工单', path: '/student/orders' }
  ],
  REPAIRER: [
    { label: '首页', path: '/' },
    { label: '抢单池', path: '/repairer/pool' },
    { label: '我的工单', path: '/repairer/orders' }
  ],
  MANAGER: [
    { label: '首页', path: '/' },
    { label: '派单管理', path: '/admin/dispatch' },
    { label: '工单看板', path: '/admin/board' },
    { label: '超时管理', path: '/admin/timeout' },
    { label: '数据统计', path: '/admin/stat' },
    { label: '维修工负载', path: '/admin/repairer-load' },
    { label: '用户管理', path: '/admin/users' },
    { label: '基础数据', path: '/admin/base-data' },
    { label: '首页轮播', path: '/admin/carousel' }
  ]
}

export const ROLE_LABEL = {
  STUDENT: '学生',
  REPAIRER: '维修工',
  MANAGER: '宿舍管理员'
}
