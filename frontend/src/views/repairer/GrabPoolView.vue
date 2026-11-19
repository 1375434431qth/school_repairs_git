<template>
  <div class="page-container">
    <PageHeader title="抢单池" subtitle="仅展示您负责楼栋内待派单的工单，先抢先得">
      <template #actions>
        <el-button :loading="loading" @click="load">
          <AppIcon name="refresh" :size="14" /> 刷新
        </el-button>
      </template>
    </PageHeader>

    <div v-loading="loading" class="pool-grid">
      <div v-for="item in list" :key="item.id" class="pool-card">
        <div class="pool-head">
          <span class="mono order-no">{{ item.orderNo }}</span>
          <OrderStatusTag :status="item.status" :label="item.statusLabel" />
        </div>
        <div class="pool-title">{{ item.faultTypeName }}</div>
        <div class="pool-location">
          <AppIcon name="building" :size="14" /> {{ item.buildingName }} {{ item.roomNo }}
        </div>
        <div class="pool-meta">
          <span :style="{ color: PRIORITY_MAP[item.priority]?.color }">
            优先级：{{ PRIORITY_MAP[item.priority]?.label || '-' }}
          </span>
          <span>提交：{{ formatDateTime(item.createTime) }}</span>
        </div>
        <div class="pool-meta">
          <span>期望完成：{{ formatDateTime(item.deadlineTime) }}</span>
        </div>
        <el-button
          type="primary"
          class="grab-btn"
          :loading="grabbingId === item.id"
          :disabled="cooldown > 0"
          @click="handleGrab(item)"
        >
          {{ cooldown > 0 ? `请稍候 ${cooldown}s` : '立即抢单' }}
        </el-button>
      </div>
    </div>

    <EmptyState v-if="!loading && !list.length" description="当前没有可抢的工单，稍后再来看看" />
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import AppIcon from '@/components/base/AppIcon.vue'
import OrderStatusTag from '@/components/business/OrderStatusTag.vue'
import { PRIORITY_MAP } from '@/composables/useOrderStatus'
import { grabOrder, poolOrders } from '@/api/dispatch'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const grabbingId = ref(null)
const cooldown = ref(0)
let cooldownTimer = null

async function load() {
  loading.value = true
  try {
    list.value = (await poolOrders()) || []
  } finally {
    loading.value = false
  }
}

function startCooldown(seconds = 3) {
  cooldown.value = seconds
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}

async function handleGrab(item) {
  if (grabbingId.value || cooldown.value > 0) return
  grabbingId.value = item.id
  startCooldown(3)
  try {
    await grabOrder(item.id)
    ElMessage.success('抢单成功，请尽快接单并上门')
    router.push(`/repairer/orders/${item.id}`)
  } catch (error) {
    if (error?.code === 40901) {
      ElMessage.warning('手慢了，该工单已被抢')
      list.value = list.value.filter((order) => order.id !== item.id)
    }
  } finally {
    grabbingId.value = null
  }
}

onMounted(load)
onBeforeUnmount(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<style scoped>
.pool-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 120px;
}

.pool-card {
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  border-top: 3px solid var(--brand);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pool-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-no {
  font-size: 13px;
  color: var(--text-sub);
}

.pool-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
}

.pool-location {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-regular);
  font-size: 13px;
}

.pool-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-sub);
}

.grab-btn {
  margin-top: 8px;
  width: 100%;
}

@media (max-width: 1080px) {
  .pool-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
