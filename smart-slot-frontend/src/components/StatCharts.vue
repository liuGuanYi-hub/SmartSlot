<template>
  <div class="stat-charts-container">
    <el-row :gutter="20">
      <!-- 近 7 天营收与客流趋势 -->
      <el-col :xs="24" :lg="15">
        <div class="chart-card card-shadow">
          <div class="chart-title">近 7 天营收与预约订单趋势</div>
          <div ref="trendChartRef" class="chart-box"></div>
        </div>
      </el-col>

      <!-- 各场地预约偏好热度 -->
      <el-col :xs="24" :lg="9">
        <div class="chart-card card-shadow">
          <div class="chart-title">场馆与场地预约热度分布</div>
          <div ref="pieChartRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 全周 7x13 时段坪效热力图 -->
    <div class="heatmap-section">
      <div class="chart-card card-shadow">
        <div class="heatmap-header">
          <div>
            <div class="chart-title">全周 7x13 小时时段坪效热力图 (Slot Heatmap)</div>
            <div class="chart-subtitle">纵览 09:00 - 22:00 全时段预订频次与场馆负荷密度，辅助峰谷差异化定价与排期调度</div>
          </div>
          <div class="heatmap-tags">
            <span class="heat-tag peak">🔥 晚间与周末黄金档</span>
            <span class="heat-tag off-peak">🌿 工作日上午闲时</span>
          </div>
        </div>
        <div ref="heatmapChartRef" class="heatmap-chart-box"></div>
      </div>
    </div>
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
const heatmapChartRef = ref(null)

let trendChart = null
let pieChart = null
let heatmapChart = null

function initCharts() {
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
  }
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
  }
  if (heatmapChartRef.value) {
    heatmapChart = echarts.init(heatmapChartRef.value)
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

  // 3. 全周 7x13 时段坪效热力图
  if (heatmapChart && props.stats.heatmapDays && props.stats.heatmapHours) {
    const days = props.stats.heatmapDays
    const hours = props.stats.heatmapHours
    const data = props.stats.heatmapData || []

    heatmapChart.setOption({
      tooltip: {
        position: 'top',
        formatter: function (params) {
          const hour = hours[params.value[0]]
          const day = days[params.value[1]]
          const val = params.value[2]
          const tag = val >= 5 ? '🔥 高峰黄金档' : val <= 1 ? '🌿 闲时低谷档' : '⚡ 活跃档'
          return `<div style="padding: 4px 6px;">
                    <div style="font-weight:700;margin-bottom:4px;color:#0f172a;">${day} ${hour}</div>
                    <div style="color:#334155;">预约占用: <strong>${val}</strong> 场次</div>
                    <div style="font-size:12px;margin-top:4px;">${tag}</div>
                  </div>`
        }
      },
      grid: {
        height: '62%',
        top: '8%',
        left: '6%',
        right: '4%',
        bottom: '18%'
      },
      xAxis: {
        type: 'category',
        data: hours,
        splitArea: { show: true },
        axisLabel: {
          interval: 0,
          rotate: 0,
          color: '#64748b',
          fontSize: 11
        }
      },
      yAxis: {
        type: 'category',
        data: days,
        splitArea: { show: true },
        axisLabel: {
          color: '#64748b',
          fontSize: 11
        }
      },
      visualMap: {
        min: 0,
        max: 8,
        calculable: true,
        orient: 'horizontal',
        left: 'center',
        bottom: '1%',
        inRange: {
          color: ['#f1f5f9', '#93c5fd', '#6366f1', '#4338ca', '#ef4444']
        },
        textStyle: {
          color: '#64748b'
        }
      },
      series: [
        {
          name: '预约频次',
          type: 'heatmap',
          data: data,
          label: {
            show: true,
            fontSize: 11,
            color: '#1e293b'
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.3)'
            }
          }
        }
      ]
    })
  }
}

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
  heatmapChart?.resize()
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
  heatmapChart?.dispose()
})
</script>

<style scoped>
.stat-charts-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chart-card {
  padding: 18px 20px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
}

.chart-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}

.chart-subtitle {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.chart-box {
  width: 100%;
  height: 320px;
}

.heatmap-section {
  margin-top: 4px;
}

.heatmap-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 10px;
}

.heatmap-tags {
  display: flex;
  gap: 8px;
}

.heat-tag {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 600;
}

.heat-tag.peak {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.heat-tag.off-peak {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.heatmap-chart-box {
  width: 100%;
  height: 360px;
}
</style>
