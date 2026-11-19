import { defineStore } from 'pinia'

/** 列表筛选与详情缓存：只缓存查询条件与最近打开的详情，不缓存状态流转结果 */
export const useOrderStore = defineStore('order', {
  state: () => ({
    filters: {
      status: '',
      keyword: ''
    },
    pageCache: {},
    currentOrder: null
  }),
  actions: {
    setFilters(filters) {
      this.filters = { ...this.filters, ...filters }
    },
    setCurrent(order) {
      this.currentOrder = order
    },
    invalidate() {
      this.currentOrder = null
      this.pageCache = {}
    }
  }
})
