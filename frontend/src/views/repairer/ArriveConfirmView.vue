<template>
  <div class="page-container">
    <PageHeader title="确认到场" subtitle="到场时间由服务端记录，请到达维修现场后操作" />

    <Panel v-loading="loading" title="工单信息">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="工单号"><span class="mono">{{ detail.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="状态">
          <OrderStatusTag :status="detail.status" :label="detail.statusLabel" />
        </el-descriptions-item>
        <el-descriptions-item label="报修位置">{{ detail.buildingName }} {{ detail.roomNo }}</el-descriptions-item>
        <el-descriptions-item label="故障类型">{{ detail.faultTypeName }}</el-descriptions-item>
        <el-descriptions-item label="报修人">{{ detail.studentName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{ detail.description }}</el-descriptions-item>
      </el-descriptions>

      <div class="arrive-actions">
        <el-button type="primary" size="large" :loading="submitting" @click="handleArrive">我已到达现场</el-button>
        <el-button size="large" @click="router.back()">返回</el-button>
      </div>
    </Panel>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import OrderStatusTag from '@/components/business/OrderStatusTag.vue'
import { arriveOrder, getRepairOrderDetail } from '@/api/dispatch'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    detail.value = await getRepairOrderDetail(route.params.id)
    if (!['ACCEPTED', 'REWORKING'].includes(detail.value?.status)) {
      await ElMessageBox.alert('当前工单不是“待上门/返修中”状态，无法登记到场', '提示', { type: 'warning' })
      router.replace(`/repairer/orders/${route.params.id}`)
    }
  } finally {
    loading.value = false
  }
}

async function handleArrive() {
  submitting.value = true
  try {
    await arriveOrder(detail.value.id)
    ElMessage.success('到场登记成功，请开始维修')
    router.replace(`/repairer/orders/${detail.value.id}`)
  } catch (error) {
    if (error?.code === 40009) {
      ElMessage.warning('工单状态已变化，请返回列表刷新')
      router.replace('/repairer/orders')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.arrive-actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
