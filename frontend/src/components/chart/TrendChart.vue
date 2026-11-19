<template>
  <div ref="chartRef" class="chart" :style="{ height }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import echarts from '@/utils/echarts'

const props = defineProps({
  xData: { type: Array, default: () => [] },
  series: { type: Array, default: () => [] },
  type: { type: String, default: 'line' },
  unit: { type: String, default: '' },
  height: { type: String, default: '300px' }
})

const chartRef = ref(null)
let chart = null
let observer = null

function render() {
  if (!chart) return
  chart.setOption(
    {
      color: ['#1ba3e0', '#52c41a', '#fa8c16'],
      tooltip: { trigger: 'axis' },
      legend: { right: 10, top: 0 },
      grid: { left: 48, right: 24, top: 40, bottom: 36 },
      xAxis: {
        type: 'category',
        data: props.xData,
        boundaryGap: props.type === 'bar',
        axisLine: { lineStyle: { color: '#d9e1ea' } },
        axisLabel: { color: '#8b94a3' }
      },
      yAxis: {
        type: 'value',
        name: props.unit,
        axisLabel: { color: '#8b94a3' },
        splitLine: { lineStyle: { color: '#eef2f7' } }
      },
      series: props.series.map((item) => ({
        name: item.name,
        type: props.type,
        smooth: props.type === 'line',
        symbolSize: 6,
        barMaxWidth: 26,
        areaStyle: props.type === 'line' ? { opacity: 0.08 } : undefined,
        data: item.data
      }))
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

watch(() => [props.xData, props.series], render, { deep: true })

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
