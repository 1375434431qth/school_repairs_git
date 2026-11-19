<template>
  <div v-loading="loading" class="page-container">
    <PageHeader :title="detail ? `工单 ${detail.orderNo}` : '工单详情'" :subtitle="detail ? `${detail.buildingName} ${detail.roomNo}` : ''">
      <template #actions>
        <el-button @click="router.back()">返回</el-button>
        <el-button v-if="canCancel" type="danger" plain @click="handleCancel">取消报修</el-button>
        <el-button v-if="canConfirm" type="primary" @click="handleConfirm">确认完工</el-button>
        <el-button v-if="canReopen" type="warning" plain @click="handleReopen">反馈未修好</el-button>
        <el-button v-if="canEvaluate" type="primary" @click="router.push(`/student/orders/${detail.id}/evaluate`)">
          去评价
        </el-button>
      </template>
    </PageHeader>

    <template v-if="detail">
      <Panel title="基本信息">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="工单状态">
            <OrderStatusTag :status="detail.status" :label="detail.statusLabel" />
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <span :style="{ color: PRIORITY_MAP[detail.priority]?.color }">
              {{ PRIORITY_MAP[detail.priority]?.label || '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="故障类型">{{ detail.faultTypeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报修楼栋">{{ detail.buildingName }}</el-descriptions-item>
          <el-descriptions-item label="房间号">{{ detail.roomNo }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="期望完成">{{ formatDateTime(detail.deadlineTime) }}</el-descriptions-item>
          <el-descriptions-item label="维修工">{{ detail.assigneeName || '待派单' }}</el-descriptions-item>
          <el-descriptions-item label="到场时间">{{ formatDateTime(detail.arriveTime) }}</el-descriptions-item>
          <el-descriptions-item label="完工时间">{{ formatDateTime(detail.finishTime) }}</el-descriptions-item>
          <el-descriptions-item label="确认时间">{{ formatDateTime(detail.confirmTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.isRework === 1" label="返修来源">
            {{ detail.sourceOrderId ? `原工单 #${detail.sourceOrderId}` : '返修单' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.timeoutFlag === 1" label="超时">
            <el-tag type="danger" size="small">{{ TIMEOUT_TYPE_MAP[detail.timeoutType] || '已超时' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.cancelReason" label="取消原因" :span="3">
            {{ detail.cancelReason }}
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

      <Panel v-if="detail.repairRecord" title="维修信息">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="维修工">{{ detail.repairRecord.repairerName || detail.assigneeName }}</el-descriptions-item>
          <el-descriptions-item label="维修耗时">{{ formatDuration(detail.repairRecord.costMinutes) }}</el-descriptions-item>
          <el-descriptions-item label="维修过程" :span="2">{{ detail.repairRecord.processDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完工说明" :span="2">{{ detail.repairRecord.resultDesc || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detail.repairRecord.parts?.length" class="mt-12">
          <div class="sub-title">更换配件</div>
          <el-table :data="detail.repairRecord.parts" size="small" border>
            <el-table-column prop="partName" label="配件名称" />
            <el-table-column prop="quantity" label="数量" width="100" />
            <el-table-column prop="unit" label="单位" width="100" />
            <el-table-column label="单价" width="110">
              <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
            </el-table-column>
          </el-table>
        </div>
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

      <Panel v-if="detail.evaluation" title="我的评价">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="评分">
            <el-rate :model-value="detail.evaluation.score" disabled />
          </el-descriptions-item>
          <el-descriptions-item label="评价人">
            {{ detail.evaluation.anonymous === 1 ? '匿名' : detail.evaluation.studentName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">{{ detail.evaluation.tags || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评价内容" :span="2">{{ detail.evaluation.content || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评价时间" :span="2">{{ formatDateTime(detail.evaluation.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </Panel>

      <Panel v-if="detail.dispatchRecords?.length" title="派单记录">
        <el-table :data="detail.dispatchRecords" size="small" border>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ DISPATCH_TYPE_MAP[row.dispatchType] || row.dispatchType }}</template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="120" />
          <el-table-column prop="assigneeName" label="维修工" width="120" />
          <el-table-column label="优先级变化" width="130">
            <template #default="{ row }">
              <span v-if="row.beforePriority || row.afterPriority">
                {{ PRIORITY_MAP[row.beforePriority]?.label || '-' }} → {{ PRIORITY_MAP[row.afterPriority]?.label || '-' }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="180" />
          <el-table-column label="时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </Panel>

      <Panel v-if="detail.rounds?.length" title="维修轮次">
        <el-table :data="detail.rounds" size="small" border>
          <el-table-column prop="roundNo" label="轮次" width="70" />
          <el-table-column prop="repairerName" label="维修工" width="110" />
          <el-table-column prop="statusLabel" label="状态" width="90" />
          <el-table-column prop="reworkReason" label="返修原因" min-width="180">
            <template #default="{ row }">{{ row.reworkReason || '-' }}</template>
          </el-table-column>
          <el-table-column label="完工时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.finishTime) }}</template>
          </el-table-column>
        </el-table>
      </Panel>

      <Panel title="流转轨迹">
        <OrderTimeline :tracks="detail.tracks || []" />
      </Panel>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import OrderStatusTag from '@/components/business/OrderStatusTag.vue'
import OrderTimeline from '@/components/business/OrderTimeline.vue'
import { PRIORITY_MAP, TIMEOUT_TYPE_MAP, DISPATCH_TYPE_MAP } from '@/composables/useOrderStatus'
import { cancelOrder, confirmOrder, getOrderDetail, reopenOrder } from '@/api/order'
import { formatDateTime, formatDuration, formatMoney } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)

const orderId = computed(() => route.params.id)
const canCancel = computed(() => ['CREATED', 'ASSIGNED'].includes(detail.value?.status))
const canConfirm = computed(() => detail.value?.status === 'FINISHED')
const canReopen = computed(() => ['FINISHED', 'CONFIRMED'].includes(detail.value?.status))
const canEvaluate = computed(() => detail.value?.status === 'CONFIRMED')

async function load() {
  loading.value = true
  try {
    detail.value = await getOrderDetail(orderId.value)
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

async function handleCancel() {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消报修', {
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
      type: 'warning',
      inputValidator: (text) => (text && text.trim().length >= 2 ? true : '请填写至少 2 个字的原因')
    })
    await cancelOrder(orderId.value, value.trim())
    ElMessage.success('已取消报修')
    load()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    handleStateError(error)
  }
}

async function handleConfirm() {
  try {
    await ElMessageBox.confirm('确认维修已完成？确认后可对本次维修进行评价。', '确认完工', {
      confirmButtonText: '确认完成',
      cancelButtonText: '再看看',
      type: 'info'
    })
    await confirmOrder(orderId.value)
    ElMessage.success('已确认完工')
    load()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    handleStateError(error)
  }
}

async function handleReopen() {
  try {
    const { value } = await ElMessageBox.prompt(
      '请描述仍未解决的问题，维修工将按此继续返修（必填，最多 200 字）。',
      '反馈未修好',
      {
        confirmButtonText: '发起返修',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：水龙头仍然滴水，接头处渗水',
        inputValidator: (text) => {
          if (!text || !text.trim()) return '请填写未修好的具体原因'
          if (text.trim().length > 200) return '原因不能超过 200 字'
          return true
        }
      }
    )
    const data = await reopenOrder(orderId.value, { reason: value.trim() })
    ElMessage.success('已发起返修，工单进入返修中')
    router.replace(`/student/orders/${data.orderId}`)
    load()
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

.sub-title {
  margin-bottom: 8px;
  font-weight: 600;
  color: var(--text);
}
</style>
