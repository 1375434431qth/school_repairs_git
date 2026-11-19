<template>
  <div class="page-container">
    <PageHeader title="派单管理" subtitle="按楼栋筛选待派单工单，指派维修工或调整优先级" />

    <Panel>
      <div class="toolbar">
        <el-select v-model="filters.buildingId" placeholder="全部楼栋" clearable style="width: 170px" @change="handleSearch">
          <el-option v-for="item in dictStore.buildings" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px" @change="handleSearch">
          <el-option v-for="item in STATUS_LIST" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          placeholder="工单号 / 楼栋房间"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ pagination.total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="orderNo" label="工单号" width="180">
          <template #default="{ row }"><span class="mono">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="报修位置" min-width="150">
          <template #default="{ row }">{{ row.buildingName }} {{ row.roomNo }}</template>
        </el-table-column>
        <el-table-column prop="faultTypeName" label="故障类型" min-width="120" />
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <span :style="{ color: PRIORITY_MAP[row.priority]?.color }">
              {{ PRIORITY_MAP[row.priority]?.label || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><OrderStatusTag :status="row.status" :label="row.statusLabel" /></template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="维修工" width="110">
          <template #default="{ row }">{{ row.assigneeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'CREATED'" link type="primary" @click="openAssign(row)">派单</el-button>
            <el-button
              v-if="['CREATED', 'ASSIGNED', 'ACCEPTED', 'ARRIVED'].includes(row.status)"
              link
              type="warning"
              @click="openPriority(row)"
            >
              调优先级
            </el-button>
            <el-button
              v-if="['ASSIGNED', 'ACCEPTED', 'ARRIVED'].includes(row.status)"
              link
              type="success"
              @click="handleUrge(row)"
            >
              催单
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && !list.length" description="当前筛选条件下暂无工单" />

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadList"
          @size-change="handleSearch"
        />
      </div>
    </Panel>

    <el-dialog v-model="assignVisible" title="派单" width="520px">
      <el-form :model="assignForm" label-width="90px">
        <el-form-item label="工单">
          <span class="mono">{{ currentRow?.orderNo }}</span>
          <span class="muted" style="margin-left: 10px">{{ currentRow?.buildingName }} {{ currentRow?.roomNo }}</span>
        </el-form-item>
        <el-form-item label="维修工" required>
          <RepairerPicker v-model="assignForm.assigneeId" :building-id="currentRow?.buildingId" />
        </el-form-item>
        <el-form-item label="派单备注">
          <el-input v-model="assignForm.remark" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="priorityVisible" title="调整优先级" width="460px">
      <el-form label-width="90px">
        <el-form-item label="工单">
          <span class="mono">{{ currentRow?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="优先级" required>
          <PrioritySelect v-model="priorityForm.priority" />
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-input v-model="priorityForm.reason" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priorityVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPriority">保存</el-button>
      </template>
    </el-dialog>

    <ManagerOrderDrawer v-model="detailVisible" :order-id="detailOrderId" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import OrderStatusTag from '@/components/business/OrderStatusTag.vue'
import PrioritySelect from '@/components/business/PrioritySelect.vue'
import RepairerPicker from '@/components/business/RepairerPicker.vue'
import ManagerOrderDrawer from '@/components/business/ManagerOrderDrawer.vue'
import { STATUS_LIST, PRIORITY_MAP } from '@/composables/useOrderStatus'
import { usePagination } from '@/composables/usePagination'
import { useDictStore } from '@/stores/dict'
import { assignOrder, changePriority, pageManagerOrders, urgeOrder } from '@/api/manager'
import { formatDateTime } from '@/utils/format'

const dictStore = useDictStore()
const { pagination } = usePagination()
const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const filters = reactive({ buildingId: null, status: 'CREATED', keyword: '' })

const assignVisible = ref(false)
const priorityVisible = ref(false)
const detailVisible = ref(false)
const detailOrderId = ref(null)
const currentRow = ref(null)
const assignForm = reactive({ assigneeId: null, remark: '' })
const priorityForm = reactive({ priority: 3, reason: '' })

async function loadList() {
  loading.value = true
  try {
    const data = await pageManagerOrders({
      buildingId: filters.buildingId || undefined,
      status: filters.status || undefined,
      keyword: filters.keyword || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    list.value = data?.records || []
    pagination.total = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadList()
}

function handleReset() {
  filters.buildingId = null
  filters.status = 'CREATED'
  filters.keyword = ''
  handleSearch()
}

function openDetail(row) {
  detailOrderId.value = row.id
  detailVisible.value = true
}

function openAssign(row) {
  currentRow.value = row
  assignForm.assigneeId = null
  assignForm.remark = ''
  assignVisible.value = true
}

function openPriority(row) {
  currentRow.value = row
  priorityForm.priority = row.priority || 3
  priorityForm.reason = ''
  priorityVisible.value = true
}

async function submitAssign() {
  if (!assignForm.assigneeId) {
    ElMessage.warning('请选择维修工')
    return
  }
  submitting.value = true
  try {
    await assignOrder(currentRow.value.id, {
      assigneeId: assignForm.assigneeId,
      remark: assignForm.remark || undefined
    })
    ElMessage.success('派单成功，已通知维修工')
    assignVisible.value = false
    loadList()
  } catch (error) {
    if (error?.code === 40009 || error?.code === 40010) {
      ElMessage.warning('工单或维修工状态已变化，已刷新列表')
      loadList()
    }
  } finally {
    submitting.value = false
  }
}

async function submitPriority() {
  if (!priorityForm.reason.trim()) {
    ElMessage.warning('请填写调整原因')
    return
  }
  submitting.value = true
  try {
    await changePriority(currentRow.value.id, {
      priority: priorityForm.priority,
      reason: priorityForm.reason.trim()
    })
    ElMessage.success('优先级已调整')
    priorityVisible.value = false
    loadList()
  } catch (error) {
    if (error?.code === 40009) loadList()
  } finally {
    submitting.value = false
  }
}

async function handleUrge(row) {
  try {
    await ElMessageBox.confirm(`确认向维修工 ${row.assigneeName || ''} 发送催单通知？`, '催单', {
      confirmButtonText: '发送催单',
      cancelButtonText: '取消',
      type: 'info'
    })
    await urgeOrder(row.id)
    ElMessage.success('催单通知已发送')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    if (error?.code === 40009) loadList()
  }
}

onMounted(() => {
  dictStore.loadBuildings()
  loadList()
})
</script>
