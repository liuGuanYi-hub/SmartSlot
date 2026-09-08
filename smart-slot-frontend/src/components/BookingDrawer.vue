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

      <!-- 预订模式分段切换 (自主包场 vs 发起 AA 拼场) -->
      <div class="booking-mode-switcher">
        <button 
          type="button"
          class="mode-switch-btn" 
          :class="{ active: bookingMode === 'solo' }"
          @click="bookingMode = 'solo'"
        >
          <el-icon><Calendar /></el-icon>
          <span>自主订场 (独享)</span>
        </button>
        <button 
          type="button"
          class="mode-switch-btn" 
          :class="{ active: bookingMode === 'match' }"
          @click="bookingMode = 'match'"
        >
          <el-icon><Connection /></el-icon>
          <span>发起拼场 (找搭子)</span>
          <span class="hot-pill">AA制</span>
        </button>
      </div>

      <!-- 防超卖与分布式锁机制提示 (带呼吸感) -->
      <div class="lock-mechanism-tip">
        <div class="tip-icon"><span class="live-dot"></span></div>
        <div class="tip-body">
          <strong>{{ bookingMode === 'solo' ? 'Redis 实时锁定时段：' : 'AA 拼场招募预占：' }}</strong>
          <span>{{ bookingMode === 'solo' ? '提交后该时段将在集群中原子锁定 15 分钟，其他用户无法重复抢占，超时未支付将自动回滚释放。' : '发起后时段将为您优先锁定招募，满员即自动转正出票并下发专属入场码，若未成团可全额原路退款。' }}</span>
        </div>
      </div>

      <!-- 模式 1: 自主订场表单 + 优惠券抵扣 -->
      <template v-if="bookingMode === 'solo'">
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

        <!-- 优惠券选择器 -->
        <div class="coupon-select-card card-shadow">
          <div class="c-head">
            <div class="c-title-row">
              <el-icon><Ticket /></el-icon>
              <span>优惠券抵扣</span>
            </div>
            <span v-if="selectedCouponDiscount > 0" class="c-badge">已减 ￥{{ selectedCouponDiscount }}</span>
          </div>

          <el-select 
            v-model="selectedUserCouponId" 
            placeholder="暂无可用优惠券或不使用" 
            clearable
            size="large"
            style="width: 100%; margin-top: 8px;"
            @change="onCouponChange"
          >
            <el-option 
              v-for="uc in availableCoupons" 
              :key="uc.id" 
              :label="`${uc.couponName} (抵扣 ￥${calculateItemDiscount(uc)})`" 
              :value="uc.id" 
            />
          </el-select>
          <div v-if="availableCoupons.length === 0" class="no-coupon-tip">
            暂无适用的优惠券？<span class="link-span" @click="$router.push('/coupons')">去领券中心免费领券 ▶</span>
          </div>
        </div>
      </template>

      <!-- 模式 2: 发起 AA 拼场招募表单 -->
      <template v-else>
        <el-form
          ref="matchFormRef"
          :model="matchForm"
          :rules="matchRules"
          label-position="top"
          class="booking-interactive-form"
        >
          <el-form-item label="拼场招募主题" prop="title">
            <el-input 
              v-model="matchForm.title" 
              placeholder="如：李宁双打进阶局缺2人，AA制畅打" 
              size="large"
            />
          </el-form-item>

          <div class="drawer-grid-2">
            <el-form-item label="运动标签" prop="sportTag">
              <el-select v-model="matchForm.sportTag" size="large" style="width: 100%;">
                <el-option label="双打进阶·AA畅打" value="双打进阶·AA畅打" />
                <el-option label="新手友好·破冰娱乐" value="新手友好·破冰娱乐" />
                <el-option label="半场3v3热血对抗" value="半场3v3热血对抗" />
                <el-option label="单打稳定对拉切磋" value="单打稳定对拉切磋" />
              </el-select>
            </el-form-item>

            <el-form-item label="招募总人数 (含您)" prop="targetMembers">
              <el-input-number 
                v-model="matchForm.targetMembers" 
                :min="2" 
                :max="10" 
                size="large" 
                style="width: 100%;" 
              />
            </el-form-item>
          </div>

          <el-form-item label="招募寄语 / 装备说明 (选填)">
            <el-input 
              v-model="matchForm.description" 
              type="textarea" 
              rows="2" 
              placeholder="说明水平等级、自带球拍等..." 
            />
          </el-form-item>
        </el-form>

        <!-- AA 费用核算预览卡 -->
        <div class="aa-preview-banner">
          <div class="ap-row">
            <span>场地原价：</span>
            <span>￥{{ slotData.price }}</span>
          </div>
          <div class="ap-row">
            <span>分摊人数：</span>
            <span>{{ matchForm.targetMembers }} 人</span>
          </div>
          <div class="ap-row ap-lead">
            <span>人均 AA 费用：</span>
            <span class="ap-price">￥{{ currentMatchCostPerPerson }} / 人</span>
          </div>
        </div>
      </template>

      <!-- 底部费用结算栏 -->
      <div class="drawer-footer-bar">
        <div class="amount-summary">
          <span class="summary-label">{{ bookingMode === 'solo' ? '券后实付' : '首付 AA 份额' }}</span>
          <div class="summary-price">
            <span class="sym">￥</span>
            <span class="val">{{ currentPayAmount }}</span>
          </div>
          <div v-if="bookingMode === 'solo' && selectedCouponDiscount > 0" class="discount-cross">
            原价 ￥{{ slotData.price }}
          </div>
        </div>
        <div class="action-buttons">
          <el-button size="large" @click="visible = false">取消</el-button>
          <el-button 
            type="primary" 
            size="large" 
            class="submit-lock-btn shimmer-badge" 
            :loading="submitting" 
            @click="handleSubmit"
          >
            {{ bookingMode === 'solo' ? '锁定并支付' : '支付首款并发起' }}
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
              <div class="holographic-foil-strip"></div>
              <div class="stub-label">到场核验凭证码 (VERIFY PASS)</div>
              <div class="glowing-code-text">{{ paidOrder?.verifyCode }}</div>
              
              <!-- 拟真动态二维码与防伪条 -->
              <div class="ticket-qr-box">
                <svg viewBox="0 0 100 100" class="qr-svg">
                  <!-- 四角定位符 -->
                  <rect x="10" y="10" width="24" height="24" rx="4" fill="#0f172a" />
                  <rect x="14" y="14" width="16" height="16" rx="2" fill="#ffffff" />
                  <rect x="18" y="18" width="8" height="8" rx="1" fill="#0f172a" />
                  
                  <rect x="66" y="10" width="24" height="24" rx="4" fill="#0f172a" />
                  <rect x="70" y="14" width="16" height="16" rx="2" fill="#ffffff" />
                  <rect x="74" y="18" width="8" height="8" rx="1" fill="#0f172a" />
                  
                  <rect x="10" y="66" width="24" height="24" rx="4" fill="#0f172a" />
                  <rect x="14" y="70" width="16" height="16" rx="2" fill="#ffffff" />
                  <rect x="18" y="74" width="8" height="8" rx="1" fill="#0f172a" />

                  <!-- 数据矩阵仿真条块 -->
                  <rect x="42" y="12" width="6" height="14" fill="#0f172a" />
                  <rect x="52" y="16" width="8" height="6" fill="#0f172a" />
                  <rect x="40" y="40" width="20" height="20" rx="3" fill="#4f46e5" />
                  <rect x="15" y="44" width="14" height="6" fill="#0f172a" />
                  <rect x="72" y="45" width="16" height="8" fill="#0f172a" />
                  <rect x="42" y="70" width="8" height="16" fill="#0f172a" />
                  <rect x="56" y="68" width="14" height="6" fill="#0f172a" />
                  <rect x="76" y="76" width="10" height="12" fill="#0f172a" />
                </svg>
              </div>

              <!-- 30秒动态防伪安全指示 (Week 2 企业级防伪亮点) -->
              <div class="totp-anti-counterfeit">
                <span class="totp-shield">🛡️ 企业防伪</span>
                <span class="totp-timer">动态防伪 · 前台扫码秒核销</span>
              </div>

              <div style="margin-top: 12px;">
                <el-button 
                  size="small" 
                  type="primary" 
                  plain 
                  style="border-radius: 20px;"
                  @click="copyVerifyCode(paidOrder?.verifyCode)"
                >
                  <el-icon><CopyDocument /></el-icon> 复制核销码
                </el-button>
              </div>
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

    <!-- 多渠道支付网关收银台 (支付宝/微信/余额) -->
    <CashierModal ref="cashierModalRef" @pay-success="onCashierSuccess" />
  </el-drawer>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { User, Iphone, CopyDocument, Ticket, Connection, Calendar } from '@element-plus/icons-vue'
import { lockAndCreateOrder, payOrder, getIdempotentToken } from '@/api/booking'
import { calculateOptimalCoupon } from '@/api/coupon'
import { createMatch } from '@/api/match'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import CashierModal from '@/components/CashierModal.vue'

const emit = defineEmits(['success'])
const router = useRouter()
const userStore = useUserStore()

const visible = ref(false)
const slotData = ref(null)
const submitting = ref(false)
const paying = ref(false)

const bookingMode = ref('solo') // 'solo' | 'match'

// 优惠券状态
const availableCoupons = ref([])
const optimalData = ref(null)
const selectedUserCouponId = ref(null)

// 拼场表单
const matchFormRef = ref(null)
const matchForm = reactive({
  title: '',
  sportTag: '双打进阶·AA畅打',
  targetMembers: 4,
  description: ''
})

const matchRules = {
  title: [{ required: true, message: '请输入拼场招募主题', trigger: 'blur' }],
  targetMembers: [{ required: true, message: '请设置招募人数', trigger: 'change' }]
}

const payDialogVisible = ref(false)
const cashierModalRef = ref(null)
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

const selectedCouponDiscount = computed(() => {
  if (!selectedUserCouponId.value || !availableCoupons.value.length) return 0
  const c = availableCoupons.value.find(item => item.id === selectedUserCouponId.value)
  return c ? calculateItemDiscount(c) : 0
})

const currentMatchCostPerPerson = computed(() => {
  if (!slotData.value?.price || !matchForm.targetMembers) return '0.00'
  return (slotData.value.price / matchForm.targetMembers).toFixed(2)
})

const currentPayAmount = computed(() => {
  if (bookingMode.value === 'solo') {
    const orig = slotData.value?.price || 0
    const disc = selectedCouponDiscount.value || 0
    return Math.max(0.01, orig - disc).toFixed(2)
  } else {
    return currentMatchCostPerPerson.value
  }
})

function calculateItemDiscount(c) {
  const orig = slotData.value?.price || 0
  if (c.couponType === 2) {
    const rate = c.discountRate || 1
    return (orig * (1 - rate)).toFixed(2)
  }
  return (c.discountAmount || 0).toFixed(2)
}

function onCouponChange(val) {
  // selectedUserCouponId.value is updated
}

async function open(data) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录会员账号')
    router.push('/login')
    return
  }

  slotData.value = data
  bookingMode.value = 'solo'
  formData.contactName = userStore.userInfo?.nickname || userStore.userInfo?.username || ''
  formData.contactPhone = userStore.userInfo?.phone || '13912345678'
  paymentSuccess.value = false
  createdOrder.value = null
  paidOrder.value = null

  // 默认拼场主题
  matchForm.title = `${data.venue?.name || '场地'} AA畅打切磋局`
  matchForm.sportTag = '双打进阶·AA畅打'
  matchForm.targetMembers = 4
  matchForm.description = ''

  visible.value = true

  // 请求优惠券智能推荐
  try {
    const optRes = await calculateOptimalCoupon({
      venueId: data.venue?.id || data.venue?.venueId,
      amount: data.price
    })
    optimalData.value = optRes
    availableCoupons.value = optRes?.availableCoupons || []
    if (optRes?.bestCoupon) {
      selectedUserCouponId.value = optRes.bestCoupon.id
    } else {
      selectedUserCouponId.value = null
    }
  } catch (e) {
    console.error('获取优惠券智能推荐异常:', e)
  }
}

function handleClose(done) {
  done()
}

async function handleSubmit() {
  if (bookingMode.value === 'solo') {
    await submitBooking()
  } else {
    await submitMatch()
  }
}

async function submitMatch() {
  if (!matchFormRef.value) return
  await matchFormRef.value.validate()

  submitting.value = true
  try {
    await createMatch({
      venueId: slotData.value.venue?.id || slotData.value.venue?.venueId,
      bookDate: slotData.value.date,
      timeSlot: slotData.value.timeSlot,
      title: matchForm.title,
      sportTag: matchForm.sportTag,
      targetMembers: matchForm.targetMembers,
      description: matchForm.description
    })
    visible.value = false
    ElMessage.success('🎉 恭喜！拼场招募发起成功，已锁定该时段并进入拼场大厅！')
    await userStore.fetchCurrentUser?.()
    emit('success')
    router.push('/match')
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

async function submitBooking() {
  if (!formRef.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    const token = await getIdempotentToken()
    const res = await lockAndCreateOrder({
      venueId: slotData.value.venue?.id || slotData.value.venue?.venueId,
      bookDate: slotData.value.date,
      timeSlot: slotData.value.timeSlot,
      contactName: formData.contactName,
      contactPhone: formData.contactPhone,
      userCouponId: selectedUserCouponId.value || undefined
    }, token)
    createdOrder.value = res
    visible.value = false
    ElMessage.success('已成功在 Redis 中锁定该时段（15分钟保护）！')
    cashierModalRef.value?.open({
      orderNo: res.orderNo,
      totalAmount: res.totalAmount,
      venueName: slotData.value.venue?.name,
      bookDate: res.bookDate,
      timeSlot: res.timeSlot
    })
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

async function onCashierSuccess(payRes) {
  paidOrder.value = {
    ...createdOrder.value,
    ...payRes
  }
  paymentSuccess.value = true
  payDialogVisible.value = true
  await userStore.fetchCurrentUser?.()
  emit('success')
}

async function handlePay() {
  paying.value = true
  try {
    const token = await getIdempotentToken()
    const res = await payOrder(createdOrder.value.orderNo, token)
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

/* 全息流动彩虹光斑防伪带 (Week 2 亮点) */
.ticket-bottom-section {
  position: relative;
  overflow: hidden;
  padding: 10px 0;
}

.holographic-foil-strip {
  position: absolute;
  top: 0;
  left: -100%;
  width: 300%;
  height: 100%;
  background: linear-gradient(
    115deg,
    transparent 25%,
    rgba(244, 114, 182, 0.12) 35%,
    rgba(192, 132, 252, 0.22) 45%,
    rgba(129, 140, 248, 0.22) 55%,
    rgba(56, 189, 248, 0.16) 65%,
    transparent 75%
  );
  pointer-events: none;
  animation: holographicShift 6s linear infinite;
}

@keyframes holographicShift {
  0% { transform: translateX(0); }
  100% { transform: translateX(50%); }
}

.ticket-qr-box {
  width: 90px;
  height: 90px;
  margin: 10px auto;
  background: #ffffff;
  padding: 6px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.qr-svg {
  width: 100%;
  height: 100%;
}

.totp-anti-counterfeit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 11px;
  color: #4338ca;
  background: #eef2ff;
  padding: 4px 12px;
  border-radius: 999px;
  font-weight: 600;
}

/* 预订模式分段切换 */
.booking-mode-switcher {
  display: flex;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 12px;
  margin-bottom: 16px;
  gap: 4px;
}

.mode-switch-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.mode-switch-btn.active {
  background: #ffffff;
  color: #4f46e5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.hot-pill {
  font-size: 10px;
  background: #ef4444;
  color: #fff;
  padding: 1px 5px;
  border-radius: 6px;
  font-weight: 800;
}

/* 优惠券选择卡片 */
.coupon-select-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  margin-top: 16px;
}

.c-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.c-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.c-badge {
  font-size: 12px;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 700;
}

.no-coupon-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 8px;
}

.link-span {
  color: #4f46e5;
  cursor: pointer;
  font-weight: 600;
}

.link-span:hover {
  text-decoration: underline;
}

.drawer-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* AA 费用预览卡 */
.aa-preview-banner {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), rgba(99, 102, 241, 0.04));
  border: 1px solid rgba(79, 70, 229, 0.2);
  border-radius: 12px;
  padding: 14px 16px;
  margin-top: 14px;
}

.ap-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
}

.ap-lead {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed rgba(79, 70, 229, 0.2);
}

.ap-price {
  font-size: 18px;
  color: #ef4444;
  font-weight: 900;
}

.discount-cross {
  font-size: 12px;
  color: #94a3b8;
  text-decoration: line-through;
  margin-top: 2px;
}
</style>
