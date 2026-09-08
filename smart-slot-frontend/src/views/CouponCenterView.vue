<template>
  <div class="coupon-center-page">
    <div class="page-container">
      <!-- 顶部 Hero 营销横幅 (21st.dev 拟物流光) -->
      <div class="coupon-hero-banner card-shadow">
        <div class="banner-content">
          <div class="hero-tag-pill">
            <span class="sparkle">🎟️</span>
            <span>福利中心 · 每日限量特惠</span>
          </div>
          <h1 class="hero-title">会员专属神券中心</h1>
          <p class="hero-subtitle">新人首单立减、夜间黄金档满减、周末全品类折扣，一键领取，结算自动优选抵扣！</p>
          
          <div class="hero-stats-row">
            <div class="stat-bubble">
              <span class="bubble-val">￥20</span>
              <span class="bubble-lbl">新人无门槛礼</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-bubble">
              <span class="bubble-val">8.5折</span>
              <span class="bubble-lbl">周末畅玩特惠</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-bubble">
              <span class="bubble-val">0秒</span>
              <span class="bubble-lbl">领券即刻生效</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 优惠券卡片网格 -->
      <div class="coupon-grid-section" v-loading="loading">
        <div class="section-head">
          <div class="section-title-wrap">
            <h2 class="section-title">火热发放中特惠券</h2>
            <span class="section-badge">共 {{ coupons.length }} 张在发好券</span>
          </div>
          <el-button 
            type="primary" 
            plain 
            round 
            @click="$router.push('/profile?tab=coupons')"
          >
            <el-icon><Ticket /></el-icon> 查看我的卡券包
          </el-button>
        </div>

        <div class="coupon-grid">
          <div 
            v-for="c in coupons" 
            :key="c.id" 
            class="coupon-ticket-card card-shadow"
            :class="{ 'claimed-style': isClaimed(c) }"
          >
            <!-- 左侧金额与折扣区 -->
            <div class="ticket-left">
              <div class="ticket-amount">
                <template v-if="c.type === 2">
                  <span class="rate-val">{{ (c.discountRate * 10).toFixed(1) }}</span>
                  <span class="rate-unit">折</span>
                </template>
                <template v-else>
                  <span class="curr">￥</span>
                  <span class="amt-val">{{ Math.round(c.discountAmount) }}</span>
                </template>
              </div>
              <div class="ticket-type-tag">
                {{ getCouponTypeName(c.type) }}
              </div>
            </div>

            <!-- 中间虚线打孔 -->
            <div class="ticket-divider">
              <div class="notch top-notch"></div>
              <div class="dash-line"></div>
              <div class="notch bottom-notch"></div>
            </div>

            <!-- 右侧详情与操作区 -->
            <div class="ticket-right">
              <div class="ticket-header-row">
                <h3 class="ticket-name">{{ c.name }}</h3>
                <span class="usage-scope-pill">{{ c.categoryId ? '限定品类' : '全场通用' }}</span>
              </div>

              <div class="ticket-rule-desc">
                {{ c.minSpend > 0 ? `单笔消费满 ￥${c.minSpend} 可用` : '无任何使用门槛' }}
              </div>

              <div class="ticket-meta-footer">
                <div class="valid-time-hint">
                  <el-icon><Clock /></el-icon>
                  <span>领取后 {{ c.validDays }} 天有效</span>
                </div>

                <div class="claim-action">
                  <el-button 
                    v-if="isClaimed(c)" 
                    type="success" 
                    size="small" 
                    plain 
                    round
                    @click="$router.push('/matrix')"
                  >
                    已领 · 去订场
                  </el-button>
                  <el-button 
                    v-else 
                    type="primary" 
                    size="small" 
                    round 
                    class="claim-btn"
                    :loading="claimingId === c.id"
                    @click="handleClaim(c)"
                  >
                    免费领取
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 优惠券使用说明提示 -->
      <div class="coupon-faq-card card-shadow">
        <h3 class="faq-title">🎟️ 优惠券使用规则指引</h3>
        <div class="faq-grid">
          <div class="faq-item">
            <strong>1. 如何使用优惠券？</strong>
            <p>在日历时段矩阵选择场地与时段提交预约时，系统将自动推荐为您抵扣额度最高的优惠券，无需手动换算！</p>
          </div>
          <div class="faq-item">
            <strong>2. 优惠券是否可与储值余额叠加？</strong>
            <p>完全支持！优惠券先进行订单金额抵扣减免，剩余实付金额可直接使用会员虚拟账户余额或微信/支付宝完成支付。</p>
          </div>
          <div class="faq-item">
            <strong>3. 订单取消后优惠券是否退还？</strong>
            <p>退订有保障！在开场前取消预约订单时，不仅全额原路退还实付支付款项，已抵扣的优惠券也将自动退回您的卡券包。</p>
          </div>
          <div class="faq-item">
            <strong>4. 拼场约球是否可以使用优惠券？</strong>
            <p>优惠券当前适用于个人自主锁场预订，拼场搭子采用全员极低 AA 均摊制，已享受全网超低拼团单价！</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Ticket, Clock } from '@element-plus/icons-vue'
import { getClaimableCoupons, claimCoupon } from '@/api/coupon'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const claimingId = ref(null)
const coupons = ref([])

function getCouponTypeName(type) {
  switch (type) {
    case 1: return '满减神券'
    case 2: return '限时折扣'
    case 3: return '新人无门槛'
    default: return '优惠券'
  }
}

function isClaimed(c) {
  return c.description && c.description.includes('[已领取]')
}

async function loadCoupons() {
  loading.value = true
  try {
    const res = await getClaimableCoupons()
    coupons.value = res || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleClaim(c) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录会员账户')
    router.push('/login')
    return
  }

  claimingId.value = c.id
  try {
    await claimCoupon(c.id)
    ElMessage.success(`恭喜！成功领取【${c.name}】！`)
    c.description = (c.description || '') + ' [已领取]'
  } catch (e) {
    console.error(e)
  } finally {
    claimingId.value = null
  }
}

onMounted(() => {
  loadCoupons()
})
</script>

<style scoped>
.coupon-center-page {
  padding: 30px 20px 60px;
  background-color: var(--bg-primary);
  min-height: calc(100vh - 70px);
}

.page-container {
  max-width: 1180px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* Hero 横幅 */
.coupon-hero-banner {
  background: linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4338ca 100%);
  border-radius: 20px;
  padding: 40px 48px;
  color: #ffffff;
  position: relative;
  overflow: hidden;
}

.coupon-hero-banner::before {
  content: '';
  position: absolute;
  top: -50px;
  right: -50px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.25) 0%, transparent 70%);
  pointer-events: none;
}

.hero-tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 14px;
}

.hero-title {
  font-size: 32px;
  font-weight: 800;
  margin: 0 0 10px 0;
  letter-spacing: -0.5px;
}

.hero-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 28px 0;
  max-width: 680px;
  line-height: 1.6;
}

.hero-stats-row {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stat-bubble {
  display: flex;
  flex-direction: column;
}

.bubble-val {
  font-size: 24px;
  font-weight: 800;
  color: #fbbf24;
}

.bubble-lbl {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
}

/* 列表区 */
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0;
}

.section-badge {
  font-size: 12px;
  background: rgba(79, 70, 229, 0.1);
  color: #4f46e5;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 600;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 22px;
}

/* 拟物撕边票根卡片 */
.coupon-ticket-card {
  display: flex;
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid var(--border-color);
  overflow: hidden;
  transition: all 0.25s ease;
}

.coupon-ticket-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.08);
}

.ticket-left {
  width: 110px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 18px 10px;
  flex-shrink: 0;
}

.ticket-amount {
  display: flex;
  align-items: baseline;
}

.ticket-amount .curr {
  font-size: 16px;
  font-weight: 700;
}

.ticket-amount .amt-val {
  font-size: 34px;
  font-weight: 900;
  line-height: 1;
}

.ticket-amount .rate-val {
  font-size: 32px;
  font-weight: 900;
  line-height: 1;
}

.ticket-amount .rate-unit {
  font-size: 16px;
  font-weight: 700;
  margin-left: 2px;
}

.ticket-type-tag {
  font-size: 11px;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
  margin-top: 8px;
  font-weight: 600;
}

.ticket-divider {
  width: 16px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
}

.ticket-divider .notch {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--bg-primary);
  position: absolute;
}

.ticket-divider .top-notch {
  top: -8px;
}

.ticket-divider .bottom-notch {
  bottom: -8px;
}

.ticket-divider .dash-line {
  height: 80%;
  border-left: 2px dashed var(--border-color);
}

.ticket-right {
  flex: 1;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.ticket-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.ticket-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  line-height: 1.3;
}

.usage-scope-pill {
  font-size: 11px;
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  padding: 2px 7px;
  border-radius: 6px;
  white-space: nowrap;
  font-weight: 600;
}

.ticket-rule-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 8px 0 12px;
}

.ticket-meta-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.valid-time-hint {
  font-size: 11px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.claim-btn {
  font-weight: 700;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  border: none;
}

.claimed-style .ticket-left {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
}

/* FAQ */
.coupon-faq-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 28px 32px;
  border: 1px solid var(--border-color);
}

.faq-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0 0 20px 0;
}

.faq-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.faq-item strong {
  font-size: 14px;
  color: var(--text-primary);
  display: block;
  margin-bottom: 6px;
}

.faq-item p {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

@media (max-width: 768px) {
  .coupon-hero-banner {
    padding: 28px 20px;
  }
  .hero-title {
    font-size: 24px;
  }
  .faq-grid {
    grid-template-columns: 1fr;
  }
  .coupon-grid {
    grid-template-columns: 1fr;
  }
}
</style>
