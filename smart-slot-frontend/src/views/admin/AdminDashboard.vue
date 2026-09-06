<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <div>
        <h2 class="title">运营数据中心</h2>
        <p class="subtitle">场馆营收分析、时段利用率及预约热度可视化监控</p>
      </div>
      <el-button type="success" @click="openVerify">
        <el-icon><Ticket /></el-icon> 快速核销入场
      </el-button>
    </div>

    <!-- 4 大核心指标卡片 -->
    <el-row :gutter="16" class="metric-row">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card card-shadow">
          <div class="metric-label">累计营业收入</div>
          <div class="metric-value text-indigo">￥{{ stats?.totalRevenue || '0.00' }}</div>
          <div class="metric-desc">在线已支付订单累计</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card card-shadow">
          <div class="metric-label">有效预约订单</div>
          <div class="metric-value text-emerald">{{ stats?.totalOrders || 0 }} 单</div>
          <div class="metric-desc">预约成功及已核销</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card card-shadow">
          <div class="metric-label">今日排期预约</div>
          <div class="metric-value text-amber">{{ stats?.todayBookings || 0 }} 场</div>
          <div class="metric-desc">今日实时占用时段</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="metric-card card-shadow">
          <div class="metric-label">注册会员用户</div>
          <div class="metric-value text-slate">{{ stats?.totalUsers || 0 }} 人</div>
          <div class="metric-desc">系统总会员数</div>
        </div>
      </el-col>
    </el-row>

    <!-- 核心组件: ECharts 走势与热度分析图表 -->
    <div class="charts-wrap">
      <StatCharts :stats="stats" />
    </div>

    <VerifyModal ref="verifyModalRef" @verified="fetchStats" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Ticket } from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/dashboard'
import StatCharts from '@/components/StatCharts.vue'
import VerifyModal from '@/components/VerifyModal.vue'

const stats = ref(null)
const verifyModalRef = ref(null)

async function fetchStats() {
  try {
    stats.value = await getDashboardStats()
  } catch (e) {
    console.error(e)
  }
}

function openVerify() {
  verifyModalRef.value?.open()
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
}

.subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.metric-row {
  margin-bottom: 24px;
}

.metric-card {
  padding: 20px;
  border-radius: 12px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  margin-bottom: 12px;
}

.metric-label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.metric-value {
  font-size: 28px;
  font-weight: 800;
  margin: 10px 0 6px;
}

.metric-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.text-indigo { color: #4f46e5; }
.text-emerald { color: #059669; }
.text-amber { color: #d97706; }
.text-slate { color: var(--text-main); }

.charts-wrap {
  margin-top: 8px;
}
</style>
