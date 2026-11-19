<template>
  <el-timeline v-if="tracks.length" class="timeline">
    <el-timeline-item
      v-for="(track, index) in tracks"
      :key="track.id || index"
      :timestamp="formatDateTime(track.ts)"
      placement="top"
      :color="index === tracks.length - 1 ? '#1ba3e0' : '#d5eefa'"
    >
      <div class="track-title">
        <span v-if="track.roundNo" class="track-round">第 {{ track.roundNo }} 轮</span>
        {{ trackActionLabel(track.action) }}
        <span v-if="track.toStatus" class="track-status">{{ statusLabel(track.toStatus) }}</span>
      </div>
      <div class="track-meta">
        <span v-if="track.operatorName">{{ track.operatorName }}</span>
        <span v-if="track.operatorRole" class="muted">（{{ roleLabel(track.operatorRole) }}）</span>
      </div>
      <div v-if="track.remark" class="track-remark">{{ track.remark }}</div>
    </el-timeline-item>
  </el-timeline>
  <EmptyState v-else description="暂无流转记录" />
</template>

<script setup>
import EmptyState from '@/components/base/EmptyState.vue'
import { statusLabel, trackActionLabel } from '@/composables/useOrderStatus'
import { formatDateTime } from '@/utils/format'

defineProps({
  tracks: { type: Array, default: () => [] }
})

const ROLE_LABEL = { STUDENT: '学生', REPAIRER: '维修工', MANAGER: '管理员', SYSTEM: '系统' }
const roleLabel = (role) => ROLE_LABEL[role] || '系统'
</script>

<style scoped>
.timeline {
  padding: 4px 8px 0 2px;
}

.track-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.track-status {
  margin-left: 8px;
  padding: 1px 8px;
  font-size: 12px;
  font-weight: 400;
  color: var(--brand-dark);
  background: var(--brand-light);
  border-radius: 8px;
}

.track-round {
  margin-right: 8px;
  padding: 1px 8px;
  font-size: 12px;
  font-weight: 400;
  color: #fa541c;
  background: #fff2e8;
  border-radius: 8px;
}

.track-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-regular);
}

.track-remark {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.7;
}
</style>
