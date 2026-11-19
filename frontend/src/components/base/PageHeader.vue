<template>
  <div class="page-head">
    <div>
      <div class="page-title">{{ title }}</div>
      <div v-if="subtitle" class="page-subtitle">{{ subtitle }}</div>
    </div>
    <div class="page-head-actions">
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
