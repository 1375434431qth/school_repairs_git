import { defineStore } from 'pinia'
import { ElNotification } from '@/utils/feedback'
import * as noticeApi from '@/api/notice'
import { createWsClient } from '@/utils/websocket'

/** 通知统一按生成时间倒序（同一秒按 id 倒序），保证最新始终在最上方 */
function sortByCreateTimeDesc(list) {
  return [...list].sort((a, b) => {
    const ta = String(a?.createTime || '')
    const tb = String(b?.createTime || '')
    if (ta !== tb) return tb.localeCompare(ta)
    return Number(b?.id || 0) - Number(a?.id || 0)
  })
}

export const useNoticeStore = defineStore('notice', {
  state: () => ({
    list: [],
    total: 0,
    unreadCount: 0,
    loading: false,
    client: null,
    pollTimer: null
  }),
  actions: {
    async fetchUnread() {
      try {
        const count = await noticeApi.unreadCount()
        this.unreadCount = Number(count || 0)
      } catch (e) {
        // 静默失败，下次轮询再试
      }
    },
    async fetchList(params = {}) {
      this.loading = true
      try {
        const data = await noticeApi.pageNotices({ pageNum: 1, pageSize: 20, ...params })
        this.list = sortByCreateTimeDesc(data?.records || [])
        this.total = Number(data?.total || 0)
      } finally {
        this.loading = false
      }
    },
    async markRead(id) {
      await noticeApi.markNoticeRead(id)
      const item = this.list.find((notice) => notice.id === id)
      if (item && item.readFlag === 0) {
        item.readFlag = 1
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      }
    },
    async markAllRead() {
      await noticeApi.markAllNoticesRead()
      this.list.forEach((notice) => {
        notice.readFlag = 1
      })
      this.unreadCount = 0
    },
    onPush(payload) {
      if (!payload || !payload.id) return
      // 去重后按时间倒序插入，避免延迟推送的旧通知被顶到最上方
      this.list = sortByCreateTimeDesc([
        payload,
        ...this.list.filter((notice) => notice.id !== payload.id)
      ])
      this.unreadCount += 1
      if (!document.hidden) {
        ElNotification({
          title: payload.title || '新通知',
          message: payload.content || '',
          type: 'info',
          duration: 5000
        })
      }
    },
    connect() {
      if (this.client) return
      this.client = createWsClient({
        onMessage: (payload) => this.onPush(payload),
        onOpen: () => this.stopPolling(),
        onClose: () => this.startPolling()
      })
      this.client.connect()
      this.fetchUnread()
    },
    startPolling() {
      if (this.pollTimer) return
      this.pollTimer = setInterval(() => this.fetchUnread(), 30000)
    },
    stopPolling() {
      if (this.pollTimer) clearInterval(this.pollTimer)
      this.pollTimer = null
    },
    disconnect() {
      if (this.client) this.client.close()
      this.client = null
      this.stopPolling()
      this.list = []
      this.total = 0
      this.unreadCount = 0
    }
  }
})
