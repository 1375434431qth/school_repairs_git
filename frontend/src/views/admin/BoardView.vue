<template>
  <div class="page-container page-wide">
    <PageHeader title="工单看板" subtitle="按状态查看全部工单，点击卡片查看详情">
      <template #actions>
        <el-select v-model="buildingId" placeholder="全部楼栋" clearable style="width: 170px" @change="load">
          <el-option v-for="item in dictStore.buildings" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :loading="loading" @click="load">
          <AppIcon name="refresh" :size="14" /> 刷新
        </el-button>
      </template>
    </PageHeader>

    <div v-loading="loading" class="board">
      <div v-for="column in columns" :key="column.status" class="board-column">
        <div class="column-head" :style="{ borderTopColor: STATUS_MAP[column.status]?.color }">
          <span class="column-title">
            <i class="dot" :style="{ background: STATUS_MAP[column.status]?.color }"></i>
            {{ column.statusLabel || STATUS_MAP[column.status]?.label }}
          </span>
          <span class="column-count">{{ column.count }}</span>
        </div>
        <div class="column-body">
          <div
            v-for="order in column.orders || []"
            :key="order.id"
            class="board-card"
            @click="openDetail(order)"
          >
            <div class="board-card-head">
              <span class="mono order-no">{{ order.orderNo }}</span>
              <span :style="{ color: PRIORITY_MAP[order.priority]?.color, fontSize: '12px' }">
                {{ PRIORITY_MAP[order.priority]?.label || '-' }}
              </span>
            </div>
            <div class="board-location">{{ order.buildingName }} {{ order.roomNo }}</div>
            <div class="board-fault">{{ order.faultTypeName }}</div>
            <div class="board-meta">
              <span>{{ order.assigneeName || '未派单' }}</span>
              <span>{{ formatDateTime(order.createTime).slice(5) }}</span>
            </div>
            <el-tag v-if="order.timeoutFlag === 1" type="danger" size="small" class="timeout-tag">超时</el-tag>
          </div>
          <div v-if="!column.orders?.length" class="column-empty">暂无工单</div>
        </div>
      </div>
    </div>

    <ManagerOrderDrawer v-model="detailVisible" :order-id="detailOrderId" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import AppIcon from '@/components/base/AppIcon.vue'
import ManagerOrderDrawer from '@/components/business/ManagerOrderDrawer.vue'
import { STATUS_MAP, PRIORITY_MAP } from '@/composables/useOrderStatus'
import { useDictStore } from '@/stores/dict'
import { getBoard } from '@/api/manager'
import { formatDateTime } from '@/utils/format'

const dictStore = useDictStore()
const loading = ref(false)
const columns = ref([])
const buildingId = ref(null)
const detailVisible = ref(false)
const detailOrderId = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await getBoard({ buildingId: buildingId.value || undefined })
    columns.value = data?.columns || []
  } finally {
    loading.value = false
  }
}

function openDetail(order) {
  if (!order?.id) return
  detailOrderId.value = order.id
  detailVisible.value = true
}

onMounted(() => {
  dictStore.loadBuildings()
  load()
})
</script>

<style scoped>
.page-wide {
  width: 1400px;
  max-width: calc(100% - 32px);
}

.board {
  display: grid;
  grid-template-columns: repeat(9, minmax(160px, 1fr));
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.board-column {
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  min-height: 420px;
}

.column-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-top: 3px solid var(--brand);
  border-bottom: 1px solid var(--border);
}

.column-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.column-count {
  min-width: 22px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: var(--brand-light);
  color: var(--brand-dark);
  font-size: 12px;
  line-height: 20px;
  text-align: center;
}

.column-body {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.board-card {
  position: relative;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: #fbfdff;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.board-card:hover {
  border-color: var(--brand);
  box-shadow: 0 2px 8px rgba(27, 163, 224, 0.16);
}

.board-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.order-no {
  font-size: 12px;
  color: var(--text-sub);
}

.board-location {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.board-fault {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-regular);
}

.board-meta {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-sub);
}

.timeout-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  transform: translateY(-120%);
}

.column-empty {
  padding: 20px 0;
  text-align: center;
  font-size: 12px;
  color: var(--text-sub);
}
</style>
