<template>
  <div class="user-profile-page">
    <div class="page-container">
      <!-- 1. 21st.dev 风格全息拟物黑金会员卡 (Black Gold VIP Card) -->
      <div class="vip-card-banner card-shadow" v-loading="loading">
        <div class="vip-card-inner">
          <!-- 装饰性光晕与全息纹理 -->
          <div class="card-foil-reflection"></div>
          
          <div class="card-top-row">
            <div class="card-brand">
              <span class="brand-badge">SMARTSLOT ELITE</span>
              <span class="card-tier">尊享黑金会员</span>
            </div>
            <div class="card-chip-wrap">
              <svg viewBox="0 0 40 30" class="sim-chip-svg">
                <rect x="2" y="2" width="36" height="26" rx="4" fill="#fbbf24" opacity="0.85" />
                <path d="M 2 15 L 38 15 M 14 2 L 14 28 M 26 2 L 26 28 M 14 8 L 26 8 M 14 22 L 26 22" stroke="#92400e" stroke-width="1.2" fill="none" />
              </svg>
              <span class="nfc-icon">📶</span>
            </div>
          </div>

          <div class="card-number">
            <span>VIP</span>
            <span>8800</span>
            <span>{{ formatId(profile.id) }}</span>
            <span>2026</span>
          </div>

          <div class="card-bottom-row">
            <div class="card-holder-meta">
              <div class="holder-item">
                <span class="h-label">持卡会员 / HOLDER</span>
                <span class="h-val">{{ profile.nickname || profile.username }}</span>
              </div>
              <div class="holder-item">
                <span class="h-label">预留手机 / PHONE</span>
                <span class="h-val">{{ profile.phone ? maskPhone(profile.phone) : '未绑定' }}</span>
              </div>
              <div class="holder-item">
                <span class="h-label">信用评分 / CREDIT</span>
                <span class="h-val credit-highlight">{{ profile.creditScore || 100 }} 分 · 极好</span>
              </div>
            </div>

            <div class="card-balance-box">
              <span class="b-caption">可用账户余额 (CNY)</span>
              <div class="b-amount">
                <span class="currency">￥</span>
                <span class="num">{{ (profile.balance || 0).toFixed(2) }}</span>
              </div>
              <el-button 
                type="warning" 
                size="small" 
                round 
                class="topup-quick-btn"
                @click="activeTab = 'wallet'"
              >
                立即充值
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. 核心功能分栏 Tab 导航 -->
      <div class="profile-content-card card-shadow">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <!-- Tab 1: 我的钱包与充值 -->
          <el-tab-pane label="我的钱包与在线充值" name="wallet">
            <div class="wallet-section">
              <div class="section-intro">
                <h3 class="title">会员储值福利中心</h3>
                <p class="desc">充值享高额阶梯式赠金，全场通用抵扣，余额订场尊享 0 秒极速退款。</p>
              </div>

              <!-- 预设满赠充值面额网格 -->
              <div class="recharge-grid">
                <div 
                  v-for="plan in rechargePlans" 
                  :key="plan.amount"
                  class="plan-card"
                  :class="{ active: selectedPlan?.amount === plan.amount && !isCustomAmount }"
                  @click="selectPlan(plan)"
                >
                  <span v-if="plan.tag" class="plan-tag">{{ plan.tag }}</span>
                  <div class="plan-amount">
                    <span class="symbol">￥</span>
                    <span class="val">{{ plan.amount }}</span>
                  </div>
                  <div class="plan-bonus">
                    赠送 <strong>￥{{ plan.bonus }}</strong>
                  </div>
                  <div class="plan-total">实际到账 ￥{{ plan.amount + plan.bonus }}</div>
                </div>

                <!-- 自定义金额卡片 -->
                <div 
                  class="plan-card custom-plan-card"
                  :class="{ active: isCustomAmount }"
                  @click="isCustomAmount = true; selectedPlan = null"
                >
                  <div class="plan-amount">
                    <span class="val" style="font-size: 18px;">自定义金额</span>
                  </div>
                  <el-input-number 
                    v-model="customInputAmount" 
                    :min="10" 
                    :max="5000" 
                    :step="50"
                    size="small"
                    style="width: 120px; margin-top: 6px;"
                    @focus="isCustomAmount = true; selectedPlan = null"
                  />
                  <div class="plan-total" style="margin-top: 6px;">按梯度享满赠</div>
                </div>
              </div>

              <!-- 充值支付渠道选择 -->
              <div class="channel-selector-row">
                <span class="c-label">支付方式：</span>
                <el-radio-group v-model="rechargeChannel">
                  <el-radio-button label="ALIPAY">
                    <span>支付宝沙箱</span>
                  </el-radio-button>
                  <el-radio-button label="WECHAT">
                    <span>微信支付沙箱</span>
                  </el-radio-button>
                  <el-radio-button label="SANDBOX">
                    <span>云闪付极速通道</span>
                  </el-radio-button>
                </el-radio-group>
              </div>

              <!-- 充值结算与提交 -->
              <div class="recharge-action-bar">
                <div class="summary-meta">
                  <span>支付金额：<strong class="pay-price">￥{{ currentPayAmount }}</strong></span>
                  <span class="bonus-hint" v-if="calculatedBonus > 0">（加赠 ￥{{ calculatedBonus }}，实得 ￥{{ currentPayAmount + calculatedBonus }}）</span>
                </div>
                <el-button 
                  type="primary" 
                  size="large" 
                  :loading="recharging" 
                  class="submit-recharge-btn"
                  @click="handleRecharge"
                >
                  确认支付并充值
                </el-button>
              </div>
            </div>
          </el-tab-pane>

          <!-- Tab 2: 资金收支流水明细 -->
          <el-tab-pane label="资金收支明细台账" name="records">
            <div class="records-section">
              <div class="section-intro">
                <h3 class="title">钱包收支账单流水</h3>
                <p class="desc">包含在线储值充值、订场划扣、订单取消原路退款及管理员后台调账全记录。</p>
              </div>

              <el-table 
                :data="recordsList" 
                v-loading="loadingRecords" 
                stripe 
                style="width: 100%; border-radius: 12px; overflow: hidden;"
              >
                <el-table-column prop="tradeNo" label="流水单号" min-width="160">
                  <template #default="{ row }">
                    <span class="font-mono">{{ row.tradeNo }}</span>
                  </template>
                </el-table-column>
                
                <el-table-column label="账单业务类型" width="130">
                  <template #default="{ row }">
                    <el-tag v-if="row.orderNo && row.orderNo.startsWith('RECHARGE')" type="success" effect="plain">
                      储值充值
                    </el-tag>
                    <el-tag v-else-if="row.orderNo && row.orderNo.startsWith('ADMIN')" type="warning" effect="plain">
                      后台调账
                    </el-tag>
                    <el-tag v-else-if="row.payStatus === 3" type="info" effect="plain">
                      退订退款
                    </el-tag>
                    <el-tag v-else type="primary" effect="plain">
                      时段订场
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column prop="amount" label="变动金额" width="130">
                  <template #default="{ row }">
                    <span 
                      class="font-bold" 
                      :class="isIncome(row) ? 'text-income' : 'text-expense'"
                    >
                      {{ isIncome(row) ? '+' : '-' }}￥{{ row.amount }}
                    </span>
                  </template>
                </el-table-column>

                <el-table-column prop="channel" label="支付渠道" width="120">
                  <template #default="{ row }">
                    <el-tag size="small" :type="getChannelTagType(row.channel)">
                      {{ getChannelName(row.channel) }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column prop="buyerId" label="款项说明 / 备注" min-width="160" />

                <el-table-column prop="createTime" label="交易发生时间" width="170">
                  <template #default="{ row }">
                    {{ formatTime(row.createTime) }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <!-- Tab 3: 履约信用分体系 -->
          <el-tab-pane label="履约信用分健康度" name="credit">
            <div class="credit-section">
              <div class="credit-dashboard-grid">
                <!-- 信用分大卡 -->
                <div class="credit-meter-card card-shadow">
                  <div class="meter-score">{{ profile.creditScore || 100 }}</div>
                  <div class="meter-label">当前履约信用评级</div>
                  <div class="meter-tag">极好 · 享受全部特权</div>
                  <p class="meter-desc">良好的履约信用让您享有免押金订场、高峰期抢场豁免等多重特权。</p>
                </div>

                <!-- 履约数据统计卡 -->
                <div class="credit-stats-card card-shadow">
                  <h4 class="card-inner-title">会员历史履约大盘</h4>
                  <div class="stats-counter-grid">
                    <div class="counter-box">
                      <span class="cnt-num">{{ profile.totalBookings || 0 }}</span>
                      <span class="cnt-label">累计预约时段</span>
                    </div>
                    <div class="counter-box">
                      <span class="cnt-num text-success">{{ profile.completedBookings || 0 }}</span>
                      <span class="cnt-label">正常核销履约</span>
                    </div>
                    <div class="counter-box">
                      <span class="cnt-num text-info">{{ profile.canceledBookings || 0 }}</span>
                      <span class="cnt-label">自主提前退订</span>
                    </div>
                    <div class="counter-box">
                      <span class="cnt-num text-primary">{{ profile.activeBookings || 0 }}</span>
                      <span class="cnt-label">待到场核销</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 信用特权与规则 -->
              <div class="credit-rules-wrap">
                <h4 class="card-inner-title" style="margin-bottom: 14px;">信用分奖惩与特权规则</h4>
                <div class="rules-flex-row">
                  <div class="rule-col rule-bonus">
                    <span class="r-badge"><el-icon><Check /></el-icon> 信用加分</span>
                    <ul>
                      <li>准时到场前台扫码或道闸通行核销：<strong>+2 分 / 次</strong></li>
                      <li>完成到场体验并发表真实球友评价：<strong>+3 分 / 次</strong></li>
                      <li>账户连续 30 天无爽约记录：<strong>+5 分系统奖励</strong></li>
                    </ul>
                  </div>
                  <div class="rule-col rule-penalty">
                    <span class="r-badge badge-penalty"><el-icon><Close /></el-icon> 违约惩罚</span>
                    <ul>
                      <li>时段开场后无故不到场恶意爽约：<strong>-15 分 / 次</strong></li>
                      <li>频繁连续恶意占座锁场并超时放弃：<strong>-20 分 / 次</strong></li>
                      <li>信用分低于 70 分：<strong>限制预约周五及周末 18:00~21:00 黄金时段</strong></li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- Tab 4: 资料与安全设置 -->
          <el-tab-pane label="个人资料与安全设置" name="settings">
            <div class="settings-section">
              <div class="settings-grid">
                <!-- 修改基础信息 -->
                <div class="settings-card card-shadow">
                  <h4 class="card-inner-title">基本信息维护</h4>
                  <el-form :model="profileForm" label-position="top">
                    <el-form-item label="会员登录名">
                      <el-input :model-value="profile.username" disabled />
                    </el-form-item>
                    <el-form-item label="个性昵称">
                      <el-input v-model="profileForm.nickname" placeholder="请输入个性球友昵称" />
                    </el-form-item>
                    <el-form-item label="手机号码">
                      <el-input v-model="profileForm.phone" placeholder="11 位手机号码" maxlength="11" />
                    </el-form-item>
                    <el-button type="primary" :loading="updatingProfile" @click="handleUpdateProfile">
                      保存基础信息
                    </el-button>
                  </el-form>
                </div>

                <!-- 修改登录密码 -->
                <div class="settings-card card-shadow">
                  <h4 class="card-inner-title">修改登录密码</h4>
                  <el-form :model="pwdForm" label-position="top">
                    <el-form-item label="原登录密码">
                      <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
                    </el-form-item>
                    <el-form-item label="新登录密码">
                      <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位新密码" />
                    </el-form-item>
                    <el-form-item label="确认新密码">
                      <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                    </el-form-item>
                    <el-button type="warning" :loading="updatingPwd" @click="handleUpdatePassword">
                      确认修改密码
                    </el-button>
                  </el-form>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getUserProfile, updateUserProfile, updateUserPassword, rechargeWallet, getMyWalletRecords } from '@/api/user'
import { Check, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const activeTab = ref('wallet')
const loading = ref(false)
const profile = ref({})

// 充值档位
const rechargePlans = [
  { amount: 100, bonus: 10, tag: '新手专享' },
  { amount: 300, bonus: 45, tag: '加赠15%' },
  { amount: 500, bonus: 100, tag: '人气推荐' },
  { amount: 1000, bonus: 250, tag: '黑金加赠25%' }
]
const selectedPlan = ref(rechargePlans[1])
const isCustomAmount = ref(false)
const customInputAmount = ref(200)
const rechargeChannel = ref('ALIPAY')
const recharging = ref(false)

// 账单流水
const recordsList = ref([])
const loadingRecords = ref(false)

// 基础资料表单
const profileForm = reactive({
  nickname: '',
  phone: ''
})
const updatingProfile = ref(false)

// 密码表单
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const updatingPwd = ref(false)

const currentPayAmount = computed(() => {
  if (isCustomAmount.value) {
    return customInputAmount.value || 0
  }
  return selectedPlan.value?.amount || 0
})

const calculatedBonus = computed(() => {
  const amt = currentPayAmount.value
  if (amt >= 1000) return 250
  if (amt >= 500) return 100
  if (amt >= 300) return 45
  if (amt >= 100) return 10
  return 0
})

function selectPlan(plan) {
  isCustomAmount.value = false
  selectedPlan.value = plan
}

function formatId(id) {
  if (!id) return '0001'
  return String(id).padStart(4, '0')
}

function maskPhone(p) {
  if (!p || p.length < 11) return p
  return p.substring(0, 3) + '****' + p.substring(7)
}

function formatTime(t) {
  if (!t) return ''
  return dayjs(t).format('YYYY-MM-DD HH:mm')
}

function isIncome(row) {
  if (row.orderNo && (row.orderNo.startsWith('RECHARGE') || row.orderNo.startsWith('ADMIN'))) {
    return true
  }
  if (row.payStatus === 3) return true // 退款入账
  return false
}

function getChannelTagType(channel) {
  switch (channel) {
    case 'ALIPAY': return 'primary'
    case 'WECHAT': return 'success'
    case 'BALANCE': return 'warning'
    default: return 'info'
  }
}

function getChannelName(channel) {
  switch (channel) {
    case 'ALIPAY': return '支付宝'
    case 'WECHAT': return '微信支付'
    case 'BALANCE': return '余额抵扣'
    case 'SYSTEM': return '系统调账'
    default: return channel || '网关渠道'
  }
}

async function loadProfile() {
  loading.value = true
  try {
    const res = await getUserProfile()
    profile.value = res || {}
    profileForm.nickname = res.nickname || ''
    profileForm.phone = res.phone || ''
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  loadingRecords.value = true
  try {
    const res = await getMyWalletRecords()
    recordsList.value = res || []
  } catch (e) {
    console.error(e)
  } finally {
    loadingRecords.value = false
  }
}

async function handleRecharge() {
  if (currentPayAmount.value <= 0) {
    ElMessage.warning('请输入有效的充值金额')
    return
  }
  recharging.value = true
  try {
    const res = await rechargeWallet({
      amount: currentPayAmount.value,
      channel: rechargeChannel.value
    })
    ElMessage.success(`充值成功！赠金 ￥${calculatedBonus.value} 已自动到账，最新余额 ￥${res.balance.toFixed(2)}`)
    await loadProfile()
    await loadRecords()
  } catch (e) {
    console.error(e)
  } finally {
    recharging.value = false
  }
}

async function handleUpdateProfile() {
  if (!profileForm.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  updatingProfile.value = true
  try {
    await updateUserProfile(profileForm)
    ElMessage.success('个人资料保存成功')
    await loadProfile()
  } catch (e) {
    console.error(e)
  } finally {
    updatingProfile.value = false
  }
}

async function handleUpdatePassword() {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于 6 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  updatingPwd.value = true
  try {
    await updateUserPassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，下次登录请使用新密码')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e) {
    console.error(e)
  } finally {
    updatingPwd.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadRecords()
})
</script>

<style scoped>
.user-profile-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 80px;
}

/* 黑金会员卡 Banner */
.vip-card-banner {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #090d16 100%);
  border-radius: 24px;
  border: 1px solid rgba(251, 191, 36, 0.35);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.35);
  position: relative;
  overflow: hidden;
  margin-bottom: 28px;
}

.vip-card-inner {
  padding: 32px 36px;
  position: relative;
  z-index: 2;
}

.card-foil-reflection {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.12) 0%, transparent 60%);
  pointer-events: none;
}

.card-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.brand-badge {
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 2px;
  color: #fbbf24;
  margin-right: 12px;
}

.card-tier {
  font-size: 12px;
  background: rgba(251, 191, 36, 0.15);
  color: #fef3c7;
  border: 1px solid rgba(251, 191, 36, 0.4);
  padding: 2px 10px;
  border-radius: 20px;
}

.card-chip-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sim-chip-svg {
  width: 38px;
  height: 28px;
}

.nfc-icon {
  font-size: 18px;
  color: #fbbf24;
}

.card-number {
  display: flex;
  gap: 20px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 3px;
  color: #f1f5f9;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
  margin-bottom: 32px;
}

.card-bottom-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 20px;
}

.card-holder-meta {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.holder-item {
  display: flex;
  flex-direction: column;
}

.h-label {
  font-size: 11px;
  color: #94a3b8;
  letter-spacing: 1px;
  margin-bottom: 4px;
}

.h-val {
  font-size: 15px;
  font-weight: 600;
  color: #f8fafc;
}

.credit-highlight {
  color: #34d399 !important;
}

.card-balance-box {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(251, 191, 36, 0.3);
  padding: 14px 22px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.b-caption {
  font-size: 11px;
  color: #cbd5e1;
}

.b-amount {
  display: flex;
  align-items: baseline;
  color: #fbbf24;
  margin: 4px 0 8px;
}

.b-amount .currency {
  font-size: 16px;
  font-weight: 700;
}

.b-amount .num {
  font-size: 28px;
  font-weight: 900;
  margin-left: 2px;
}

.topup-quick-btn {
  font-weight: 700;
  background: #fbbf24;
  color: #78350f;
  border: none;
}

/* Tab 区域卡片 */
.profile-content-card {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 24px;
  border: 1px solid var(--border-subtle);
}

.section-intro {
  margin-bottom: 24px;
}

.section-intro .title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 4px;
}

.section-intro .desc {
  font-size: 13px;
  color: var(--text-muted);
}

/* 充值档位网格 */
.recharge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.plan-card {
  background: var(--card-bg-elevated);
  border: 1.5px solid var(--border-subtle);
  border-radius: 16px;
  padding: 20px 16px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.plan-card:hover {
  transform: translateY(-2px);
  border-color: #4f46e5;
}

.plan-card.active {
  border-color: #4f46e5;
  background: rgba(79, 70, 229, 0.06);
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.15);
}

.plan-tag {
  position: absolute;
  top: -10px;
  right: 12px;
  background: linear-gradient(135deg, #ef4444 0%, #f97316 100%);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
}

.plan-amount {
  display: flex;
  align-items: baseline;
  color: var(--text-main);
}

.plan-amount .symbol {
  font-size: 15px;
  font-weight: 700;
}

.plan-amount .val {
  font-size: 26px;
  font-weight: 800;
  margin-left: 2px;
}

.plan-bonus {
  font-size: 13px;
  color: #f59e0b;
  margin: 6px 0 4px;
}

.plan-total {
  font-size: 12px;
  color: var(--text-muted);
}

.custom-plan-card {
  justify-content: center;
}

/* 渠道选择 */
.channel-selector-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.c-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

/* 提交充值栏 */
.recharge-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--card-bg-elevated);
  padding: 16px 24px;
  border-radius: 16px;
  border: 1px solid var(--border-subtle);
}

.summary-meta {
  font-size: 15px;
  color: var(--text-main);
}

.pay-price {
  font-size: 24px;
  color: #4f46e5;
  margin-left: 4px;
}

.bonus-hint {
  font-size: 13px;
  color: #10b981;
}

.submit-recharge-btn {
  padding: 0 32px;
  height: 44px;
  font-weight: 700;
  border-radius: 12px;
}

/* 信用分模块 */
.credit-dashboard-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.credit-meter-card {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.08) 0%, var(--card-bg) 100%);
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 18px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.meter-score {
  font-size: 56px;
  font-weight: 900;
  color: #10b981;
  line-height: 1;
  margin-bottom: 8px;
}

.meter-label {
  font-size: 13px;
  color: var(--text-muted);
}

.meter-tag {
  font-size: 13px;
  font-weight: 700;
  color: #10b981;
  background: rgba(16, 185, 129, 0.15);
  padding: 2px 10px;
  border-radius: 20px;
  margin: 10px 0;
}

.meter-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin: 0;
}

.credit-stats-card {
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 18px;
  padding: 24px;
}

.card-inner-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 16px;
}

.stats-counter-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.counter-box {
  background: var(--card-bg-elevated);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cnt-num {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-main);
}

.cnt-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.text-success { color: #10b981 !important; }
.text-info { color: #64748b !important; }
.text-primary { color: #4f46e5 !important; }
.text-income { color: #10b981 !important; }
.text-expense { color: #ef4444 !important; }

/* 规则栏 */
.rules-flex-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.rule-col {
  padding: 18px 20px;
  border-radius: 14px;
  background: var(--card-bg-elevated);
  border: 1px solid var(--border-subtle);
}

.r-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #10b981;
  background: rgba(16, 185, 129, 0.12);
  padding: 3px 10px;
  border-radius: 20px;
  margin-bottom: 12px;
}

.badge-penalty {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.12);
}

.rule-col ul {
  padding-left: 18px;
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.8;
}

/* 安全设置 */
.settings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.settings-card {
  background: var(--card-bg-elevated);
  padding: 24px;
  border-radius: 16px;
  border: 1px solid var(--border-subtle);
}

@media (max-width: 900px) {
  .card-bottom-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .credit-dashboard-grid, .rules-flex-row, .settings-grid {
    grid-template-columns: 1fr;
  }
  .stats-counter-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
