<template>
  <el-drawer
    v-model="visible"
    title="场馆时段预约与锁定"
    size="460px"
    :before-close="handleClose"
    class="custom-booking-drawer"
  >
    <div v-if="slotData" class="drawer-content">
      <!-- 场地信息拟物化卡片 (21st.dev 灵感) -->
      <div class="ticket-preview-card card-shadow">
        <div class="ticket-header">
          <div class="t-category-badge">{{ slotData.venue?.categoryName || '运动场地' }}</div>
          <div class="t-price">￥{{ slotData.price }}<small>/小时</small></div>
        </div>
        <h3 class="t-title">{{ slotData.venue?.name || slotData.venue?.venueName }}</h3>
        
        <div class="t-meta-grid">
          <div class="meta-cell">
            <span class="m-label">预约日期</span>
            <span class="m-val">{{ slotData.date }}</span>
          </div>
          <div class="meta-cell">
            <span class="m-label">入场时段</span>
            <span class="m-val highlight">{{ slotData.timeSlot }}</span>
          </div>
        </div>
      </div>

      <!-- 防超卖与分布式锁机制提示 (带呼吸感) -->
      <div class="lock-mechanism-tip">
        <div class="tip-icon"><span class="live-dot"></span></div>
        <div class="tip-body">
          <strong>Redis 实时锁定时段：</strong>
          <span>提交后该时段将在集群中原子锁定 15 分钟，其他用户无法重复抢占，超时未支付将自动回滚释放。</span>
        </div>
      </div>

      <!-- 联系人表单 (带 JSR-303 前端联动校验) -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        class="booking-interactive-form"
      >
        <el-form-item label="使用人姓名" prop="contactName">
          <el-input 
            v-model="formData.contactName" 
            placeholder="请输入使用人姓名" 
            size="large"
          >
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item label="联系电话" prop="contactPhone">
          <el-input 
            v-model="formData.contactPhone" 
            placeholder="请输入 11 位手机号码" 
            maxlength="11" 
            size="large"
          >
            <template #prefix><el-icon><Iphone /></el-icon></template>
          </el-input>
        </el-form-item>
      </el-form>

      <!-- 底部费用结算栏 -->
      <div class="drawer-footer-bar">
        <div class="amount-summary">
          <span class="summary-label">合计应付</span>
          <div class="summary-price">
            <span class="sym">￥</span>
            <span class="val">{{ slotData.price }}</span>
          </div>
        </div>
        <div class="action-buttons">
          <el-button size="large" @click="visible = false">取消</el-button>
          <el-button 
            type="primary" 
            size="large" 
            class="submit-lock-btn shimmer-badge" 
            :loading="submitting" 
            @click="submitBooking"
          >
            锁定并支付
          </el-button>
        </div>
      </div>
    </div>

    <!-- 拟真数字票据收银台模态框 (21st.dev 风格票根出单) -->
    <el-dialog
      v-model="payDialogVisible"
      title="模拟收银台 · 专属入场凭证"
      width="420px"
      append-to-body
      :close-on-click-modal="false"
      class="ticket-dialog"
    >
      <div v-if="createdOrder" class="checkout-wrapper">
        <template v-if="!paymentSuccess">
          <div class="pre-pay-summary">
            <div class="order-id-row">
              <span>订单流水号：</span>
              <span class="mono-code">{{ createdOrder.orderNo }}</span>
            </div>
            <div class="order-amount-row">
              <span>支付金额：</span>
              <span class="big-pay-amount">￥{{ createdOrder.totalAmount }}</span>
            </div>
            <div class="balance-status-row">
              <span>当前可用虚拟余额：</span>
              <span class="balance-hint">￥{{ userStore.userInfo?.balance || 0 }}</span>
            </div>
          </div>

          <el-button
            type="success"
            size="large"
            style="width: 100%; margin-top: 24px; border-radius: 12px; font-weight: 700; height: 48px;"
            :loading="paying"
            @click="handlePay"
          >
            立即扣款并生成 6 位核销票据
          </el-button>
        </template>

        <!-- 支付成功后：拟物化打孔入场票据 (Ticket Stub) -->
        <div v-else class="ticket-result-zone">
          <div class="ticket-container card-shadow">
            <div class="ticket-notch-left"></div>
            <div class="ticket-notch-right"></div>
            
            <div class="ticket-top-section">
              <div class="ticket-venue-badge">{{ createdOrder.venueName || '专业运动场地' }}</div>
              <div class="ticket-date-time">{{ createdOrder.bookDate }} ({{ createdOrder.timeSlot }})</div>
              <div class="ticket-guest-name">持票人: {{ createdOrder.contactName }}</div>
            </div>

            <div class="ticket-perforation"></div>

            <div class="ticket-bottom-section">
              <div class="stub-label">到场核验凭证码 (VERIFY CODE)</div>
              <div class="glowing-code-text">{{ paidOrder?.verifyCode }}</div>
              <el-button 
                size="small" 
                type="primary" 
                plain 
                style="margin-top: 10px; border-radius: 20px;"
                @click="copyVerifyCode(paidOrder?.verifyCode)"
              >
                <el-icon><CopyDocument /></el-icon> 复制核销码
              </el-button>
            </div>
          </div>

          <el-button 
            type="primary" 
            size="large" 
            style="width: 100%; margin-top: 20px; border-radius: 12px;"
            @click="finishFlow"
          >
            完成并前往我的预约行程
          </el-button>
        </div>
      </div>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User, Iphone, CopyDocument } from '@element-plus/icons-vue'
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
  contactName: [{ required: true, message: '请输入使用人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入合法的 11 位手机号码', trigger: 'blur' }
  ]
}

function open(data) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录会员账号')
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
    ElMessage.success('已成功在 Redis 中锁定该时段（15分钟保护）！')
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
    await userStore.fetchCurrentUser()
    ElMessage.success('扣款成功，已生成专属数字票根！')
  } catch (e) {
    console.error(e)
  } finally {
    paying.value = false
  }
}

function copyVerifyCode(code) {
  if (!code) return
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success(`核销码 ${code} 已复制到剪贴板！`)
  }).catch(() => {
    ElMessage.info(`核销码为: ${code}`)
  })
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

.ticket-preview-card {
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #e0e7ff 100%);
  border-radius: 16px;
  border: 1px solid #c7d2fe;
}

.ticket-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.t-category-badge {
  font-size: 11px;
  font-weight: 700;
  color: #4f46e5;
  background: rgba(79, 70, 229, 0.12);
  padding: 3px 8px;
  border-radius: 6px;
}

.t-price {
  font-size: 18px;
  font-weight: 800;
  color: #4f46e5;
}

.t-price small {
  font-size: 11px;
  color: #64748b;
  font-weight: normal;
}

.t-title {
  font-size: 17px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 14px;
}

.t-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  background: #ffffff;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.meta-cell {
  display: flex;
  flex-direction: column;
}

.m-label {
  font-size: 11px;
  color: #64748b;
}

.m-val {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin-top: 2px;
}

.m-val.highlight {
  color: #4f46e5;
}

.lock-mechanism-tip {
  display: flex;
  gap: 10px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 12px;
  padding: 12px 14px;
  margin: 18px 0;
  font-size: 12px;
  color: #92400e;
  line-height: 1.5;
}

.booking-interactive-form {
  margin-top: 10px;
}

.drawer-footer-bar {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.amount-summary {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 14px;
}

.summary-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
}

.summary-price {
  color: #e11d48;
  font-weight: 900;
}

.summary-price .sym {
  font-size: 16px;
}

.summary-price .val {
  font-size: 26px;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.action-buttons .el-button {
  flex: 1;
}

.submit-lock-btn {
  font-weight: 700;
  background: #4f46e5;
}

/* 拟真票据模态框 (21st.dev 风格) */
.checkout-wrapper {
  padding: 10px 0;
}

.pre-pay-summary {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 18px;
  border-radius: 12px;
  font-size: 13px;
}

.order-id-row, .order-amount-row, .balance-status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.balance-status-row {
  margin-bottom: 0;
}

.mono-code {
  font-family: monospace;
  font-weight: 600;
  color: #334155;
}

.big-pay-amount {
  font-size: 20px;
  font-weight: 800;
  color: #e11d48;
}

.balance-hint {
  font-weight: 700;
  color: #059669;
}

/* 拟真票根展示区 */
.ticket-container {
  padding: 24px;
  text-align: center;
}

.ticket-top-section {
  padding-bottom: 6px;
}

.ticket-venue-badge {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.ticket-date-time {
  font-size: 13px;
  color: #4f46e5;
  font-weight: 700;
  margin-top: 4px;
}

.ticket-guest-name {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

.stub-label {
  font-size: 11px;
  color: #94a3b8;
  letter-spacing: 1.5px;
  font-weight: 700;
}

.glowing-code-text {
  font-size: 42px;
  font-weight: 900;
  letter-spacing: 8px;
  color: #4f46e5;
  font-family: monospace;
  margin: 6px 0;
  text-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}
</style>
