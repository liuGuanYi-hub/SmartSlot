<template>
  <div class="my-bookings-page">
    <div class="page-title-row">
      <div>
        <h2 class="title">我的预约记录</h2>
        <p class="subtitle">查看您的场地预订历程、出示入场核销码或对服务进行打分</p>
      </div>
      <el-button type="primary" plain @click="$router.push('/matrix')">
        继续预约场地
      </el-button>
    </div>

    <!-- 状态切换 Tabs -->
    <el-card shadow="never" class="table-card card-shadow">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部订单" name="all" />
        <el-tab-pane label="待支付(锁定)" name="0" />
        <el-tab-pane label="待核销入场" name="1" />
        <el-tab-pane label="已核销完成" name="2" />
        <el-tab-pane label="已取消" name="3" />
      </el-tabs>

      <!-- 分页数据表格 (MyBatis-Plus 分页插件联动) -->
      <el-table :data="orderList" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="orderNo" label="订单编号" min-width="160" />
        <el-table-column prop="venueName" label="预约场地" min-width="150" />
        <el-table-column label="预约时段" min-width="160">
          <template #default="{ row }">
            <div>{{ row.bookDate }}</div>
            <div class="time-slot-tag">{{ row.timeSlot }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="支付金额" width="100">
          <template #default="{ row }">
            <span class="amount-text">￥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)">
              {{ getStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核销凭证" width="120">
          <template #default="{ row }">
            <el-button 
              v-if="row.verifyCode && row.orderStatus === 1" 
              type="success" 
              size="small" 
              link 
              @click="showVerifyCodeModal(row)"
            >
              码: {{ row.verifyCode }}
            </el-button>
            <span v-else-if="row.orderStatus === 2" class="verified-text">已核销</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <!-- 待支付操作 -->
            <el-button 
              v-if="row.orderStatus === 0" 
              type="primary" 
              size="small" 
              @click="openPayModal(row)"
            >
              立即支付
            </el-button>

            <!-- 取消预约操作 -->
            <el-button 
              v-if="row.orderStatus === 0 || row.orderStatus === 1" 
              type="danger" 
              size="small" 
              link 
              @click="handleCancel(row)"
            >
              取消
            </el-button>

            <!-- 评价打分操作 -->
            <el-button 
              v-if="row.orderStatus === 2" 
              type="primary" 
              size="small" 
              link 
              @click="openReviewDialog(row)"
            >
              评价打分
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

    <!-- 核销码弹窗 -->
    <el-dialog v-model="codeDialogVisible" title="您的专属入场核销码" width="360px" center>
      <div class="qr-verify-box" v-if="currentOrder">
        <div class="qr-label">向场馆前台工作人员出示即可核验入场</div>
        <div class="large-code-badge">{{ currentOrder.verifyCode }}</div>
        <div class="qr-detail">
          <div>{{ currentOrder.venueName }}</div>
          <div>{{ currentOrder.bookDate }} ({{ currentOrder.timeSlot }})</div>
        </div>
      </div>
    </el-dialog>

    <!-- 评价打分弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="场地使用服务评价" width="450px">
      <el-form label-position="top">
        <el-form-item label="总体满意度打分">
          <el-rate v-model="reviewForm.rating" show-text :texts="['极差', '较差', '一般', '不错', '非常满意']" />
        </el-form-item>
        <el-form-item label="评价心得或建议">
          <el-input 
            v-model="reviewForm.content" 
            type="textarea" 
            rows="4" 
            placeholder="请分享场地的灯光、地胶、通风或服务体验..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="handleReviewSubmit">
          提交评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyOrdersPage, payOrder, cancelOrder, submitReview } from '@/api/booking'
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

async function openPayModal(order) {
  try {
    await ElMessageBox.confirm(`确认使用账户余额支付 ￥${order.totalAmount} 吗？`, '快速支付确认', {
      type: 'warning'
    })
    await payOrder(order.orderNo)
    ElMessage.success('支付成功，已生成核销凭证！')
    fetchOrders()
  } catch (e) {
    // user cancel or err
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
    ElMessage.success('订单已成功取消')
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
    ElMessage.warning('请输入评价内容')
    return
  }
  submittingReview.value = true
  try {
    await submitReview(reviewForm)
    ElMessage.success('评价提交成功！')
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.page-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.table-card {
  border-radius: 12px;
  padding: 8px 16px 20px;
}

.time-slot-tag {
  font-size: 12px;
  color: #4f46e5;
  font-weight: 600;
}

.amount-text {
  font-weight: 700;
  color: #e11d48;
}

.verified-text {
  font-size: 12px;
  color: #10b981;
}

.text-muted {
  color: #94a3b8;
}

.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.qr-verify-box {
  text-align: center;
  padding: 10px 0;
}

.qr-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 16px;
}

.large-code-badge {
  font-size: 38px;
  font-weight: 900;
  letter-spacing: 8px;
  color: #4f46e5;
  font-family: monospace;
  background: #f5f3ff;
  padding: 12px 24px;
  border-radius: 10px;
  display: inline-block;
  margin-bottom: 16px;
  border: 1px dashed #8b5cf6;
}

.qr-detail {
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}
</style>
