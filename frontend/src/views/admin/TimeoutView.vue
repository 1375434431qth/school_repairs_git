<template>
  <div class="page-container">
    <PageHeader title="超时管理" subtitle="查看已被标记超时的工单，可手动补标或催单" />

    <Panel>
      <div class="toolbar">
        <el-select v-model="filters.buildingId" placeholder="全部楼栋" clearable style="width: 170px" @change="handleSearch">
          <el-option v-for="item in dictStore.buildings" :key="item.id" :label="item.name" :value="item.id" />
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
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><OrderStatusTag :status="row.status" :label="row.statusLabel" /></template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="维修工" width="110">
          <template #default="{ row }">{{ row.assigneeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="超时标记" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.timeoutFlag === 1" type="danger" size="small">已超时</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="期望完成" width="150">
          <template #default="{ row }">{{ formatDateTime(row.deadlineTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="warning" @click="openMark(row)">标记超时</el-button>
            <el-button link type="success" @click="handleUrge(row)">催单</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && !list.length" description="暂无超时工单" />

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

    <el-dialog v-model="markVisible" title="手动标记超时" width="440px">
      <el-form label-width="90px">
        <el-form-item label="工单">
          <span class="mono">{{ currentRow?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="超时环节" required>
          <el-select v-model="markForm.timeoutType" style="width: 100%">
            <el-option v-for="(label, value) in TIMEOUT_TYPE_MAP" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="markForm.remark" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="markVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMark">确认标记</el-button>
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
import ManagerOrderDrawer from '@/components/business/ManagerOrderDrawer.vue'
import { TIMEOUT_TYPE_MAP } from '@/composables/useOrderStatus'
import { usePagination } from '@/composables/usePagination'
import { useDictStore } from '@/stores/dict'
import { getTimeoutList, markTimeout, urgeOrder } from '@/api/manager'
import { formatDateTime } from '@/utils/format'

const dictStore = useDictStore()
const { pagination } = usePagination()
const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const filters = reactive({ buildingId: null, keyword: '' })
const markVisible = ref(false)
const detailVisible = ref(false)
const detailOrderId = ref(null)
const currentRow = ref(null)
const markForm = reactive({ timeoutType: 'FINISH', remark: '' })

async function loadList() {
  loading.value = true
  try {
    const data = await getTimeoutList({
      buildingId: filters.buildingId || undefined,
      keyword: filters.keyword || undefined,
      timeoutFlag: 1,
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
  filters.keyword = ''
  handleSearch()
}

function openDetail(row) {
  detailOrderId.value = row.id
  detailVisible.value = true
}

function openMark(row) {
  currentRow.value = row
  markForm.timeoutType = 'FINISH'
  markForm.remark = ''
  markVisible.value = true
}

async function submitMark() {
  submitting.value = true
  try {
    await markTimeout(currentRow.value.id, {
      timeoutType: markForm.timeoutType,
      remark: markForm.remark || undefined
    })
    ElMessage.success('已标记超时')
    markVisible.value = false
    loadList()
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
