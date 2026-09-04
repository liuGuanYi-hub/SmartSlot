<template>
  <el-drawer
    v-model="visible"
    title="确认场地预约与时段锁定"
    size="420px"
    :before-close="handleClose"
  >
    <div v-if="slotData" class="drawer-content">
      <!-- 场地信息卡片 -->
      <div class="venue-card card-shadow">
        <div class="venue-title">{{ slotData.venue?.name || slotData.venue?.venueName }}</div>
        <div class="venue-meta">
          <el-tag size="small" type="primary">{{ slotData.venue?.categoryName || '标准运动场地' }}</el-tag>
          <span class="price-text">￥{{ slotData.price }} / 小时</span>
        </div>
        <el-divider style="margin: 12px 0;" />
        <div class="detail-item">
          <span class="label">预约日期：</span>
          <span class="val">{{ slotData.date }}</span>
        </div>
        <div class="detail-item">
          <span class="label">预约时段：</span>
          <span class="val highlight">{{ slotData.timeSlot }}</span>
        </div>
      </div>

      <!-- 提示信息 -->
      <el-alert
        title="防超卖提示: 提交后系统将通过 Redis 预占该时段 15 分钟，请在时限内完成支付。"
        type="warning"
        :closable="false"
        show-icon
        style="margin: 16px 0;"
      />

      <!-- 预约联系表单 (带 JSR-303 前端联动校验) -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        class="booking-form"
      >
        <el-form-item label="使用者姓名" prop="contactName">
          <el-input v-model="formData.contactName" placeholder="请输入预约联系人姓名" />
        </el-form-item>
        <el-form-item label="联系手机号" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入11位中国大陆手机号码" maxlength="11" />
        </el-form-item>
      </el-form>

      <!-- 费用汇总与操作按钮 -->
      <div class="drawer-footer">
        <div class="total-bar">
          <span>待支付合计：</span>
          <span class="total-amount">￥{{ slotData.price }}</span>
        </div>
        <div class="btn-group">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitBooking">
            锁定并立即支付
          </el-button>
        </div>
      </div>
    </div>

    <!-- 支付与核销码展示对话框 -->
    <el-dialog
      v-model="payDialogVisible"
      title="模拟收银台"
      width="380px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-if="createdOrder" class="pay-modal-content">
        <div class="order-summary">
          <div class="sum-row">
            <span>订单编号：</span>
            <span class="order-no">{{ createdOrder.orderNo }}</span>
          </div>
          <div class="sum-row">
            <span>应付金额：</span>
            <span class="pay-amount">￥{{ createdOrder.totalAmount }}</span>
          </div>
          <div class="sum-row">
            <span>我的虚拟余额：</span>
            <span>￥{{ userStore.userInfo?.balance || 0 }}</span>
          </div>
        </div>

        <template v-if="!paymentSuccess">
          <el-button
            type="success"
            size="large"
            style="width: 100%; margin-top: 20px;"
            :loading="paying"
            @click="handlePay"
          >
            确认扣款并获取核销码
          </el-button>
        </template>

        <!-- 支付成功与 6 位核销码展示 -->
        <div v-else class="success-box">
          <el-result
            icon="success"
            title="预约成功"
            sub-title="请凭下方 6 位专属核销码前往场馆向前台核销入场"
          >
            <template #extra>
              <div class="verify-code-card">
                <div class="code-title">专属核销码</div>
                <div class="code-number">{{ paidOrder?.verifyCode }}</div>
              </div>
              <el-button type="primary" @click="finishFlow" style="margin-top: 16px;">
                完成并查看日程
              </el-button>
            </template>
          </el-result>
        </div>
      </div>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { lockAndCreateOrder, payOrder } from '@/api/booking'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const emit = defineEmits(['success'])
const router = useRouter()
const userStore = useUserStore()

const visible = ref(false)
const slotData = ref(null)
const submitting = ref(false)
const paying = ref(false)

const payDialogVisible = ref(false)
const createdOrder = ref(null)
const paidOrder = ref(null)
const paymentSuccess = ref(false)

const formRef = ref(null)
const formData = reactive({
  contactName: '',
  contactPhone: ''
})

const formRules = {
  contactName: [{ required: true, message: '请输入使用者姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的11位手机号', trigger: 'blur' }
  ]
}

function open(data) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再进行场地预约')
    router.push('/login')
    return
  }

  slotData.value = data
  formData.contactName = userStore.userInfo?.nickname || userStore.userInfo?.username || ''
  formData.contactPhone = userStore.userInfo?.phone || '13912345678'
  paymentSuccess.value = false
  createdOrder.value = null
  paidOrder.value = null
  visible.value = true
}

function handleClose(done) {
  done()
}

async function submitBooking() {
  if (!formRef.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    const res = await lockAndCreateOrder({
      venueId: slotData.value.venue?.id || slotData.value.venue?.venueId,
      bookDate: slotData.value.date,
      timeSlot: slotData.value.timeSlot,
      contactName: formData.contactName,
      contactPhone: formData.contactPhone
    })
    createdOrder.value = res
    payDialogVisible.value = true
    visible.value = false
    ElMessage.success('时段已成功在 Redis 中加锁预占 15 分钟！')
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

async function handlePay() {
  paying.value = true
  try {
    const res = await payOrder(createdOrder.value.orderNo)
    paidOrder.value = res
    paymentSuccess.value = true
    await userStore.fetchCurrentUser() // 同步余额
    ElMessage.success('模拟支付成功！')
  } catch (e) {
    console.error(e)
  } finally {
    paying.value = false
  }
}

function finishFlow() {
  payDialogVisible.value = false
  emit('success')
  router.push('/my-bookings')
}

defineExpose({
  open
})
</script>

<style scoped>
.drawer-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.venue-card {
  padding: 16px;
  background: #f8fafc;
}

.venue-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.venue-meta {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-text {
  font-size: 16px;
  font-weight: 700;
  color: #4f46e5;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
  color: #475569;
}

.detail-item .highlight {
  font-weight: 700;
  color: #0f172a;
}

.booking-form {
  margin-top: 8px;
}

.drawer-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.total-bar {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 14px;
}

.total-amount {
  font-size: 22px;
  font-weight: 800;
  color: #e11d48;
}

.btn-group {
  display: flex;
  gap: 12px;
}

.btn-group .el-button {
  flex: 1;
}

.order-summary {
  background: #f8fafc;
  padding: 14px;
  border-radius: 8px;
  font-size: 13px;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.sum-row:last-child {
  margin-bottom: 0;
}

.pay-amount {
  font-size: 16px;
  font-weight: 700;
  color: #e11d48;
}

.order-no {
  font-family: monospace;
}

.verify-code-card {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #ffffff;
  padding: 18px 24px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 10px 15px -3px rgba(79, 70, 229, 0.3);
}

.code-title {
  font-size: 13px;
  opacity: 0.9;
  letter-spacing: 1px;
}

.code-number {
  font-size: 32px;
  font-weight: 900;
  letter-spacing: 6px;
  margin-top: 4px;
  font-family: monospace;
}
</style>
