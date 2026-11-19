<template>
  <div class="login-page">
    <div class="login-topbar">欢迎访问校园宿舍报修与维修工单派单系统</div>

    <div class="login-body">
      <div class="login-card">
        <div class="login-card-head">
          <div class="login-mark"><AppIcon name="building" :size="26" /></div>
          <h2>校园宿舍报修系统</h2>
          <p>学生 · 维修工 · 宿舍管理员统一登录入口</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="登录账号" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入登录账号"
              size="large"
              clearable
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入登录密码"
              size="large"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>

        <div class="login-tip">账号由管理员统一分配，忘记密码请联系宿舍管理员</div>
      </div>
    </div>

    <footer class="login-footer">© 2026 校园宿舍报修与维修工单派单系统</footer>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from '@/utils/feedback'
import AppIcon from '@/components/base/AppIcon.vue'
import { useUserStore } from '@/stores/user'
import { useNoticeStore } from '@/stores/notice'
import { ROLE_HOME } from '@/router'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const noticeStore = useNoticeStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6~20 位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (loading.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({ username: form.username.trim(), password: form.password })
    noticeStore.connect()
    ElMessage.success('登录成功')
    const redirect =
      typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
        ? route.query.redirect
        : null
    router.replace(redirect || ROLE_HOME[userStore.role] || '/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #eaf7fe 0%, #f5f8fb 46%, #eef3f8 100%);
}

.login-topbar {
  height: var(--topbar-height);
  line-height: var(--topbar-height);
  text-align: center;
  font-size: 12px;
  color: #fff;
  background: var(--brand);
}

.login-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
}

.login-card {
  width: 420px;
  max-width: 100%;
  padding: 34px 36px 28px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 12px 40px rgba(15, 111, 156, 0.14);
  border-top: 4px solid var(--brand);
}

.login-card-head {
  text-align: center;
  margin-bottom: 22px;
}

.login-mark {
  width: 54px;
  height: 54px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, #35b6ec, #1489bf);
}

.login-card-head h2 {
  font-size: 22px;
  letter-spacing: 2px;
  color: var(--text);
}

.login-card-head p {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-sub);
  letter-spacing: 1px;
}

.login-btn {
  width: 100%;
  margin-top: 6px;
  letter-spacing: 6px;
}

.login-tip {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: var(--text-sub);
}

.login-footer {
  padding: 14px 0;
  text-align: center;
  font-size: 12px;
  color: #fff;
  background: var(--brand);
}
</style>
