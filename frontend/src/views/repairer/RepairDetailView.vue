<template>
  <div v-loading="loading" class="page-container">
    <PageHeader :title="detail ? `工单 ${detail.orderNo}` : '工单详情'" :subtitle="detail ? `${detail.buildingName} ${detail.roomNo}` : ''">
      <template #actions>
        <el-button @click="router.back()">返回</el-button>
        <template v-if="detail">
          <el-button v-if="detail.status === 'ASSIGNED'" type="danger" plain @click="handleReject">拒单</el-button>
          <el-button v-if="detail.status === 'ASSIGNED'" type="primary" :loading="acting" @click="handleAccept">接单</el-button>
          <el-button v-if="['ACCEPTED', 'REWORKING'].includes(detail.status)" type="primary" @click="router.push(`/repairer/orders/${detail.id}/arrive`)">
            到场打卡
          </el-button>
          <el-button v-if="detail.status === 'ARRIVED'" @click="router.push(`/repairer/orders/${detail.id}/repair`)">
            填写维修过程
          </el-button>
          <el-button v-if="detail.status === 'ARRIVED'" type="primary" @click="router.push(`/repairer/orders/${detail.id}/finish`)">
            提交完工
          </el-button>
        </template>
      </template>
    </PageHeader>

    <template v-if="detail">
      <Panel title="工单信息">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="状态">
            <OrderStatusTag :status="detail.status" :label="detail.statusLabel" />
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <span :style="{ color: PRIORITY_MAP[detail.priority]?.color }">
              {{ PRIORITY_MAP[detail.priority]?.label || '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="故障类型">{{ detail.faultTypeName }}</el-descriptions-item>
          <el-descriptions-item label="报修人">{{ detail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">
            <a :href="detail.contactPhone ? `tel:${detail.contactPhone}` : undefined">
              {{ detail.contactPhone || '-' }}
            </a>
          </el-descriptions-item>
          <el-descriptions-item label="报修位置">{{ detail.buildingName }} {{ detail.roomNo }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="期望完成">{{ formatDateTime(detail.deadlineTime) }}</el-descriptions-item>
          <el-descriptions-item label="到场时间">{{ formatDateTime(detail.arriveTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.timeoutFlag === 1" label="超时标记" :span="3">
            <el-tag type="danger" size="small">{{ TIMEOUT_TYPE_MAP[detail.timeoutType] || '已超时' }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </Panel>

      <Panel title="故障描述">
        <div class="description-text">{{ detail.description }}</div>
      </Panel>

      <Panel title="现场照片">
        <div v-if="detail.beforeImages?.length" class="image-grid">
          <el-image
            v-for="(img, index) in detail.beforeImages"
            :key="img.fileId"
            :src="img.url"
            :preview-src-list="detail.beforeImages.map((item) => item.url)"
            :initial-index="index"
            fit="cover"
          />
        </div>
        <EmptyState v-else description="无现场照片" />
      </Panel>

      <Panel v-if="detail.repairRecord" title="已保存的维修记录">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="维修过程" :span="2">{{ detail.repairRecord.processDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完工说明" :span="2">{{ detail.repairRecord.resultDesc || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-table v-if="detail.repairRecord.parts?.length" :data="detail.repairRecord.parts" size="small" border class="mt-12">
          <el-table-column prop="partName" label="配件名称" />
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column prop="unit" label="单位" width="100" />
          <el-table-column label="单价" width="110">
            <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
          </el-table-column>
        </el-table>
        <div v-else-if="detail.repairRecord.noPartsFlag === 1" class="muted mt-12">本次维修无需更换配件</div>
      </Panel>

      <Panel v-if="detail.afterImages?.length" title="完工照片">
        <div class="image-grid">
          <el-image
            v-for="(img, index) in detail.afterImages"
            :key="img.fileId"
            :src="img.url"
            :preview-src-list="detail.afterImages.map((item) => item.url)"
            :initial-index="index"
            fit="cover"
          />
        </div>
      </Panel>

      <Panel title="流转轨迹">
        <OrderTimeline :tracks="detail.tracks || []" />
      </Panel>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import OrderStatusTag from '@/components/business/OrderStatusTag.vue'
import OrderTimeline from '@/components/business/OrderTimeline.vue'
import { PRIORITY_MAP, TIMEOUT_TYPE_MAP } from '@/composables/useOrderStatus'
import { acceptOrder, getRepairOrderDetail, rejectOrder } from '@/api/dispatch'
import { formatDateTime, formatMoney } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const acting = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    detail.value = await getRepairOrderDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

function handleStateError(error) {
  if (error?.code === 40009) {
    ElMessage.warning('工单状态已变化，已为您刷新')
    load()
  }
}

async function handleAccept() {
  acting.value = true
  try {
    await acceptOrder(detail.value.id)
    ElMessage.success('接单成功，请尽快上门')
    load()
  } catch (error) {
    handleStateError(error)
  } finally {
    acting.value = false
  }
}

async function handleReject() {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒单原因', '拒单', {
      confirmButtonText: '确认拒单',
      cancelButtonText: '取消',
      type: 'warning',
      inputValidator: (text) => (text && text.trim().length >= 2 ? true : '请填写至少 2 个字的原因')
    })
    await rejectOrder(detail.value.id, value.trim())
    ElMessage.success('已拒单，工单已退回待派单池')
    router.replace('/repairer/orders')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    handleStateError(error)
  }
}

onMounted(load)
</script>

<style scoped>
.description-text {
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-regular);
  white-space: pre-wrap;
}
</style>
