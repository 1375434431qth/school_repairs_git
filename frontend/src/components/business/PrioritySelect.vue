<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    style="width: 130px"
    @update:model-value="(value) => emit('update:modelValue', value)"
  >
    <el-option v-for="item in PRIORITY_LIST" :key="item.value" :label="item.label" :value="item.value">
      <span :style="{ color: item.color }">{{ item.label }}</span>
      <span class="muted"> · {{ item.hours }}h</span>
    </el-option>
  </el-select>
</template>

<script setup>
import { PRIORITY_LIST } from '@/composables/useOrderStatus'

defineProps({
  modelValue: { type: [Number, String], default: null },
  placeholder: { type: String, default: '优先级' },
  clearable: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])
</script>
