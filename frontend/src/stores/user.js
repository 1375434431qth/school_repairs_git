import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'

function readUser() {
  try {
    return JSON.parse(sessionStorage.getItem('userInfo') || 'null')
  } catch (e) {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: sessionStorage.getItem('token') || '',
    refreshToken: sessionStorage.getItem('refreshToken') || '',
    userInfo: readUser()
  }),
  getters: {
    isLogin: (state) => !!state.token,
    role: (state) => state.userInfo?.role || '',
    roles: (state) => (state.userInfo?.role ? [state.userInfo.role] : []),
    buildingId: (state) => state.userInfo?.buildingId || null,
    displayName: (state) => state.userInfo?.realName || state.userInfo?.username || ''
  },
  actions: {
    setAuth(data) {
      this.token = data.token || ''
      this.refreshToken = data.refreshToken || ''
      this.userInfo = data.user || null
      sessionStorage.setItem('token', this.token)
      sessionStorage.setItem('refreshToken', this.refreshToken)
      sessionStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },
    async login(payload) {
      const data = await authApi.login(payload)
      this.setAuth(data)
      return data
    },
    async fetchProfile() {
      const data = await authApi.me()
      this.userInfo = data
      sessionStorage.setItem('userInfo', JSON.stringify(data))
      return data
    },
    async refresh() {
      if (!this.refreshToken) throw new Error('缺少 refreshToken')
      const data = await authApi.refresh(this.refreshToken)
      this.setAuth(data)
      return data
    },
    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // token 已失效时忽略
      }
      this.clear()
    },
    clear() {
      this.token = ''
      this.refreshToken = ''
      this.userInfo = null
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('refreshToken')
      sessionStorage.removeItem('userInfo')
    }
  }
})
