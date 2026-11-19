<template>
  <span class="status-tag" :style="{ color: meta.color, background: meta.bg }">{{ meta.label }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { STATUS_MAP } from '@/composables/useOrderStatus'

const props = defineProps({
  status: { type: String, default: '' },
  label: { type: String, default: '' }
})

const meta = computed(() => {
  const item = STATUS_MAP[props.status] || {}
  return {
    color: item.color || '#909399',
    bg: item.bg || '#f4f4f5',
    label: props.label || item.label || props.status || '-'
  }
})
</script>

<style scoped>
.status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}
</style>
