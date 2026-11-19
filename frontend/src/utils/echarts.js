/**
 * ECharts 按需注册：只打包用到的图表与组件，避免整包进产物。
 */
import * as echarts from 'echarts/core'
import { BarChart, GaugeChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GaugeChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer
])

export default echarts
