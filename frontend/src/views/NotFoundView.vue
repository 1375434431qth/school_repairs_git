<template>
  <div class="notfound">
    <div class="code">404</div>
    <h2>页面不存在</h2>
    <p>您访问的页面可能已被移除或地址有误</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ROLE_HOME } from '@/router'

const router = useRouter()
const userStore = useUserStore()

function goHome() {
  router.replace(userStore.isLogin ? ROLE_HOME[userStore.role] || '/' : '/login')
}
</script>

<style scoped>
.notfound {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: var(--bg);
}

.code {
  font-size: 88px;
  font-weight: 700;
  color: var(--brand);
  letter-spacing: 4px;
}

h2 {
  font-size: 20px;
  color: var(--text);
}

p {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-sub);
}
</style>
