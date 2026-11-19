<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    filterable
    :loading="loading"
    style="width: 100%"
    @update:model-value="(value) => emit('update:modelValue', value)"
  >
    <el-option
      v-for="item in repairers"
      :key="item.repairerId"
      :label="`${item.realName}（在手 ${item.handCount} 单，今日完工 ${item.todayFinishCount}）`"
      :value="item.repairerId"
    >
      <div class="picker-option">
        <span>{{ item.realName }}</span>
        <span class="muted">
          {{ item.buildingName || '未分配楼栋' }} · 在手 {{ item.handCount }} · 今日 {{ item.todayFinishCount }}
        </span>
      </div>
    </el-option>
  </el-select>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { listRepairers } from '@/api/basedata'

const props = defineProps({
  modelValue: { type: [Number, String], default: null },
  buildingId: { type: [Number, String], default: null },
  placeholder: { type: String, default: '选择维修工' }
})

const emit = defineEmits(['update:modelValue'])
const repairers = ref([])
const loading = ref(false)

async function load() {
  if (!props.buildingId && props.buildingId !== 0) return
  loading.value = true
  try {
    repairers.value = (await listRepairers(props.buildingId)) || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => props.buildingId, load)
</script>

<style scoped>
.picker-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
</style>
