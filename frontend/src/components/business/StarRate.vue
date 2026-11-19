<template>
  <el-rate
    :model-value="modelValue"
    :texts="texts"
    show-text
    size="large"
    @update:model-value="(value) => emit('update:modelValue', value)"
  />
</template>

<script setup>
defineProps({
  modelValue: { type: Number, default: 0 }
})

const emit = defineEmits(['update:modelValue'])

const texts = ['非常不满意', '不满意', '一般', '满意', '非常满意']
</script>
