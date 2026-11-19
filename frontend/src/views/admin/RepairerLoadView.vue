<template>
  <div class="page-container">
    <PageHeader title="维修工负载" subtitle="在手单数 = 待接单 + 待上门 + 维修中，便于均衡派单">
      <template #actions>
        <el-select v-model="buildingId" placeholder="全部楼栋" clearable style="width: 170px" @change="load">
          <el-option v-for="item in dictStore.buildings" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :loading="loading" @click="load">
          <AppIcon name="refresh" :size="14" /> 刷新
        </el-button>
      </template>
    </PageHeader>

    <div class="summary-grid">
      <StatCard label="维修工总数" :value="list.length" icon="users" tone="#1ba3e0" />
      <StatCard label="在手工单" :value="totalHand" icon="clipboard" tone="#fa8c16" />
      <StatCard label="今日完工" :value="totalToday" icon="wrench" tone="#52c41a" />
    </div>

    <Panel>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column prop="buildingName" label="负责楼栋" width="150">
          <template #default="{ row }">{{ row.buildingName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="skillTags" label="技能标签" min-width="200">
          <template #default="{ row }">
            <template v-if="row.skillTags">
              <el-tag v-for="tag in row.skillTags.split(',')" :key="tag" size="small" class="skill-tag">
                {{ tag }}
              </el-tag>
            </template>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="在手单数" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.handCount >= 5 ? 'danger' : row.handCount >= 3 ? 'warning' : 'success'" size="small">
              {{ row.handCount ?? 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="今日完工" width="120" align="center">
          <template #default="{ row }">{{ row.todayFinishCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="负载状态" width="140">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.min((row.handCount || 0) * 20, 100)"
              :status="row.handCount >= 5 ? 'exception' : row.handCount >= 3 ? 'warning' : undefined"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && !list.length" description="暂无维修工数据" />
    </Panel>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import StatCard from '@/components/base/StatCard.vue'
import AppIcon from '@/components/base/AppIcon.vue'
import { useDictStore } from '@/stores/dict'
import { getRepairerLoad } from '@/api/manager'

const dictStore = useDictStore()
const loading = ref(false)
const buildingId = ref(null)
const list = ref([])

const totalHand = computed(() => list.value.reduce((sum, item) => sum + (item.handCount || 0), 0))
const totalToday = computed(() => list.value.reduce((sum, item) => sum + (item.todayFinishCount || 0), 0))

async function load() {
  loading.value = true
  try {
    list.value = (await getRepairerLoad({ buildingId: buildingId.value || undefined })) || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  dictStore.loadBuildings()
  load()
})
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.skill-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}
</style>
