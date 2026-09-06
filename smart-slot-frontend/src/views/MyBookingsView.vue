<template>
  <div class="my-bookings-page">
    <div class="page-title-row">
      <div>
        <h2 class="title">我的预约日程</h2>
        <p class="subtitle">查看并管理您的场馆时段预约、出示数字核销票据或提交场地体验评分</p>
      </div>
      <el-button type="primary" class="shimmer-badge" @click="$router.push('/matrix')">
        继续预约新时段
      </el-button>
    </div>

    <!-- 状态切换 Tabs 与表格卡片 -->
    <el-card shadow="never" class="table-card card-shadow">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="custom-tabs">
        <el-tab-pane label="全部行程" name="all" />
        <el-tab-pane label="15分待支付锁定" name="0" />
        <el-tab-pane label="待核销入场" name="1" />
        <el-tab-pane label="已核销完成" name="2" />
        <el-tab-pane label="已取消记录" name="3" />
      </el-tabs>

      <!-- 分页数据表格 (MyBatis-Plus 分页插件联动) -->
      <el-table :data="orderList" v-loading="loading" style="width: 100%" stripe class="custom-table">
        <el-table-column prop="orderNo" label="订单流水号" min-width="170">
          <template #default="{ row }">
            <span class="mono-order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="venueName" label="预约场馆" min-width="160">
          <template #default="{ row }">
            <strong class="venue-cell-title">{{ row.venueName }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="预约日期及时间" min-width="170">
          <template #default="{ row }">
            <div class="date-cell">{{ row.bookDate }}</div>
            <div class="time-slot-pill">{{ row.timeSlot }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="支付金额" width="110">
          <template #default="{ row }">
            <span class="amount-text">￥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="当前状态" width="130">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)" effect="light" class="status-tag">
              {{ getStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入场凭证" width="140">
          <template #default="{ row }">
            <el-button 
              v-if="row.verifyCode && row.orderStatus === 1" 
              type="primary" 
              size="small" 
              plain
              class="ticket-btn shimmer-badge"
              @click="showVerifyCodeModal(row)"
            >
              出示票据 ({{ row.verifyCode }})
            </el-button>
            <span v-else-if="row.orderStatus === 2" class="verified-text">已核销通过</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="预订时间" min-width="160" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <!-- 待支付操作 -->
            <el-button 
              v-if="row.orderStatus === 0" 
              type="primary" 
              size="small" 
              @click="openPayModal(row)"
            >
              立即付款
            </el-button>

            <!-- 取消预约操作 -->
            <el-button 
              v-if="row.orderStatus === 0 || row.orderStatus === 1" 
              type="danger" 
              size="small" 
              link 
              @click="handleCancel(row)"
            >
              取消预约
            </el-button>

            <!-- 评价打分操作 -->
            <el-button 
              v-if="row.orderStatus === 2" 
              type="primary" 
              size="small" 
              link 
              @click="openReviewDialog(row)"
            >
              服务评价
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- MyBatis-Plus 分页控件 -->
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

    <!-- 拟物化数字票据弹窗 (21st.dev 风格) -->
    <el-dialog v-model="codeDialogVisible" title="数字入场核销凭证" width="380px" center>
      <div class="ticket-container card-shadow" v-if="currentOrder">
        <div class="ticket-notch-left"></div>
        <div class="ticket-notch-right"></div>
        
        <div class="ticket-top-section">
          <div class="stub-venue-name">{{ currentOrder.venueName }}</div>
          <div class="stub-date-slot">{{ currentOrder.bookDate }} · {{ currentOrder.timeSlot }}</div>
          <div class="stub-user-hint">凭此 6 位核销码向前台出示核验入场</div>
        </div>

        <div class="ticket-perforation"></div>

        <div class="ticket-bottom-section">
          <div class="stub-number-code">{{ currentOrder.verifyCode }}</div>
          <el-button 
            size="small" 
            type="primary" 
            plain 
            style="margin-top: 10px; border-radius: 20px;"
            @click="copyCode(currentOrder.verifyCode)"
          >
            <el-icon><CopyDocument /></el-icon> 复制核销码
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 评价打分弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="场地设施与服务评价" width="460px">
      <el-form label-position="top">
        <el-form-item label="本次体验评分">
          <el-rate v-model="reviewForm.rating" show-text :texts="['极差', '较差', '一般', '不错', '极为满意']" />
        </el-form-item>
        <el-form-item label="评价心得与建议">
          <el-input 
            v-model="reviewForm.content" 
            type="textarea" 
            rows="4" 
            placeholder="分享场馆的地胶脚感、照明防眩晕度或前台服务态度..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="handleReviewSubmit">
          提交真实评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { CopyDocument } from '@element-plus/icons-vue'
import { getMyOrdersPage, payOrder, cancelOrder, submitReview, getIdempotentToken } from '@/api/booking'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('all')
const orderList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const codeDialogVisible = ref(false)
const currentOrder = ref(null)

const reviewDialogVisible = ref(false)
const submittingReview = ref(false)
const reviewForm = reactive({
  orderId: null,
  rating: 5,
  content: ''
})

async function fetchOrders() {
  loading.value = true
  try {
    const statusParam = activeTab.value === 'all' ? null : parseInt(activeTab.value)
    const res = await getMyOrdersPage({
      current: currentPage.value,
      size: pageSize.value,
      status: statusParam
    })
    orderList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  currentPage.value = 1
  fetchOrders()
}

function getStatusText(status) {
  switch (status) {
    case 0: return '待支付锁定'
    case 1: return '已预约(待核销)'
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

function showVerifyCodeModal(order) {
  currentOrder.value = order
  codeDialogVisible.value = true
}

function copyCode(code) {
  if (!code) return
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success(`核销码 ${code} 已复制到剪贴板！`)
  }).catch(() => {
    ElMessage.info(`核销码为: ${code}`)
  })
}

async function openPayModal(order) {
  try {
    await ElMessageBox.confirm(`确认使用虚拟账户余额支付 ￥${order.totalAmount} 吗？`, '快速支付确认', {
      type: 'warning'
    })
    const token = await getIdempotentToken()
    await payOrder(order.orderNo, token)
    ElMessage.success('支付成功，已生成核销凭证！')
    fetchOrders()
  } catch (e) {
    // canceled
  }
}

async function handleCancel(order) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入取消原因：', '取消预约', {
      confirmButtonText: '确认取消',
      cancelButtonText: '放弃',
      inputPlaceholder: '临时有事 / 计划变更'
    })
    await cancelOrder(order.id, reason || '用户自主取消')
    ElMessage.success('订单已成功取消，费用已原路退回')
    fetchOrders()
  } catch (e) {
    // canceled
  }
}

function openReviewDialog(order) {
  reviewForm.orderId = order.id
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewDialogVisible.value = true
}

async function handleReviewSubmit() {
  if (!reviewForm.content.trim()) {
    ElMessage.warning('请输入您的评价体会')
    return
  }
  submittingReview.value = true
  try {
    await submitReview(reviewForm)
    ElMessage.success('评价提交成功，感谢您的支持！')
    reviewDialogVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    submittingReview.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.my-bookings-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 60px;
}

.page-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.title {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
}

.subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.table-card {
  border-radius: 16px;
  padding: 12px 20px 24px;
}

.mono-order-no {
  font-family: monospace;
  font-size: 13px;
  color: #475569;
}

.venue-cell-title {
  color: #0f172a;
}

.date-cell {
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}

.time-slot-pill {
  font-size: 12px;
  color: #4f46e5;
  font-weight: 700;
  font-family: monospace;
}

.amount-text {
  font-weight: 800;
  color: #e11d48;
  font-size: 15px;
}

.status-tag {
  font-weight: 700;
  border-radius: 6px;
}

.ticket-btn {
  font-family: monospace;
  font-weight: 700;
  border-radius: 6px;
}

.verified-text {
  font-size: 12px;
  color: #10b981;
  font-weight: 600;
}

.text-muted {
  color: #94a3b8;
}

.pagination-bar {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 拟物化票据弹窗 */
.ticket-container {
  padding: 24px;
  text-align: center;
  background: #ffffff;
}

.stub-venue-name {
  font-size: 17px;
  font-weight: 800;
  color: #0f172a;
}

.stub-date-slot {
  font-size: 13px;
  color: #4f46e5;
  font-weight: 700;
  margin-top: 4px;
}

.stub-user-hint {
  font-size: 12px;
  color: #64748b;
  margin-top: 6px;
}

.stub-number-code {
  font-size: 42px;
  font-weight: 900;
  letter-spacing: 8px;
  color: #4f46e5;
  font-family: monospace;
  margin: 8px 0;
  text-shadow: 0 4px 14px rgba(79, 70, 229, 0.3);
}
</style>
