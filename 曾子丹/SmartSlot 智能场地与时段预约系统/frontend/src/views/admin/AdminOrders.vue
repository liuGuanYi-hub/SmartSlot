<template>
  <div class="admin-orders-page">
    <div class="page-header">
      <div>
        <h2 class="title">预约订单检索与核销中心</h2>
        <p class="subtitle">实时监控会员预约订单状态、处理退订及快速核验 6 位入场核销码</p>
      </div>
      <div class="header-btns">
        <el-button
          v-permission="['ROLE_ADMIN', 'ROLE_MANAGER']"
          type="warning"
          plain
          :loading="reconciling"
          @click="handleManualReconcile"
        >
          <el-icon><RefreshRight /></el-icon> 异常对账自愈
        </el-button>
        <el-button
          v-permission="['ROLE_ADMIN', 'ROLE_MANAGER']"
          type="info"
          plain
          @click="openReconcileDrawer"
        >
          <el-icon><Money /></el-icon> 支付网关财务对账
        </el-button>
        <el-button
          v-permission="['ROLE_ADMIN', 'ROLE_MANAGER']"
          type="primary"
          plain
          :loading="exporting"
          @click="handleExportOrders"
        >
          <el-icon><Download /></el-icon> 导出对账单 (.xlsx)
        </el-button>
        <el-button type="success" @click="openQuickVerify">
          <el-icon><Ticket /></el-icon> 快捷核销入场
        </el-button>
      </div>
    </div>

    <!-- 筛选搜索栏 -->
    <el-card shadow="never" class="filter-card card-shadow">
      <el-form :inline="true" class="demo-form-inline">
        <el-form-item label="订单状态">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 150px;">
            <el-option label="待支付锁定" :value="0" />
            <el-option label="待核销入场" :value="1" />
            <el-option label="已核销完成" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单编号">
          <el-input v-model="filterOrderNo" placeholder="业务单号" clearable />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="filterPhone" placeholder="预留手机号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchOrders">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 分页数据表格 (MyBatis-Plus 分页插件) -->
    <el-card shadow="never" class="table-card card-shadow">
      <el-table :data="orderList" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="160" />
        <el-table-column prop="venueName" label="预约场地" min-width="150" />
        <el-table-column label="预约时段" min-width="160">
          <template #default="{ row }">
            <div>{{ row.bookDate }}</div>
            <div class="slot-text">{{ row.timeSlot }}</div>
          </template>
        </el-table-column>
        <el-table-column label="联系人" min-width="140">
          <template #default="{ row }">
            <div>{{ row.contactName }}</div>
            <div class="phone-text">{{ row.contactPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="支付金额" width="100">
          <template #default="{ row }">
            <span class="price-val">￥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)">
              {{ getStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="verifyCode" label="核销码" width="110">
          <template #default="{ row }">
            <span v-if="row.verifyCode" class="code-badge">{{ row.verifyCode }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.orderStatus === 1"
              type="success"
              size="small"
              link
              @click="handleDirectVerify(row.verifyCode)"
            >
              直接核销
            </el-button>
            <span v-else-if="row.orderStatus === 2" class="text-verified">已核销通过</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchOrders"
          @current-change="fetchOrders"
        />
      </div>
    </el-card>

    <!-- 快捷核销弹窗组件 -->
    <VerifyModal ref="verifyModalRef" @verified="fetchOrders" />

    <!-- 支付网关财务对账中控抽屉 -->
    <el-drawer
      v-model="reconcileDrawerVisible"
      title="多渠道支付网关流水对账中心"
      size="850px"
      append-to-body
      class="reconcile-drawer"
    >
      <div v-loading="reconcileLoading" class="reconcile-wrapper">
        <!-- 统计指标网格 -->
        <div class="reconcile-kpi-grid">
          <div class="kpi-card total">
            <div class="kpi-title">总入账营收</div>
            <div class="kpi-num">￥{{ Number(reconcileSummary?.totalIncome || 0).toFixed(2) }}</div>
            <div class="kpi-sub">共计 {{ reconcileSummary?.totalTransactions || 0 }} 笔交易</div>
          </div>
          <div class="kpi-card alipay">
            <div class="kpi-title">支付宝实收</div>
            <div class="kpi-num">￥{{ Number(reconcileSummary?.alipayIncome || 0).toFixed(2) }}</div>
            <div class="kpi-sub">RSA2 验签入账</div>
          </div>
          <div class="kpi-card wechat">
            <div class="kpi-title">微信支付实收</div>
            <div class="kpi-num">￥{{ Number(reconcileSummary?.wechatIncome || 0).toFixed(2) }}</div>
            <div class="kpi-sub">HMAC-SHA256 入账</div>
          </div>
          <div class="kpi-card match">
            <div class="kpi-title">网关平账率</div>
            <div class="kpi-num">{{ reconcileSummary?.matchRate || 100.0 }}%</div>
            <div class="kpi-sub">异常笔数: {{ reconcileSummary?.exceptionCount || 0 }}</div>
          </div>
        </div>

        <div class="reconcile-action-bar">
          <span class="table-tip">第三方网关实收流水与系统订单逐笔撮合比对 (按交易时间倒序):</span>
          <el-button size="small" type="primary" plain @click="fetchReconcileData">
            <el-icon><Refresh /></el-icon> 刷新实时对账单
          </el-button>
        </div>

        <!-- 详细撮合对账列表 -->
        <el-table :data="reconcileSummary?.records || []" stripe border style="width: 100%; margin-top: 10px;">
          <el-table-column prop="tradeNo" label="支付流水号" min-width="160" />
          <el-table-column prop="orderNo" label="系统订单号" min-width="160" />
          <el-table-column prop="channel" label="渠道" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.channel === 'ALIPAY' ? 'primary' : row.channel === 'WECHAT' ? 'success' : 'warning'">
                {{ row.channel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="gatewayAmount" label="网关实收" width="95">
            <template #default="{ row }">
              <span style="font-weight: 700; color: #10b981;">￥{{ Number(row.gatewayAmount).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="orderAmount" label="订单应收" width="95">
            <template #default="{ row }">
              <span style="font-weight: 600;">￥{{ Number(row.orderAmount).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reconcileStatus" label="对账结论" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="row.reconcileStatus === 'MATCH' ? 'success' : 'danger'">
                {{ row.reconcileStatus === 'MATCH' ? '✓ 平账一致' : '✗ 账单异常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="gatewayTradeNo" label="网关流水号" min-width="170" show-overflow-tooltip />
          <el-table-column prop="createTime" label="交易时间" min-width="155" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Ticket, Download, RefreshRight, Money, Refresh } from '@element-plus/icons-vue'
import { getAdminOrdersPage } from '@/api/booking'
import { downloadOrdersExcel, reconcileExpiredOrders } from '@/api/admin'
import { getPaymentReconciliation } from '@/api/pay'
import { ElMessage } from 'element-plus'
import VerifyModal from '@/components/VerifyModal.vue'

const orderList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterStatus = ref(null)
const filterOrderNo = ref('')
const filterPhone = ref('')
const loading = ref(false)
const exporting = ref(false)
const reconciling = ref(false)

const verifyModalRef = ref(null)

const reconcileDrawerVisible = ref(false)
const reconcileLoading = ref(false)
const reconcileSummary = ref(null)

async function openReconcileDrawer() {
  reconcileDrawerVisible.value = true
  await fetchReconcileData()
}

async function fetchReconcileData() {
  reconcileLoading.value = true
  try {
    const res = await getPaymentReconciliation()
    reconcileSummary.value = res.data
  } catch (err) {
    ElMessage.error(err.message || '获取对账单失败')
  } finally {
    reconcileLoading.value = false
  }
}

async function handleManualReconcile() {
  reconciling.value = true
  try {
    const res = await reconcileExpiredOrders()
    ElMessage.success(`对账巡检完成！成功自愈超时时段数: ${res}`)
    fetchOrders()
  } catch (e) {
    ElMessage.error(e.message || '对账自愈失败')
  } finally {
    reconciling.value = false
  }
}

async function handleExportOrders() {
  exporting.value = true
  try {
    await downloadOrdersExcel({
      status: filterStatus.value,
      orderNo: filterOrderNo.value,
      phone: filterPhone.value
    })
    ElMessage.success('对账单导出就绪，已通过浏览器流式下载！')
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getAdminOrdersPage({
      current: currentPage.value,
      size: pageSize.value,
      status: filterStatus.value,
      orderNo: filterOrderNo.value,
      phone: filterPhone.value
    })
    orderList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function searchOrders() {
  currentPage.value = 1
  fetchOrders()
}

function resetSearch() {
  filterStatus.value = null
  filterOrderNo.value = ''
  filterPhone.value = ''
  searchOrders()
}

function getStatusText(status) {
  switch (status) {
    case 0: return '待支付锁定'
    case 1: return '待核销入场'
    case 2: return '已核销完成'
    case 3: return '已取消'
    default: return '未知'
  }
}

function getStatusType(status) {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'primary'
    case 2: return 'success'
    case 3: return 'info'
    default: return 'info'
  }
}

function openQuickVerify() {
  verifyModalRef.value?.open()
}

function handleDirectVerify(code) {
  verifyModalRef.value?.open(code)
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-btns {
  display: flex;
  align-items: center;
  gap: 12px;
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

.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
  padding: 8px 16px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
}

.table-card {
  border-radius: 12px;
  padding: 8px 16px 20px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
}

.slot-text {
  font-size: 12px;
  color: #4f46e5;
  font-weight: 600;
}

.phone-text {
  font-size: 12px;
  color: var(--text-muted);
}

.price-val {
  font-weight: 700;
  color: #e11d48;
}

.code-badge {
  font-family: monospace;
  font-weight: 700;
  background: var(--card-bg-elevated);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--text-main);
}

.text-verified {
  color: #10b981;
  font-size: 12px;
}

.text-muted {
  color: #94a3b8;
}

.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}

.reconcile-wrapper {
  padding: 10px 4px;
}

.reconcile-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.kpi-card {
  padding: 14px 16px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.kpi-card.total {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08) 0%, rgba(99, 102, 241, 0.04) 100%);
  border-color: #c7d2fe;
}
.kpi-card.alipay {
  background: linear-gradient(135deg, rgba(22, 119, 255, 0.08) 0%, rgba(59, 130, 246, 0.04) 100%);
  border-color: #bfdbfe;
}
.kpi-card.wechat {
  background: linear-gradient(135deg, rgba(7, 193, 96, 0.08) 0%, rgba(16, 185, 129, 0.04) 100%);
  border-color: #bbf7d0;
}
.kpi-card.match {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08) 0%, rgba(217, 119, 6, 0.04) 100%);
  border-color: #fde68a;
}

.kpi-title {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}

.kpi-num {
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: -0.5px;
}

.kpi-sub {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.reconcile-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-tip {
  font-size: 12px;
  color: #64748b;
}
</style>
