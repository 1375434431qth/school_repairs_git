<template>
  <div class="app-shell">
    <!-- 顶部蓝色通栏：与效果图一致 -->
    <div class="app-topbar">
      <div class="app-topbar-inner">
        <span>欢迎访问校园宿舍报修与维修工单派单系统</span>
        <div class="topbar-right">
          <template v-if="userStore.isLogin">
            <span class="topbar-user">
              {{ userStore.displayName }}
              <em>{{ roleLabel }}</em>
            </span>
            <button class="topbar-bell" type="button" title="站内通知" @click="openNotices">
              <AppIcon name="bell" :size="16" />
              <i v-if="noticeStore.unreadCount > 0" class="badge">
                {{ noticeStore.unreadCount > 99 ? '99+' : noticeStore.unreadCount }}
              </i>
            </button>
            <button class="topbar-link" type="button" @click="handleLogout">
              <AppIcon name="logout" :size="14" />
              退出
            </button>
          </template>
          <router-link v-else to="/login" class="topbar-link">请登录</router-link>
        </div>
      </div>
    </div>

    <!-- 白色页头：Logo + 横向导航 -->
    <header class="app-header">
      <div class="app-header-inner">
        <router-link to="/" class="brand">
          <img class="brand-logo" src="/images/logo.png" alt="重庆城市科技学院" />
          <span class="brand-text">
            <strong>校园宿舍报修系统</strong>
            <small>学生报修 · 维修派单 · 工单跟踪</small>
          </span>
        </router-link>
        <nav class="app-nav">
          <router-link
            v-for="item in menus"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
          >
            {{ item.label }}
          </router-link>
        </nav>
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>

    <footer class="app-footer">
      <div class="app-footer-inner">
        <span>校园宿舍报修与维修工单派单系统</span>
        <span>服务电话：023-0000-0000</span>
        <span>© 2026 CQUST</span>
      </div>
    </footer>

    <el-drawer v-model="drawerVisible" title="站内通知" size="380px">
      <div class="notice-toolbar">
        <span class="muted">未读 {{ noticeStore.unreadCount }} 条</span>
        <el-button link type="primary" :disabled="!noticeStore.unreadCount" @click="markAll">
          全部已读
        </el-button>
      </div>
      <div v-loading="noticeStore.loading" class="notice-list">
        <div
          v-for="item in noticeStore.list"
          :key="item.id"
          class="notice-item"
          :class="{ unread: item.readFlag === 0 }"
          @click="openNotice(item)"
        >
          <div class="notice-title">{{ item.title }}</div>
          <div class="notice-content">{{ item.content }}</div>
          <div class="notice-time">{{ formatDateTime(item.createTime) }}</div>
        </div>
        <EmptyState v-if="!noticeStore.list.length" description="暂无通知" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/base/AppIcon.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { useNoticeStore } from '@/stores/notice'
import { MENUS, ROLE_LABEL } from './menus'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const noticeStore = useNoticeStore()
const drawerVisible = ref(false)

const roleLabel = computed(() => ROLE_LABEL[userStore.role] || userStore.role)
const menus = computed(() => MENUS[userStore.role] || [])

/** 首页只做精确匹配，其余按前缀匹配，避免「首页」在所有页面都高亮 */
function isActive(path) {
  return path === '/' ? route.path === '/' : route.path === path || route.path.startsWith(`${path}/`)
}

function openNotices() {
  drawerVisible.value = true
  noticeStore.fetchList().then(() => {
    if (!noticeStore.list.length) noticeStore.fetchUnread()
  })
}

async function markAll() {
  await noticeStore.markAllRead()
}

async function openNotice(item) {
  if (item.readFlag === 0) {
    await noticeStore.markRead(item.id)
  }
  if (!item.orderId) return
  drawerVisible.value = false
  if (userStore.role === 'STUDENT') {
    router.push(`/student/orders/${item.orderId}`)
  } else if (userStore.role === 'REPAIRER') {
    router.push(`/repairer/orders/${item.orderId}`)
  }
}

async function handleLogout() {
  await userStore.logout()
  noticeStore.disconnect()
  router.replace('/login')
}

onMounted(() => {
  if (userStore.isLogin) {
    noticeStore.connect()
  }
})

onBeforeUnmount(() => {
  noticeStore.disconnect()
})
</script>

<style scoped>
.app-shell {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.app-topbar {
  height: var(--topbar-height);
  background: var(--brand);
  color: #fff;
}

.app-topbar-inner {
  width: 1200px;
  max-width: calc(100% - 32px);
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.topbar-user {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.topbar-user em {
  font-style: normal;
  padding: 0 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.22);
}

.topbar-bell,
.topbar-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: transparent;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  opacity: 0.92;
}

.topbar-bell:hover,
.topbar-link:hover {
  opacity: 1;
}

.badge {
  position: absolute;
  top: -7px;
  right: -9px;
  min-width: 15px;
  height: 15px;
  padding: 0 4px;
  border-radius: 8px;
  background: #f5222d;
  color: #fff;
  font-size: 10px;
  font-style: normal;
  line-height: 15px;
  text-align: center;
}

.app-header {
  height: var(--header-height);
  background: #fff;
  box-shadow: 0 2px 6px rgba(15, 111, 156, 0.08);
  position: relative;
  z-index: 5;
}

.app-header-inner {
  width: 1200px;
  max-width: calc(100% - 32px);
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text);
}

.brand-logo {
  width: 46px;
  height: 46px;
  object-fit: contain;
  flex: none;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-text strong {
  font-size: 22px;
  letter-spacing: 2px;
  font-weight: 700;
}

.brand-text small {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-sub);
  letter-spacing: 1px;
}

.app-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}

.nav-item {
  position: relative;
  padding: 0 16px;
  height: 46px;
  display: inline-flex;
  align-items: center;
  font-size: 15px;
  color: var(--text-regular);
  border-left: 1px solid var(--border);
}

.nav-item:first-child {
  border-left: none;
}

.nav-item:hover {
  color: var(--brand);
}

.nav-item.active {
  color: var(--brand);
  font-weight: 700;
}

.nav-item.active::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 0;
  height: 3px;
  border-radius: 2px 2px 0 0;
  background: var(--brand);
}

.app-main {
  flex: 1;
}

.app-footer {
  margin-top: 24px;
  background: var(--brand);
  color: rgba(255, 255, 255, 0.92);
}

.app-footer-inner {
  width: 1200px;
  max-width: calc(100% - 32px);
  margin: 0 auto;
  padding: 16px 0;
  display: flex;
  justify-content: center;
  gap: 28px;
  font-size: 12px;
}

.notice-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.notice-list {
  min-height: 120px;
}

.notice-item {
  padding: 12px 4px;
  border-bottom: 1px dashed var(--border);
  cursor: pointer;
}

.notice-item:hover {
  background: #fafdff;
}

.notice-item.unread .notice-title::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 6px;
  border-radius: 50%;
  background: #f5222d;
  vertical-align: middle;
}

.notice-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.notice-content {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-regular);
  line-height: 1.7;
}

.notice-time {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-sub);
}
</style>
