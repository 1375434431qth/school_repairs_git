import { defineStore } from 'pinia'
import { listCarousels } from '@/api/carousel'

/** 首页轮播：会话内只请求一次，管理端写操作后调 reload() 强制刷新 */
export const useCarouselStore = defineStore('carousel', {
  state: () => ({
    list: [],
    loaded: false,
    loading: false
  }),
  actions: {
    async load() {
      if (this.loaded) return this.list
      return this.reload()
    },
    async reload() {
      this.loading = true
      try {
        this.list = (await listCarousels()) || []
        this.loaded = true
        return this.list
      } finally {
        this.loading = false
      }
    }
  }
})
