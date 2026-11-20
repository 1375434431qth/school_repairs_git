<template>
  <div class="ai-assistant">
    <button class="ai-fab" type="button" aria-label="打开智能助手" @click="open">
      <span class="ai-fab-badge">AI</span>
      <span class="ai-fab-text">智能助手</span>
    </button>

    <el-drawer
      v-model="visible"
      class="ai-drawer"
      direction="rtl"
      size="420px"
      :with-header="false"
      :append-to-body="true"
    >
      <div class="ai-panel">
        <header class="ai-header">
          <div class="ai-title">
            <span class="ai-title-badge">AI</span>
            <span>智能助手</span>
          </div>
          <div class="ai-header-actions">
            <el-button
              link
              type="primary"
              :disabled="!aiStore.messages.length || aiStore.loading"
              @click="clear"
            >
              清空
            </el-button>
            <el-button link @click="visible = false">关闭</el-button>
          </div>
        </header>

        <div ref="messagesRef" class="ai-messages">
          <div v-if="!aiStore.messages.length" class="ai-welcome">
            <div class="ai-welcome-badge">AI</div>
            <div class="ai-welcome-title">你好，我是报修系统智能助手</div>
            <div class="ai-welcome-desc">
              可以问我系统怎么用、工单状态、派单/抢单/评价规则，也可以让我帮你找操作入口。
            </div>
          </div>

          <div
            v-for="msg in aiStore.messages"
            :key="msg.id"
            class="ai-message"
            :class="msg.role"
          >
            <div class="ai-bubble">
              <div
                v-if="msg.role === 'assistant' && !msg.content && msg.status === 'streaming'"
                class="ai-typing"
              >
                <i></i><i></i><i></i>
              </div>
              <div v-else-if="msg.content" class="ai-text">{{ msg.content }}</div>

              <span
                v-if="msg.role === 'assistant' && msg.status === 'streaming' && msg.content"
                class="ai-cursor"
              ></span>

              <div v-if="msg.links && msg.links.length && msg.status !== 'streaming'" class="ai-links">
                <el-button
                  v-for="link in msg.links"
                  :key="link.path"
                  link
                  type="primary"
                  @click="go(link)"
                >
                  {{ link.label }} →
                </el-button>
              </div>

              <div v-if="msg.status === 'error'" class="ai-error">
                <span>{{ msg.error || '回答失败' }}</span>
                <el-button link type="primary" :disabled="aiStore.loading" @click="retry">
                  重试
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="quickQuestions.length" class="ai-quick">
          <button
            v-for="question in quickQuestions"
            :key="question"
            class="ai-quick-item"
            type="button"
            :disabled="aiStore.loading"
            @click="send(question)"
          >
            {{ question }}
          </button>
        </div>

        <div class="ai-input">
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="500"
            show-word-limit
            placeholder="输入你的问题，Enter 发送，Shift+Enter 换行"
            @keydown.enter.exact.prevent="send()"
          />
          <div class="ai-input-actions">
            <el-button v-if="aiStore.loading" type="warning" plain @click="stop">
              停止生成
            </el-button>
            <el-button
              type="primary"
              :loading="aiStore.loading"
              :disabled="!input.trim()"
              @click="send()"
            >
              发送
            </el-button>
          </div>
          <div class="ai-disclaimer">回答由 DeepSeek 生成，仅供参考</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAiStore } from '@/stores/ai'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/feedback'

const QUICK_QUESTIONS = {
  STUDENT: ['怎么提交报修？', '工单一直没人接怎么办？', '怎么发起返修？', '评价规则是什么？'],
  REPAIRER: ['抢单规则是什么？', '超时怎么计算？', '完工需要填写什么？'],
  MANAGER: ['如何派单？', '怎么标记超时？', '返修率怎么算？', '看板各列代表什么？']
}

const aiStore = useAiStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const visible = ref(false)
const input = ref('')
const messagesRef = ref(null)

const quickQuestions = computed(() => QUICK_QUESTIONS[userStore.role] || [])

function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function open() {
  visible.value = true
  scrollToBottom()
}

function send(text) {
  const content = (typeof text === 'string' ? text : input.value).trim()
  if (!content) {
    ElMessage.warning('请输入问题')
    return
  }
  if (aiStore.loading) return
  input.value = ''
  aiStore.send(content, route.path)
  scrollToBottom()
}

function stop() {
  aiStore.stop()
}

function retry() {
  aiStore.retryLast(route.path)
}

function clear() {
  aiStore.reset()
  ElMessage.success('已清空对话')
}

function go(link) {
  if (!link || !link.path || !link.path.startsWith('/') || link.path.startsWith('/login')) {
    ElMessage.warning('页面地址无效')
    return
  }
  visible.value = false
  router.push(link.path)
}

watch(
  () => aiStore.messages,
  () => scrollToBottom(),
  { deep: true }
)

onBeforeUnmount(() => {
  aiStore.reset()
})
</script>

<style scoped>
.ai-fab {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 2200;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  padding: 0 16px 0 10px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--brand), var(--brand-deep));
  color: #fff;
  font-size: 14px;
  box-shadow: 0 8px 22px rgba(15, 111, 156, 0.28);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.ai-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(15, 111, 156, 0.34);
}

.ai-fab-badge {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.24);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.ai-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f6f9fc;
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: #fff;
  border-bottom: 1px solid var(--border);
}

.ai-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text);
  font-size: 15px;
  font-weight: 700;
}

.ai-title-badge,
.ai-welcome-badge {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 9px;
  background: var(--brand-light);
  color: var(--brand-deep);
  font-size: 11px;
  font-weight: 700;
}

.ai-header-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-welcome {
  margin: auto 0;
  padding: 18px;
  border-radius: 12px;
  background: #fff;
  box-shadow: var(--shadow);
  text-align: center;
}

.ai-welcome-badge {
  margin: 0 auto 10px;
}

.ai-welcome-title {
  color: var(--text);
  font-size: 15px;
  font-weight: 700;
}

.ai-welcome-desc {
  margin-top: 8px;
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.8;
}

.ai-message {
  display: flex;
}

.ai-message.user {
  justify-content: flex-end;
}

.ai-message.assistant {
  justify-content: flex-start;
}

.ai-bubble {
  max-width: 86%;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.75;
  word-break: break-word;
}

.ai-message.user .ai-bubble {
  background: var(--brand);
  color: #fff;
  border-bottom-right-radius: 2px;
}

.ai-message.assistant .ai-bubble {
  background: #fff;
  color: var(--text);
  border-bottom-left-radius: 2px;
  box-shadow: var(--shadow);
}

.ai-text {
  white-space: pre-wrap;
}

.ai-typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 18px;
}

.ai-typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-sub);
  animation: ai-bounce 1s infinite ease-in-out;
}

.ai-typing i:nth-child(2) {
  animation-delay: 0.15s;
}

.ai-typing i:nth-child(3) {
  animation-delay: 0.3s;
}

.ai-cursor {
  display: inline-block;
  width: 2px;
  height: 14px;
  margin-left: 2px;
  background: var(--brand);
  vertical-align: -2px;
  animation: ai-blink 1s steps(2, start) infinite;
}

.ai-links {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px dashed var(--border);
}

.ai-links :deep(.el-button) {
  height: auto;
  padding: 0;
  font-size: 12px;
}

.ai-error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  color: #d4380d;
  font-size: 12px;
}

.ai-error :deep(.el-button) {
  height: auto;
  padding: 0;
  font-size: 12px;
}

.ai-quick {
  display: flex;
  gap: 6px;
  padding: 8px 12px;
  overflow-x: auto;
  background: #fff;
  border-top: 1px solid var(--border);
}

.ai-quick-item {
  flex: none;
  padding: 6px 10px;
  border: 1px solid var(--brand-border);
  border-radius: 999px;
  background: var(--brand-light);
  color: var(--brand-deep);
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
}

.ai-quick-item:hover:not(:disabled) {
  background: #dcf1fb;
}

.ai-quick-item:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.ai-input {
  padding: 10px 12px 10px;
  background: #fff;
  border-top: 1px solid var(--border);
}

.ai-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.ai-disclaimer {
  margin-top: 6px;
  color: var(--text-sub);
  font-size: 11px;
  text-align: right;
}

@keyframes ai-bounce {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

@keyframes ai-blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

@media (max-width: 640px) {
  .ai-fab {
    right: 16px;
    bottom: 16px;
  }

  .ai-fab-text {
    display: none;
  }

  .ai-fab {
    width: 48px;
    padding: 0;
    justify-content: center;
  }
}
</style>

<style>
.ai-drawer .el-drawer__body {
  padding: 0;
}

@media (max-width: 640px) {
  .ai-drawer {
    width: 100% !important;
  }
}
</style>
