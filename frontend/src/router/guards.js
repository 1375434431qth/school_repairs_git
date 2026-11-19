import { ElMessage } from '@/utils/feedback'
import { ROLE_HOME } from './roles'

function readUser() {
  try {
    return JSON.parse(sessionStorage.getItem('userInfo') || 'null')
  } catch (e) {
    return null
  }
}

export function setupGuards(router) {
  router.beforeEach((to) => {
    document.title = `${to.meta.title || ''} - 校园宿舍报修系统`
    const token = sessionStorage.getItem('token')
    const user = readUser()

    if (to.meta.public) {
      if (token && to.name === 'Login') {
        return ROLE_HOME[user?.role] || '/'
      }
      return true
    }

    if (!token) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    const roles = to.meta.roles
    if (roles && roles.length && user?.role && !roles.includes(user.role)) {
      ElMessage.error('无权限访问')
      return ROLE_HOME[user.role] || '/'
    }
    return true
  })
}
