<template>
  <div class="stat-charts-container">
    <el-row :gutter="20">
      <!-- 近 7 天营收与客流趋势 -->
      <el-col :span="15">
        <div class="chart-card card-shadow">
          <div class="chart-title">近 7 天营收与预约订单趋势</div>
          <div ref="trendChartRef" class="chart-box"></div>
        </div>
      </el-col>

      <!-- 各场地预约偏好热度 -->
      <el-col :span="9">
        <div class="chart-card card-shadow">
          <div class="chart-title">场馆与场地预约热度分布</div>
          <div ref="pieChartRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  stats: {
    type: Object,
    default: () => ({})
  }
})

const trendChartRef = ref(null)
const pieChartRef = ref(null)

let trendChart = null
let pieChart = null

function initCharts() {
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
  }
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
  }
  renderCharts()
}

function renderCharts() {
  if (!props.stats) return

  // 1. 营收与客流趋势图
  if (trendChart) {
    const dates = props.stats.trendDates || []
    const revenues = props.stats.trendRevenues || []
    const orders = props.stats.trendOrderCounts || []

    trendChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' }
      },
      legend: {
        data: ['营业额 (元)', '预约单量 (单)'],
        top: 0
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          data: dates,
          axisTick: { alignWithLabel: true }
        }
      ],
      yAxis: [
        {
          type: 'value',
          name: '营收 (元)',
          position: 'left'
        },
        {
          type: 'value',
          name: '单量',
          position: 'right'
        }
      ],
      series: [
        {
          name: '营业额 (元)',
          type: 'bar',
          barWidth: '35%',
          data: revenues,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#6366f1' },
              { offset: 1, color: '#4f46e5' }
            ]),
            borderRadius: [4, 4, 0, 0]
          }
        },
        {
          name: '预约单量 (单)',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          data: orders,
          itemStyle: { color: '#f59e0b' },
          lineStyle: { width: 3 }
        }
      ]
    })
  }

  // 2. 场地热度分布南丁格尔玫瑰图
  if (pieChart) {
    const venueData = props.stats.venuePopularity || []
    pieChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c}次 ({d}%)'
      },
      legend: {
        bottom: '0',
        left: 'center'
      },
      series: [
        {
          name: '预约占比',
          type: 'pie',
          radius: ['25%', '70%'],
          center: ['50%', '45%'],
          roseType: 'area',
          itemStyle: {
            borderRadius: 6
          },
          data: venueData
        }
      ]
    })
  }
}

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

watch(() => props.stats, () => {
  renderCharts()
}, { deep: true })

onMounted(() => {
  initCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.chart-card {
  padding: 18px;
  background: #ffffff;
  border-radius: 12px;
}

.chart-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 14px;
}

.chart-box {
  width: 100%;
  height: 320px;
}
</style>
