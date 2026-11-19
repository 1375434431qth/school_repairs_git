<template>
  <div class="page-container">
    <PageHeader title="我的工单" subtitle="派给我的全部工单，可按状态筛选" />

    <div class="stat-grid">
      <StatCard label="本月接单" :value="stat.acceptCount ?? 0" icon="clipboard" tone="#1ba3e0" />
      <StatCard label="本月完工" :value="stat.finishCount ?? 0" icon="wrench" tone="#52c41a" />
      <StatCard label="平均维修时长" :value="formatDuration(stat.avgMinutes)" icon="clock" tone="#fa8c16" />
      <StatCard label="平均评分" :value="stat.avgScore ? stat.avgScore : '-'" icon="star" tone="#9254de" />
    </div>

    <Panel>
      <div class="toolbar">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px" @change="handleSearch">
          <el-option v-for="item in REPAIRER_STATUS_LIST" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ pagination.total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="orderNo" label="工单号" width="180">
          <template #default="{ row }"><span class="mono">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="报修位置" min-width="170">
          <template #default="{ row }">{{ row.buildingName }} {{ row.roomNo }}</template>
        </el-table-column>
        <el-table-column prop="faultTypeName" label="故障类型" min-width="130" />
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
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="期望完成" width="150">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.timeoutFlag === 1 }">{{ formatDateTime(row.deadlineTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/repairer/orders/${row.id}`)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && !list.length" description="暂无工单" />

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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import StatCard from '@/components/base/StatCard.vue'
import OrderStatusTag from '@/components/business/OrderStatusTag.vue'
import { PRIORITY_MAP, REPAIRER_STATUS_LIST } from '@/composables/useOrderStatus'
import { usePagination } from '@/composables/usePagination'
import { getMyStat, pageMyRepairOrders } from '@/api/dispatch'
import { formatDateTime, formatDuration } from '@/utils/format'

const router = useRouter()
const { pagination } = usePagination()
const loading = ref(false)
const status = ref('')
const list = ref([])
const stat = ref({})

async function loadList() {
  loading.value = true
  try {
    const data = await pageMyRepairOrders({
      status: status.value || undefined,
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

onMounted(() => {
  loadList()
  getMyStat().then((data) => {
    stat.value = data || {}
  })
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.text-danger {
  color: #f5222d;
}

@media (max-width: 1080px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
