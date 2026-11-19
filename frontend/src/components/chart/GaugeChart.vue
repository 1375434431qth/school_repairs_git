<template>
  <div ref="chartRef" class="chart" :style="{ height }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import echarts from '@/utils/echarts'

const props = defineProps({
  value: { type: Number, default: 0 },
  title: { type: String, default: '返修率' },
  height: { type: String, default: '280px' }
})

const chartRef = ref(null)
let chart = null
let observer = null

function render() {
  if (!chart) return
  chart.setOption(
    {
      series: [
        {
          type: 'gauge',
          startAngle: 210,
          endAngle: -30,
          min: 0,
          max: 100,
          radius: '90%',
          progress: { show: true, width: 14, itemStyle: { color: '#1ba3e0' } },
          axisLine: { lineStyle: { width: 14, color: [[1, '#e9f6fd']] } },
          axisTick: { show: false },
          splitLine: { length: 8, lineStyle: { color: '#c9d4df' } },
          axisLabel: { distance: 18, color: '#8b94a3', fontSize: 11 },
          pointer: { itemStyle: { color: '#1489bf' } },
          title: { offsetCenter: [0, '60%'], color: '#5a6472', fontSize: 13 },
          detail: {
            offsetCenter: [0, '25%'],
            formatter: '{value}%',
            color: '#303133',
            fontSize: 26,
            fontWeight: 'bold'
          },
          data: [{ value: Number((props.value * 100).toFixed(1)), name: props.title }]
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

watch(() => props.value, render)

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
