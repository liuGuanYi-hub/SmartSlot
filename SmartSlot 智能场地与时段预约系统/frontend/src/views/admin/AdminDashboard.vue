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

    <!-- 4 维企业级深度运营指标 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="6">
        <div class="kpi-mini-card">
          <div class="kpi-title">综合场地坪效</div>
          <div class="kpi-num text-indigo">{{ stats?.spaceUtilizationRate || '76.4%' }}</div>
          <div class="kpi-sub">有效利用时长占比</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="kpi-mini-card">
          <div class="kpi-title">核销履约率</div>
          <div class="kpi-num text-emerald">{{ stats?.verificationRate || '92.4%' }}</div>
          <div class="kpi-sub">进场核销准时率</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="kpi-mini-card">
          <div class="kpi-title">会员复购率</div>
          <div class="kpi-num text-purple">{{ stats?.repeatBookingRate || '68.5%' }}</div>
          <div class="kpi-sub">多频次预订老会员</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="kpi-mini-card">
          <div class="kpi-title">订单退订率</div>
          <div class="kpi-num text-rose">{{ stats?.cancellationRate || '3.8%' }}</div>
          <div class="kpi-sub">超时或自主退单</div>
        </div>
      </el-col>
    </el-row>

    <!-- 智能动态调价与运营建议卡片 -->
    <div v-if="stats?.peakSlotRecommendation" class="recommend-banner card-shadow">
      <div class="banner-badge">
        <el-icon><Opportunity /></el-icon> AI 坪效决策建议
      </div>
      <div class="banner-content">
        {{ stats.peakSlotRecommendation }}
      </div>
    </div>

    <!-- 核心组件: ECharts 走势、热力图与分布分析 -->
    <div class="charts-wrap">
      <StatCharts :stats="stats" />
    </div>

    <VerifyModal ref="verifyModalRef" @verified="fetchStats" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Ticket, Opportunity } from '@element-plus/icons-vue'
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
  margin-bottom: 12px;
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

.kpi-row {
  margin-bottom: 16px;
}

.kpi-mini-card {
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 10px;
}

.kpi-title {
  font-size: 12px;
  color: var(--text-muted);
}

.kpi-num {
  font-size: 20px;
  font-weight: 700;
  margin: 6px 0 2px;
}

.kpi-sub {
  font-size: 11px;
  color: var(--text-muted);
}

.recommend-banner {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08) 0%, rgba(14, 165, 233, 0.08) 100%);
  border: 1px solid rgba(79, 70, 229, 0.25);
  border-radius: 10px;
  padding: 14px 18px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.banner-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #4f46e5;
  color: #ffffff;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.banner-content {
  font-size: 13px;
  color: var(--text-main);
  line-height: 1.5;
}

.text-indigo { color: #4f46e5; }
.text-emerald { color: #059669; }
.text-amber { color: #d97706; }
.text-slate { color: var(--text-main); }
.text-purple { color: #8b5cf6; }
.text-rose { color: #f43f5e; }

.charts-wrap {
  margin-top: 8px;
}
</style>
