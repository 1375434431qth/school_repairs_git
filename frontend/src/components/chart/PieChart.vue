<template>
  <div ref="chartRef" class="chart" :style="{ height }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import echarts from '@/utils/echarts'

const props = defineProps({
  data: { type: Array, default: () => [] },
  height: { type: String, default: '300px' }
})

const chartRef = ref(null)
let chart = null
let observer = null

function render() {
  if (!chart) return
  chart.setOption(
    {
      color: ['#1ba3e0', '#52c41a', '#fa8c16', '#9254de', '#909399', '#13c2c2'],
      tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
      legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8 },
      series: [
        {
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}\n{d}%', color: '#5a6472' },
          data: props.data
        }
      ]
    },
    true
  )
}

onMounted(() => {
  chart = echarts.init(chartRef.value)
  render()
  observer = new ResizeObserver(() => chart && chart.resize())
  observer.observe(chartRef.value)
})

watch(() => props.data, render, { deep: true })

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
  if (chart) chart.dispose()
})
</script>

<style scoped>
.chart {
  width: 100%;
}
</style>
