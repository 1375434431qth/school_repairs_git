<template>
  <div class="page-container">
    <PageHeader title="我的工单" subtitle="查看报修进度、确认完工并评价">
      <template #actions>
        <el-button type="primary" @click="router.push('/student/orders/create')">
          <AppIcon name="plus" :size="15" /> 新建报修
        </el-button>
      </template>
    </PageHeader>

    <Panel>
      <div class="toolbar">
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 150px" @change="handleSearch">
          <el-option v-for="item in STATUS_LIST" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          placeholder="工单号 / 故障描述"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ pagination.total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe :empty-text="' '">
        <el-table-column prop="orderNo" label="工单号" width="180">
          <template #default="{ row }">
            <span class="mono">{{ row.orderNo }}</span>
          </template>
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
          <template #default="{ row }">
            <OrderStatusTag :status="row.status" :label="row.statusLabel" />
          </template>
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
            <el-button link type="primary" @click="router.push(`/student/orders/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && !list.length" description="暂无工单，点击右上角新建报修" />

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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import AppIcon from '@/components/base/AppIcon.vue'
import OrderStatusTag from '@/components/business/OrderStatusTag.vue'
import { STATUS_LIST, PRIORITY_MAP } from '@/composables/useOrderStatus'
import { usePagination } from '@/composables/usePagination'
import { pageMyOrders } from '@/api/order'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const { pagination } = usePagination()
const loading = ref(false)
const list = ref([])
const filters = reactive({ status: '', keyword: '' })

async function loadList() {
  loading.value = true
  try {
    const data = await pageMyOrders({
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
  filters.status = ''
  filters.keyword = ''
  handleSearch()
}

onMounted(loadList)
</script>

<style scoped>
.text-danger {
  color: #f5222d;
}
</style>
