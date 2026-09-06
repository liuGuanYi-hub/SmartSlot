<template>
  <el-dialog
    v-model="visible"
    title=""
    width="560px"
    :close-on-click-modal="false"
    append-to-body
    class="cashier-dialog"
    @close="handleClose"
  >
    <div class="cashier-wrapper">
      <!-- 顶部 Header -->
      <div class="cashier-header">
        <div class="header-left">
          <span class="cashier-tag">企业级收银台</span>
          <h3 class="cashier-title">安全收单与网关结算</h3>
        </div>
        <div class="countdown-badge" :class="{ 'warning-time': remainingSeconds < 180 }">
          <el-icon><Timer /></el-icon>
          <span>支付倒计时: {{ formattedTime }}</span>
        </div>
      </div>

      <!-- 订单信息简报 -->
      <div class="order-summary-card">
        <div class="summary-top">
          <span class="order-no">单号: {{ orderData?.orderNo || '—' }}</span>
          <span class="amount-tag">
            <span class="currency">￥</span>
            <span class="amount-val">{{ Number(orderData?.totalAmount || 0).toFixed(2) }}</span>
          </span>
        </div>
        <div class="summary-meta">
          <div class="meta-item">
            <span class="label">预约场馆:</span>
            <span class="val">{{ orderData?.venueName || '指定场地' }}</span>
          </div>
          <div class="meta-item">
            <span class="label">预约日期:</span>
            <span class="val">{{ orderData?.bookDate || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label">预约时段:</span>
            <span class="val">{{ orderData?.timeSlot || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 支付方式切换卡片 -->
      <div class="channel-section">
        <div class="section-label">请选择支付结算渠道:</div>
        <div class="channel-grid">
          <!-- 支付宝 -->
          <div
            class="channel-item alipay"
            :class="{ active: activeChannel === 'ALIPAY' }"
            @click="selectChannel('ALIPAY')"
          >
            <div class="channel-icon-wrap ali-icon">
              <svg viewBox="0 0 1024 1024" width="22" height="22">
                <path fill="#1677ff" d="M853.333 170.667v682.666H170.667V170.667h682.666m85.334-85.334H85.333v853.334h853.334V85.333z"/>
                <path fill="#1677ff" d="M685.227 618.667c-32.854 0-112.214-36.267-176.64-106.667-42.667 46.507-94.72 87.893-157.014 117.76-13.653 6.4-29.866.427-36.266-13.227-6.4-13.653-.427-29.866 13.226-36.266 57.174-27.307 104.96-65.28 144.214-107.947-49.067-16.214-103.254-29.014-162.134-36.694-15.36-1.706-25.6-15.786-23.893-30.72 1.706-15.36 15.786-25.6 30.72-23.893 72.106 9.387 137.813 25.6 195.413 46.933 13.227-28.16 23.467-58.453 30.293-89.6H426.667v-51.2h153.6v-68.266h51.2v68.266h145.066v51.2H660.48c-6.827 25.6-15.36 50.347-26.027 73.813 54.187 18.774 103.254 43.094 144.214 71.68 12.373 8.534 15.786 25.174 7.253 37.547-8.533 12.8-25.173 15.787-37.547 7.253-37.12-25.6-81.493-47.36-130.56-63.573 50.347 55.466 111.787 83.2 137.387 83.2 15.36 0 27.733 12.374 27.733 27.734 0 14.933-12.8 27.733-28.16 27.733z"/>
              </svg>
            </div>
            <div class="channel-info">
              <span class="name">支付宝支付</span>
              <span class="sub">沙箱 RSA2 异步 Webhook</span>
            </div>
            <span class="badge-tag">推荐</span>
          </div>

          <!-- 微信支付 -->
          <div
            class="channel-item wechat"
            :class="{ active: activeChannel === 'WECHAT' }"
            @click="selectChannel('WECHAT')"
          >
            <div class="channel-icon-wrap wx-icon">
              <svg viewBox="0 0 1024 1024" width="22" height="22">
                <path fill="#07c160" d="M375.467 597.333c-14.934 0-29.867-12.8-29.867-27.733 0-14.933 12.8-27.733 29.867-27.733s29.866 12.8 29.866 27.733c0 14.933-14.933 27.733-29.866 27.733m170.666 0c-14.933 0-29.866-12.8-29.866-27.733 0-14.933 12.8-27.733 29.866-27.733s29.867 12.8 29.867 27.733c0 14.933-14.934 27.733-29.867 27.733m221.867-260.266c-132.267 0-243.2 93.866-243.2 213.333 0 42.667 14.933 81.067 40.533 115.2l-25.6 76.8 89.6-44.8c42.667 12.8 85.334 21.333 138.667 21.333 132.267 0 243.2-93.866 243.2-213.333 0-119.467-110.933-213.333-243.2-213.333M426.667 128C213.333 128 42.667 268.8 42.667 448c0 98.133 55.466 183.467 140.8 243.2l-34.134 106.667 128-64c46.934 12.8 98.134 21.333 149.334 21.333 17.066 0 34.133-1.067 51.2-2.133-8.534-34.134-12.8-68.267-12.8-104.534 0-162.133 145.066-294.4 324.266-294.4 12.8 0 25.6.8 38.4 2.134C802.133 226.133 627.2 128 426.667 128z"/>
              </svg>
            </div>
            <div class="channel-info">
              <span class="name">微信支付</span>
              <span class="sub">HMAC-SHA256 签名通知</span>
            </div>
          </div>

          <!-- 余额支付 -->
          <div
            class="channel-item balance"
            :class="{ active: activeChannel === 'BALANCE' }"
            @click="selectChannel('BALANCE')"
          >
            <div class="channel-icon-wrap bal-icon">
              <el-icon :size="20"><Wallet /></el-icon>
            </div>
            <div class="channel-info">
              <span class="name">账户余额</span>
              <span class="sub">可用: ￥{{ Number(userStore.user?.balance || 0).toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间扫码与展示交互区域 -->
      <div v-if="activeChannel !== 'BALANCE'" class="qrcode-interactive-box">
        <div class="qr-frame-wrapper">
          <!-- 四角定位标 -->
          <div class="corner-marker top-left"></div>
          <div class="corner-marker top-right"></div>
          <div class="corner-marker bottom-left"></div>
          <div class="corner-marker bottom-right"></div>
          <!-- 激光扫描线动画 -->
          <div class="laser-scanner-line"></div>

          <!-- 仿真二维码本体 -->
          <div class="qr-canvas">
            <div class="qr-pattern-grid">
              <div class="qr-finder tl"></div>
              <div class="qr-finder tr"></div>
              <div class="qr-finder bl"></div>
              <!-- 模拟矩阵噪点 -->
              <div class="qr-dots-bg"></div>
              <!-- 中心 Logo -->
              <div class="qr-center-logo" :class="activeChannel.toLowerCase()">
                <span v-if="activeChannel === 'ALIPAY'">支</span>
                <span v-else>微</span>
              </div>
            </div>
          </div>
        </div>

        <div class="qr-hint-text">
          <p class="main-hint">打开手机<strong>{{ activeChannel === 'ALIPAY' ? '支付宝' : '微信' }}</strong> [扫一扫] 完成扣款</p>
          <p class="sub-hint">支持支付宝沙箱环境 APP 或开发者工具即时联调</p>
        </div>

        <!-- 一键触发沙箱模拟扣款按钮 -->
        <div class="sandbox-action-area">
          <el-button
            type="primary"
            size="large"
            class="sandbox-pay-btn"
            :loading="payingLoading"
            @click="handleSimulatePay"
          >
            <el-icon><Iphone /></el-icon> 📱 模拟手机扫码扣款成功 (触发沙箱异步 Webhook)
          </el-button>
          <div class="sandbox-tip">
            无需真实绑定银行卡，点击后服务端将模拟收到第三方异步回调并自动出票
          </div>
        </div>
      </div>

      <!-- 余额支付展示区域 -->
      <div v-else class="balance-pay-box">
        <div class="balance-card">
          <div class="bal-label">当前账户扣款预览:</div>
          <div class="bal-diff-row">
            <span>扣除前: ￥{{ Number(userStore.user?.balance || 0).toFixed(2) }}</span>
            <span class="arrow">➜</span>
            <span class="after">扣除后: ￥{{ Math.max(0, Number(userStore.user?.balance || 0) - Number(orderData?.totalAmount || 0)).toFixed(2) }}</span>
          </div>
        </div>
        <el-button
          type="primary"
          size="large"
          class="balance-confirm-btn"
          :loading="payingLoading"
          @click="handleBalancePay"
        >
          <el-icon><Check /></el-icon> 确认从账户余额立即扣款 ￥{{ Number(orderData?.totalAmount || 0).toFixed(2) }}
        </el-button>
      </div>

      <!-- 回调报文终端展示 (当沙箱调用成功后展开) -->
      <el-collapse-transition>
        <div v-if="sandboxCallbackResult" class="webhook-terminal-result">
          <div class="terminal-title">
            <el-icon color="#10b981"><CircleCheckFilled /></el-icon>
            <span>第三方网关已发送异步 Webhook 通知 · 验签入账完成</span>
          </div>
          <pre class="terminal-code"><code>{{ JSON.stringify(sandboxCallbackResult, null, 2) }}</code></pre>
        </div>
      </el-collapse-transition>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { Timer, Wallet, Iphone, Check, CircleCheckFilled } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { createPrepay, mockSandboxCallback } from '@/api/pay'

const emit = defineEmits(['pay-success', 'cancel'])

const userStore = useUserStore()
const visible = ref(false)
const orderData = ref(null)
const activeChannel = ref('ALIPAY') // 'ALIPAY' | 'WECHAT' | 'BALANCE'
const prepayInfo = ref(null)
const payingLoading = ref(false)
const sandboxCallbackResult = ref(null)

// 倒计时状态
const remainingSeconds = ref(900)
let timerInterval = null

const formattedTime = computed(() => {
  const m = Math.floor(remainingSeconds.value / 60)
  const s = remainingSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function startTimer() {
  stopTimer()
  remainingSeconds.value = 900
  timerInterval = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      stopTimer()
      ElMessage.warning('支付超时，时段锁定已释放')
      visible.value = false
    }
  }, 1000)
}

function stopTimer() {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

/**
 * 唤起收银台弹窗
 * @param {Object} order 包含 orderNo, totalAmount, venueName, bookDate, timeSlot
 */
async function open(order) {
  orderData.value = order
  sandboxCallbackResult.value = null
  visible.value = true
  activeChannel.value = 'ALIPAY'
  startTimer()

  // 发起预下单
  await initPrepay('ALIPAY')
}

async function selectChannel(channel) {
  activeChannel.value = channel
  sandboxCallbackResult.value = null
  if (channel !== 'BALANCE') {
    await initPrepay(channel)
  }
}

async function initPrepay(channel) {
  if (!orderData.value?.orderNo) return
  try {
    const res = await createPrepay({
      orderNo: orderData.value.orderNo,
      channel
    })
    prepayInfo.value = res.data
  } catch (err) {
    console.error('收银台预下单异常:', err)
  }
}

// 模拟手机微信/支付宝扣款成功 (触发网关异步 Webhook)
async function handleSimulatePay() {
  if (!orderData.value?.orderNo) return
  payingLoading.value = true
  try {
    const res = await mockSandboxCallback(orderData.value.orderNo, activeChannel.value)
    sandboxCallbackResult.value = res.data

    ElNotification({
      title: '网关异步入账成功',
      message: `${activeChannel.value === 'ALIPAY' ? '支付宝' : '微信'}支付成功！系统已为您生成专属出票核销码。`,
      type: 'success',
      duration: 3500
    })

    // 延迟 1.5 秒后关闭并通知父组件刷新
    setTimeout(() => {
      visible.value = false
      emit('pay-success', {
        orderNo: orderData.value.orderNo,
        channel: activeChannel.value,
        gatewayTradeNo: res.data?.gatewayTradeNo
      })
    }, 1200)
  } catch (err) {
    ElMessage.error(err.message || '模拟支付回调处理失败')
  } finally {
    payingLoading.value = false
  }
}

// 账户余额扣款
async function handleBalancePay() {
  if (!orderData.value?.orderNo) return
  payingLoading.value = true
  try {
    const res = await createPrepay({
      orderNo: orderData.value.orderNo,
      channel: 'BALANCE'
    })

    // 刷新前端用户信息获取最新余额
    await userStore.getUserInfo().catch(() => {})

    ElNotification({
      title: '余额扣款出票成功',
      message: `已从虚拟余额扣除 ￥${Number(orderData.value.totalAmount).toFixed(2)}，核销凭证已生成！`,
      type: 'success'
    })

    setTimeout(() => {
      visible.value = false
      emit('pay-success', {
        orderNo: orderData.value.orderNo,
        channel: 'BALANCE'
      })
    }, 800)
  } catch (err) {
    ElMessage.error(err.message || '余额支付失败')
  } finally {
    payingLoading.value = false
  }
}

function handleClose() {
  stopTimer()
  emit('cancel')
}

onBeforeUnmount(() => {
  stopTimer()
})

defineExpose({
  open
})
</script>

<style scoped>
.cashier-dialog :deep(.el-dialog__header) {
  display: none;
}

.cashier-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.cashier-wrapper {
  padding: 24px;
  background: var(--card-bg, #ffffff);
  border-radius: 12px;
}

/* 顶部标题栏 */
.cashier-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.cashier-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(79, 70, 229, 0.1);
  color: #4f46e5;
  margin-bottom: 4px;
}

.cashier-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main, #111827);
  margin: 0;
}

.countdown-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f3f4f6;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
  font-variant-numeric: tabular-nums;
}

.countdown-badge.warning-time {
  background: #fee2e2;
  color: #dc2626;
  animation: pulse 1s infinite alternate;
}

/* 订单简报 */
.order-summary-card {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 20px;
}

.summary-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #cbd5e1;
}

.order-no {
  font-size: 12px;
  color: #64748b;
  font-family: monospace;
}

.amount-tag {
  color: #e11d48;
  font-weight: 800;
}

.currency {
  font-size: 14px;
  margin-right: 2px;
}

.amount-val {
  font-size: 24px;
  letter-spacing: -0.5px;
}

.summary-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.meta-item {
  display: flex;
  flex-direction: column;
}

.meta-item .label {
  font-size: 11px;
  color: #94a3b8;
}

.meta-item .val {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 渠道选择 */
.channel-section {
  margin-bottom: 20px;
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main, #1f2937);
  margin-bottom: 10px;
}

.channel-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.channel-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.channel-item:hover {
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

.channel-item.active {
  border-color: #4f46e5;
  background: rgba(79, 70, 229, 0.04);
}

.channel-item.alipay.active {
  border-color: #1677ff;
  background: rgba(22, 119, 255, 0.05);
}

.channel-item.wechat.active {
  border-color: #07c160;
  background: rgba(7, 193, 96, 0.05);
}

.channel-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}

.bal-icon {
  color: #6366f1;
}

.channel-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.channel-info .name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.channel-info .sub {
  font-size: 10px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge-tag {
  position: absolute;
  top: -6px;
  right: -4px;
  font-size: 9px;
  font-weight: 700;
  background: #f43f5e;
  color: #ffffff;
  padding: 1px 5px;
  border-radius: 8px;
}

/* 二维码交互区 */
.qrcode-interactive-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 0 6px 0;
  background: #fafafa;
  border-radius: 10px;
  border: 1px dashed #e5e7eb;
}

.qr-frame-wrapper {
  position: relative;
  width: 170px;
  height: 170px;
  background: #ffffff;
  padding: 10px;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
}

.corner-marker {
  position: absolute;
  width: 12px;
  height: 12px;
  border-color: #4f46e5;
}

.corner-marker.top-left {
  top: 4px;
  left: 4px;
  border-top: 2.5px solid #4f46e5;
  border-left: 2.5px solid #4f46e5;
}
.corner-marker.top-right {
  top: 4px;
  right: 4px;
  border-top: 2.5px solid #4f46e5;
  border-right: 2.5px solid #4f46e5;
}
.corner-marker.bottom-left {
  bottom: 4px;
  left: 4px;
  border-bottom: 2.5px solid #4f46e5;
  border-left: 2.5px solid #4f46e5;
}
.corner-marker.bottom-right {
  bottom: 4px;
  right: 4px;
  border-bottom: 2.5px solid #4f46e5;
  border-right: 2.5px solid #4f46e5;
}

.laser-scanner-line {
  position: absolute;
  left: 8px;
  right: 8px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #ef4444, transparent);
  box-shadow: 0 0 6px rgba(239, 68, 68, 0.8);
  animation: laserScan 2.4s ease-in-out infinite alternate;
  z-index: 5;
}

@keyframes laserScan {
  0% { top: 12px; }
  100% { top: 156px; }
}

.qr-canvas {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-pattern-grid {
  position: relative;
  width: 140px;
  height: 140px;
  background: #ffffff;
}

.qr-finder {
  position: absolute;
  width: 32px;
  height: 32px;
  border: 5px solid #111827;
  border-radius: 4px;
}

.qr-finder::after {
  content: '';
  position: absolute;
  top: 4px;
  left: 4px;
  right: 4px;
  bottom: 4px;
  background: #111827;
  border-radius: 2px;
}

.qr-finder.tl { top: 0; left: 0; }
.qr-finder.tr { top: 0; right: 0; }
.qr-finder.bl { bottom: 0; left: 0; }

.qr-dots-bg {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(#1e293b 22%, transparent 25%), radial-gradient(#334155 22%, transparent 25%);
  background-size: 8px 8px;
  background-position: 0 0, 4px 4px;
  opacity: 0.85;
}

.qr-center-logo {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 13px;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  z-index: 2;
}

.qr-center-logo.alipay {
  background: #1677ff;
}

.qr-center-logo.wechat {
  background: #07c160;
}

.qr-hint-text {
  text-align: center;
  margin-top: 14px;
}

.main-hint {
  font-size: 13px;
  color: #374151;
  margin: 0 0 2px 0;
}

.sub-hint {
  font-size: 11px;
  color: #9ca3af;
  margin: 0;
}

.sandbox-action-area {
  margin-top: 16px;
  width: 90%;
  text-align: center;
}

.sandbox-pay-btn {
  width: 100%;
  height: 44px;
  font-weight: 700;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  font-size: 14px;
}

.sandbox-pay-btn:hover {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
}

.sandbox-tip {
  font-size: 11px;
  color: #6b7280;
  margin-top: 6px;
}

/* 余额支付区 */
.balance-pay-box {
  padding: 20px;
  background: #f5f3ff;
  border: 1px solid #ddd6fe;
  border-radius: 10px;
  text-align: center;
}

.balance-card {
  margin-bottom: 18px;
}

.bal-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}

.bal-diff-row {
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.bal-diff-row .arrow {
  color: #6366f1;
}

.bal-diff-row .after {
  color: #4f46e5;
  font-weight: 700;
}

.balance-confirm-btn {
  width: 100%;
  height: 44px;
  font-weight: 700;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  border: none;
}

/* Webhook 终端报文 */
.webhook-terminal-result {
  margin-top: 16px;
  background: #0f172a;
  border-radius: 8px;
  padding: 12px;
  color: #e2e8f0;
}

.terminal-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #34d399;
  margin-bottom: 8px;
}

.terminal-code {
  margin: 0;
  font-family: 'JetBrains Mono', Consolas, Monaco, monospace;
  font-size: 11px;
  color: #38bdf8;
  max-height: 120px;
  overflow-y: auto;
}
</style>
