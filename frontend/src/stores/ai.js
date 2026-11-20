import { defineStore } from 'pinia'
import { markRaw } from 'vue'
import { chatStream } from '@/api/ai'
import { ElMessage } from '@/utils/feedback'

const CONVERSATION_KEY = 'aiConversationId'
const MAX_MESSAGES = 40

export const useAiStore = defineStore('ai', {
  state: () => ({
    messages: [],
    loading: false,
    conversationId: sessionStorage.getItem(CONVERSATION_KEY) || '',
    controller: null,
    seq: 0
  }),
  actions: {
    async send(text, page) {
      const content = (text || '').trim()
      if (!content || this.loading) return

      this.messages.push({ id: ++this.seq, role: 'user', content })
      this.messages.push({
        id: ++this.seq,
        role: 'assistant',
        content: '',
        links: [],
        status: 'streaming',
        error: ''
      })
      this.trim()
      // 必须使用数组中的响应式引用，直接改原始对象不会触发视图更新
      const target = this.messages[this.messages.length - 1]
      this.loading = true
      const controller = markRaw(new AbortController())
      this.controller = controller

      try {
        await chatStream(
          {
            conversationId: this.conversationId,
            message: content,
            page
          },
          {
            meta: (data) => {
              if (data && data.conversationId) {
                this.conversationId = data.conversationId
                sessionStorage.setItem(CONVERSATION_KEY, data.conversationId)
              }
            },
            links: (data) => {
              target.links = Array.isArray(data) ? data : []
            },
            delta: (data) => {
              target.content += (data && data.content) || ''
            },
            done: () => {
              target.status = 'done'
            },
            error: (data) => {
              target.status = 'error'
              target.error = (data && data.message) || '智能助手暂不可用，请稍后重试'
              ElMessage.error(target.error)
            }
          },
          controller.signal
        )
        if (target.status === 'streaming') {
          target.status = 'done'
        }
      } catch (error) {
        if (controller.signal.aborted || (error && error.name === 'AbortError')) {
          target.status = 'done'
          if (!target.content) {
            target.content = '已停止生成'
          }
        } else if (!error || !error.handled) {
          target.status = 'error'
          target.error = (error && error.message) || '请求失败'
          ElMessage.error(target.error)
        }
      } finally {
        this.loading = false
        this.controller = null
      }
    },

    stop() {
      if (this.controller) {
        this.controller.abort()
      }
      this.loading = false
    },

    retryLast(page) {
      if (this.loading) return
      let lastUserIndex = -1
      for (let i = this.messages.length - 1; i >= 0; i--) {
        if (this.messages[i].role === 'user') {
          lastUserIndex = i
          break
        }
      }
      if (lastUserIndex < 0) return
      const content = this.messages[lastUserIndex].content
      this.messages.splice(lastUserIndex)
      this.send(content, page)
    },

    reset() {
      if (this.controller) {
        this.controller.abort()
      }
      this.messages = []
      this.loading = false
      this.controller = null
      this.conversationId = ''
      sessionStorage.removeItem(CONVERSATION_KEY)
    },

    trim() {
      if (this.messages.length > MAX_MESSAGES) {
        this.messages.splice(0, this.messages.length - MAX_MESSAGES)
      }
    }
  }
})
