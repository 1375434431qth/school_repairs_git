<template>
  <div class="page-container">
    <PageHeader title="数据统计" subtitle="近 30 天默认区间；切换区间会重新请求后端，不在前端计算">
      <template #actions>
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          @change="loadAll"
        />
        <el-button :loading="refreshing" @click="handleRefresh">刷新缓存</el-button>
      </template>
    </PageHeader>

    <div class="overview-grid" v-loading="loadingOverview">
      <StatCard label="工单总数" :value="overview.totalOrders ?? 0" icon="clipboard" tone="#1ba3e0" />
      <StatCard label="待处理" :value="overview.pendingCount ?? 0" icon="clock" tone="#fa8c16" />
      <StatCard label="超时工单" :value="overview.timeoutCount ?? 0" icon="bell" tone="#f5222d" />
      <StatCard label="今日新增" :value="overview.todayNewCount ?? 0" icon="plus" tone="#13c2c2" />
      <StatCard label="平均处理时长" :value="formatDuration(overview.avgDurationMinutes)" icon="chart" tone="#9254de" />
      <StatCard label="返修率" :value="formatPercent(rangeReworkRate)" icon="refresh" tone="#52c41a" />
    </div>

    <div class="chart-row">
      <Panel title="报修类型分布">
        <PieChart :data="typeData" height="300px" />
      </Panel>
      <Panel title="平均处理时长趋势">
        <TrendChart :x-data="trendX" :series="trendSeries" unit="分钟" height="300px" />
      </Panel>
    </div>

    <div class="chart-row">
      <Panel title="返修率">
        <GaugeChart :value="rework.rate || 0" height="280px" />
        <div class="rework-meta">
          <span>返修 {{ rework.reworkCount ?? 0 }} 单</span>
          <span>完工 {{ rework.finishedCount ?? 0 }} 单</span>
        </div>
        <el-table :data="rework.topRooms || []" size="small" border class="mt-12">
          <el-table-column prop="buildingName" label="楼栋" />
          <el-table-column prop="roomNo" label="房间" width="90" />
          <el-table-column prop="reworkCount" label="返修次数" width="100" />
          <template #empty>
            <span class="muted">该区间暂无返修房间</span>
          </template>
        </el-table>
      </Panel>
      <Panel title="维修工绩效排行">
        <el-table :data="rankList" size="small" border height="360">
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="repairerName" label="维修工" width="110" />
          <el-table-column prop="finishCount" label="完工量" width="90" />
          <el-table-column label="平均时长" width="110">
            <template #default="{ row }">{{ formatDuration(row.avgMinutes) }}</template>
          </el-table-column>
          <el-table-column label="平均评分">
            <template #default="{ row }">{{ formatScore(row.avgScore) }}</template>
          </el-table-column>
          <template #empty>
            <span class="muted">该区间暂无维修工绩效数据</span>
          </template>
        </el-table>
      </Panel>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import StatCard from '@/components/base/StatCard.vue'
import PieChart from '@/components/chart/PieChart.vue'
import TrendChart from '@/components/chart/TrendChart.vue'
import GaugeChart from '@/components/chart/GaugeChart.vue'
import {
  getAvgDuration,
  getOverview,
  getRepairerRank,
  getReworkRate,
  getTypeDist,
  refreshStat
} from '@/api/stat'
import { formatDuration, formatPercent, recentRange } from '@/utils/format'

const range = ref(recentRange(30))
const overview = ref({})
const typeData = ref([])
const trendX = ref([])
const trendSeries = ref([])
const rework = ref({})
const rankList = ref([])
const loadingOverview = ref(false)
const refreshing = ref(false)

const params = computed(() => ({
  beginDate: range.value?.[0] || undefined,
  endDate: range.value?.[1] || undefined
}))

/** 返修率卡片与仪表盘/明细使用同一区间口径 */
const rangeReworkRate = computed(() => rework.value.rate ?? overview.value.reworkRate)

function formatScore(value) {
  if (value === null || value === undefined || value === '') return '-'
  const score = Number(value)
  return Number.isNaN(score) ? '-' : score.toFixed(1)
}

async function loadOverview() {
  loadingOverview.value = true
  try {
    overview.value = (await getOverview()) || {}
  } finally {
    loadingOverview.value = false
  }
}

async function loadCharts() {
  const [typeDist, avgDuration, reworkRate, rank] = await Promise.all([
    getTypeDist(params.value),
    getAvgDuration({ ...params.value, groupBy: 'day' }),
    getReworkRate(params.value),
    getRepairerRank(params.value)
  ])
  typeData.value = (typeDist?.items || []).map((item) => ({
    name: item.categoryName || item.category,
    value: item.count
  }))
  const points = avgDuration?.points || []
  trendX.value = points.map((point) => point.date)
  trendSeries.value = [
    { name: '平均时长', data: points.map((point) => point.avgMinutes) },
    { name: '工单量', data: points.map((point) => point.orderCount) }
  ]
  rework.value = reworkRate || {}
  rankList.value = rank || []
}

function loadAll() {
  loadOverview()
  loadCharts()
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await refreshStat()
    ElMessage.success('统计缓存已刷新')
    loadAll()
  } finally {
    refreshing.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.overview-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.chart-row :deep(.card + .card) {
  margin-top: 0;
}

.rework-meta {
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 13px;
  color: var(--text-regular);
}

@media (max-width: 1240px) {
  .overview-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .chart-row {
    grid-template-columns: 1fr;
  }
}
</style>
