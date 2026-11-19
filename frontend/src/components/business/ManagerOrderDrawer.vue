<template>
  <el-drawer
    :model-value="modelValue"
    title="工单详情"
    size="640px"
    @update:model-value="(value) => emit('update:modelValue', value)"
    @open="load"
  >
    <div v-loading="loading" class="drawer-body">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单号" :span="2">
            <span class="mono">{{ detail.orderNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <OrderStatusTag :status="detail.status" :label="detail.statusLabel" />
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <span :style="{ color: PRIORITY_MAP[detail.priority]?.color }">
              {{ PRIORITY_MAP[detail.priority]?.label || '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="报修位置">{{ detail.buildingName }} {{ detail.roomNo }}</el-descriptions-item>
          <el-descriptions-item label="故障类型">{{ detail.faultTypeName }}</el-descriptions-item>
          <el-descriptions-item label="学生">{{ detail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修工">{{ detail.assigneeName || '待派单' }}</el-descriptions-item>
          <el-descriptions-item label="期望完成">{{ formatDateTime(detail.deadlineTime) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.timeoutFlag === 1" label="超时" :span="2">
            <el-tag type="danger" size="small">{{ TIMEOUT_TYPE_MAP[detail.timeoutType] || '已超时' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="故障描述" :span="2">{{ detail.description }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.beforeImages?.length" class="section">
          <div class="section-title">现场照片</div>
          <div class="image-grid">
            <el-image
              v-for="(img, index) in detail.beforeImages"
              :key="img.fileId"
              :src="img.url"
              :preview-src-list="detail.beforeImages.map((item) => item.url)"
              :initial-index="index"
              fit="cover"
            />
          </div>
        </div>

        <div v-if="detail.repairRecord" class="section">
          <div class="section-title">维修信息</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="维修工">{{ detail.repairRecord.repairerName || detail.assigneeName }}</el-descriptions-item>
            <el-descriptions-item label="维修过程">{{ detail.repairRecord.processDesc || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完工说明">{{ detail.repairRecord.resultDesc || '-' }}</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ formatDuration(detail.repairRecord.costMinutes) }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="detail.repairRecord.parts?.length" :data="detail.repairRecord.parts" size="small" border class="mt-12">
            <el-table-column prop="partName" label="配件" />
            <el-table-column prop="quantity" label="数量" width="90" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column label="单价" width="100">
              <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="detail.afterImages?.length" class="section">
          <div class="section-title">完工照片</div>
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
        </div>

        <div v-if="detail.evaluation" class="section">
          <div class="section-title">学生评价</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="评分">
              <el-rate :model-value="detail.evaluation.score" disabled />
            </el-descriptions-item>
            <el-descriptions-item label="标签">{{ detail.evaluation.tags || '-' }}</el-descriptions-item>
            <el-descriptions-item label="内容">{{ detail.evaluation.content || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="section">
          <div class="section-title">流转轨迹</div>
          <OrderTimeline :tracks="detail.tracks || []" />
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import OrderStatusTag from '@/components/business/OrderStatusTag.vue'
import OrderTimeline from '@/components/business/OrderTimeline.vue'
import { PRIORITY_MAP, TIMEOUT_TYPE_MAP } from '@/composables/useOrderStatus'
import { getManagerOrderDetail } from '@/api/manager'
import { formatDateTime, formatDuration, formatMoney } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  orderId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue'])
const loading = ref(false)
const detail = ref(null)

async function load() {
  if (!props.orderId) return
  loading.value = true
  try {
    detail.value = await getManagerOrderDetail(props.orderId)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.orderId,
  (value) => {
    if (value) detail.value = null
  }
)
</script>

<style scoped>
.drawer-body {
  min-height: 200px;
}

.section {
  margin-top: 18px;
}

.section-title {
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid var(--brand);
  font-weight: 700;
  color: var(--text);
}
</style>
