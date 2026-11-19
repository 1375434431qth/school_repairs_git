<template>
  <div class="stat-card">
    <div class="stat-icon" :style="{ color: tone, background: toneBg }">
      <AppIcon :name="icon" :size="22" />
    </div>
    <div class="stat-info">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value">{{ value }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '-' },
  icon: { type: String, default: 'clipboard' },
  tone: { type: String, default: '#1ba3e0' }
})

const toneBg = computed(() => `${props.tone}1a`)
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.stat-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.stat-label {
  font-size: 13px;
  color: var(--text-sub);
}

.stat-value {
  margin-top: 4px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
}
</style>
