<template>
  <section class="card">
    <header class="card-header">
      <div class="card-title">
        <slot name="title">{{ title }}</slot>
      </div>
      <div class="card-extra">
        <slot name="extra"></slot>
      </div>
    </header>
    <div class="card-body" :style="bodyStyle">
      <slot></slot>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  bodyPadding: { type: String, default: '16px' }
})

const bodyStyle = computed(() => ({ padding: props.bodyPadding }))
</script>

<style scoped>
.card-extra {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-sub);
}
</style>
