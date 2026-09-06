<template>
  <div class="venue-detail-page">
    <!-- 顶部面包屑与导航 -->
    <div class="nav-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/matrix' }">时段看板</el-breadcrumb-item>
        <el-breadcrumb-item>{{ venue?.name || '场地实景详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
      <el-button size="small" plain @click="router.back()">
        <el-icon><Back /></el-icon> 返回上一页
      </el-button>
    </div>

    <!-- 加载与主内容 -->
    <div v-loading="loading" class="detail-container">
      <div v-if="venue" class="detail-layout">
        <!-- 左侧/主要区域: 视觉巨幕 + 专业规格 + 口碑评价 -->
        <div class="main-content-col">
          <!-- 1. 实景沉浸图赏 -->
          <div class="hero-gallery-card card-shadow">
            <div class="main-image-wrap">
              <img :src="currentPreviewImage || venue.coverImage" :alt="venue.name" class="main-img" @error="handleImageError" />
              <div class="image-badges-overlay">
                <span class="category-pill">{{ venue.categoryName || '专业运动场馆' }}</span>
                <span class="status-pill status-open">
                  <span class="green-dot"></span> 开放预约中 ({{ venue.openTime }} - {{ venue.closeTime }})
                </span>
              </div>
            </div>

            <!-- 实景细节缩略图切换 -->
            <div class="gallery-thumbs-row">
              <div 
                v-for="(img, idx) in galleryImages" 
                :key="idx"
                class="thumb-item"
                :class="{ active: currentPreviewImage === img }"
                @click="currentPreviewImage = img"
              >
                <img :src="img" :alt="'实景视角 ' + (idx + 1)" />
              </div>
            </div>
          </div>

          <!-- 2. 场地介绍与硬核规格 Bento -->
          <div class="specs-section-card card-shadow">
            <div class="section-title-row">
              <div class="title-left">
                <h1 class="venue-title">{{ venue.name }}</h1>
                <p class="venue-desc">{{ venue.description || '国际标准专业场馆，配备恒温防眩光系统与专业减震地胶。' }}</p>
              </div>
              <div class="title-price-badge">
                <span class="price-symbol">￥</span>
                <span class="price-val">{{ venue.pricePerHour }}</span>
                <span class="price-unit">/ 小时</span>
              </div>
            </div>

            <div class="specs-bento-grid">
              <div class="bento-box">
                <div class="bento-icon">📐</div>
                <div class="bento-meta">
                  <span class="bento-label">场地规格 & 挑高</span>
                  <span class="bento-value">赛事标准 · 净高 9.5m</span>
                </div>
              </div>
              <div class="bento-box">
                <div class="bento-icon">🛡️</div>
                <div class="bento-meta">
                  <span class="bento-label">地表缓冲系统</span>
                  <span class="bento-value">奥运级防滑地胶 + 减震龙骨</span>
                </div>
              </div>
              <div class="bento-box">
                <div class="bento-icon">💡</div>
                <div class="bento-meta">
                  <span class="bento-label">专业照明照度</span>
                  <span class="bento-value">650 Lux 眩光抑制悬浮天灯</span>
                </div>
              </div>
              <div class="bento-box">
                <div class="bento-icon">❄️</div>
                <div class="bento-meta">
                  <span class="bento-label">温控与新风</span>
                  <span class="bento-value">24℃ 恒温恒湿 · 变频循环</span>
                </div>
              </div>
            </div>

            <!-- 配套设施清单 -->
            <div class="facilities-block">
              <h3 class="block-title">配套硬件与智慧设施</h3>
              <div class="facility-tags-wrap">
                <div v-for="fac in facilityList" :key="fac" class="facility-chip">
                  <el-icon class="chip-icon"><CircleCheckFilled /></el-icon>
                  <span>{{ fac }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 3. 入馆须知与取消退订规则 -->
          <div class="rules-card card-shadow">
            <h3 class="block-title">
              <el-icon><InfoFilled /></el-icon> 入馆须知与保障规则
            </h3>
            <div class="rules-grid">
              <div class="rule-item">
                <span class="rule-num">01</span>
                <div class="rule-text">
                  <strong>专业运动装备要求</strong>
                  <p>为保护实木地板与专业地胶，入场人员须穿着浅色/非黑底专业防滑运动球鞋，严禁皮鞋或高跟鞋进场。</p>
                </div>
              </div>
              <div class="rule-item">
                <span class="rule-num">02</span>
                <div class="rule-text">
                  <strong>无接触扫码秒核销</strong>
                  <p>预约成功后可在【我的预约】查看专属 6 位数字票据或二维码，在现场前台或智能道闸处扫码即可通行放行。</p>
                </div>
              </div>
              <div class="rule-item">
                <span class="rule-num">03</span>
                <div class="rule-text">
                  <strong>开场前自主无损退订</strong>
                  <p>若行程临时变更，可在时段开场前自主申请取消，费用将即刻全额原路退回至您的账户余额，同时释放库存。</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 4. 真实球友口碑墙 (Authentic Reviews) -->
          <div class="reviews-section-card card-shadow">
            <div class="reviews-header">
              <div>
                <h3 class="block-title" style="margin-bottom: 4px;">
                  <el-icon><ChatLineRound /></el-icon> 球友真实口碑与体验评价
                </h3>
                <p class="sub-caption">每一条评价均来自真实完成到场核销打球的会员</p>
              </div>
              <div class="score-summary-badge" v-if="reviews.length">
                <span class="score-num">{{ averageRating }}</span>
                <div class="score-meta">
                  <el-rate :model-value="Number(averageRating)" disabled text-color="#ff9900" size="small" />
                  <span class="review-count">基于 {{ reviews.length }} 条真实核销体验</span>
                </div>
              </div>
            </div>

            <!-- 评价列表 -->
            <div v-if="reviews.length" class="reviews-list">
              <div v-for="rev in reviews" :key="rev.id" class="review-item-card">
                <div class="rev-user-header">
                  <div class="user-meta">
                    <el-avatar :size="40" :src="rev.userAvatar || defaultAvatar" />
                    <div class="user-info">
                      <span class="user-name">{{ rev.userNickname || '尊享会员' }}</span>
                      <span class="rev-time">{{ formatTime(rev.createTime) }}</span>
                    </div>
                  </div>
                  <el-rate :model-value="rev.rating" disabled size="small" />
                </div>
                <div class="rev-content">
                  {{ rev.content }}
                </div>
              </div>
            </div>

            <!-- 空评价提示 -->
            <div v-else class="empty-reviews">
              <el-empty description="当前场地暂无历史评语，欢迎预约体验并留下您的首发精彩评价！" :image-size="100" />
            </div>
          </div>
        </div>

        <!-- 右侧/侧边栏: 快速预订引导卡片 (Sticky) -->
        <div class="sidebar-col">
          <div class="booking-cta-card card-shadow">
            <div class="cta-header">
              <span class="cta-title">预约时段看板</span>
              <span class="cta-badge">实时联动</span>
            </div>
            
            <div class="price-row">
              <span class="p-num">￥{{ venue.pricePerHour }}</span>
              <span class="p-unit">/ 小时 (含空调与灯光)</span>
            </div>

            <div class="cta-perks">
              <div class="perk-item">
                <el-icon color="#10b981"><Check /></el-icon>
                <span>全网毫秒级防超卖预占锁</span>
              </div>
              <div class="perk-item">
                <el-icon color="#10b981"><Check /></el-icon>
                <span>支持支付宝 / 微信 / 余额秒级退款</span>
              </div>
              <div class="perk-item">
                <el-icon color="#10b981"><Check /></el-icon>
                <span>智能物联网道闸凭码即放行</span>
              </div>
            </div>

            <el-divider style="margin: 16px 0;" />

            <div class="venue-quick-info">
              <div class="info-row">
                <span class="label">场馆容纳：</span>
                <span class="val">{{ venue.capacity || 4 }} 人以内</span>
              </div>
              <div class="info-row">
                <span class="label">营业时段：</span>
                <span class="val">{{ venue.openTime }} - {{ venue.closeTime }}</span>
              </div>
              <div class="info-row">
                <span class="label">当前状态：</span>
                <span class="val text-success">正常开放接单</span>
              </div>
            </div>

            <el-button 
              type="primary" 
              size="large" 
              class="go-matrix-btn" 
              @click="goToMatrix"
            >
              <el-icon><Calendar /></el-icon> 前往矩阵挑选时段
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getVenueDetail, getVenueReviews } from '@/api/venue'
import { Back, CircleCheckFilled, InfoFilled, ChatLineRound, Check, Calendar } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const venue = ref(null)
const reviews = ref([])
const currentPreviewImage = ref('')

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const galleryImages = computed(() => {
  if (!venue.value) return []
  const base = venue.value.coverImage
  return [
    base,
    'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60',
    'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800&auto=format&fit=crop&q=60',
    'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800&auto=format&fit=crop&q=60'
  ]
})

const facilityList = computed(() => {
  if (!venue.value?.facilities) return ['专业防滑地胶', '变频恒温空调', '防眩高悬吊灯', '电子储物柜', '热水淋浴', '智能门禁扫码放行']
  return venue.value.facilities.split(/[,，]/).map(s => s.trim()).filter(Boolean)
})

const averageRating = computed(() => {
  if (!reviews.value.length) return '5.0'
  const total = reviews.value.reduce((acc, cur) => acc + (cur.rating || 5), 0)
  return (total / reviews.value.length).toFixed(1)
})

function formatTime(t) {
  if (!t) return ''
  return dayjs(t).format('YYYY-MM-DD HH:mm')
}

function handleImageError(e) {
  e.target.src = 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=60'
}

function goToMatrix() {
  if (venue.value) {
    router.push({
      path: '/matrix',
      query: { focusVenueId: venue.value.id }
    })
  } else {
    router.push('/matrix')
  }
}

async function loadData() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    const [detailRes, reviewsRes] = await Promise.all([
      getVenueDetail(id),
      getVenueReviews(id)
    ])
    venue.value = detailRes
    reviews.value = reviewsRes || []
    currentPreviewImage.value = detailRes.coverImage
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.venue-detail-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 20px 80px;
}

.nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
  align-items: start;
}

/* 巨幕相册卡片 */
.hero-gallery-card {
  background: var(--card-bg);
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  margin-bottom: 24px;
}

.main-image-wrap {
  position: relative;
  width: 100%;
  height: 420px;
  background: #0f172a;
}

.main-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease;
}

.image-badges-overlay {
  position: absolute;
  bottom: 16px;
  left: 16px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.category-pill {
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(8px);
  color: #ffffff;
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.status-pill {
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(8px);
  color: #ffffff;
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.green-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.gallery-thumbs-row {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--card-bg-elevated);
}

.thumb-item {
  width: 80px;
  height: 56px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s ease;
  opacity: 0.7;
}

.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-item:hover, .thumb-item.active {
  border-color: #4f46e5;
  opacity: 1;
  transform: translateY(-2px);
}

/* 规格与介绍卡片 */
.specs-section-card {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 28px;
  border: 1px solid var(--border-subtle);
  margin-bottom: 24px;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 24px;
}

.venue-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 8px;
}

.venue-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.title-price-badge {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #ffffff;
  padding: 10px 18px;
  border-radius: 14px;
  white-space: nowrap;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3);
}

.price-symbol {
  font-size: 16px;
  font-weight: 700;
}

.price-val {
  font-size: 28px;
  font-weight: 800;
  margin-left: 2px;
}

.price-unit {
  font-size: 12px;
  opacity: 0.9;
  margin-left: 4px;
}

/* Bento 规格参数 */
.specs-bento-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.bento-box {
  background: var(--card-bg-elevated);
  border: 1px solid var(--border-subtle);
  padding: 16px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.bento-icon {
  font-size: 24px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--card-bg);
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
}

.bento-meta {
  display: flex;
  flex-direction: column;
}

.bento-label {
  font-size: 12px;
  color: var(--text-muted);
}

.bento-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
  margin-top: 2px;
}

/* 设施清单 */
.facilities-block {
  margin-top: 20px;
}

.block-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.facility-tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.facility-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--pill-bg);
  border: 1px solid var(--pill-border);
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--pill-text);
  font-weight: 500;
}

.chip-icon {
  color: #10b981;
}

/* 须知卡片 */
.rules-card {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 24px;
  border: 1px solid var(--border-subtle);
  margin-bottom: 24px;
}

.rules-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rule-item {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 12px 16px;
  background: var(--card-bg-elevated);
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
}

.rule-num {
  font-size: 18px;
  font-weight: 800;
  color: #4f46e5;
  line-height: 1;
  padding-top: 2px;
}

.rule-text strong {
  font-size: 14px;
  color: var(--text-main);
  display: block;
  margin-bottom: 4px;
}

.rule-text p {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin: 0;
}

/* 口碑墙 */
.reviews-section-card {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 28px;
  border: 1px solid var(--border-subtle);
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 14px;
}

.sub-caption {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

.score-summary-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--card-bg-elevated);
  padding: 8px 16px;
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
}

.score-num {
  font-size: 28px;
  font-weight: 800;
  color: #f59e0b;
}

.score-meta {
  display: flex;
  flex-direction: column;
}

.review-count {
  font-size: 11px;
  color: var(--text-muted);
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-item-card {
  background: var(--card-bg-elevated);
  padding: 16px 20px;
  border-radius: 14px;
  border: 1px solid var(--border-subtle);
}

.rev-user-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.rev-time {
  font-size: 11px;
  color: var(--text-muted);
}

.rev-content {
  font-size: 13.5px;
  color: var(--text-secondary);
  line-height: 1.6;
}

/* 侧边栏 CTA */
.sidebar-col {
  position: sticky;
  top: 24px;
}

.booking-cta-card {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 24px;
  border: 1px solid var(--border-subtle);
}

.cta-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.cta-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
}

.cta-badge {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 16px;
}

.p-num {
  font-size: 32px;
  font-weight: 800;
  color: #4f46e5;
}

.p-unit {
  font-size: 13px;
  color: var(--text-muted);
  margin-left: 6px;
}

.cta-perks {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.perk-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--text-secondary);
}

.venue-quick-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
}

.info-row .label {
  color: var(--text-muted);
}

.info-row .val {
  color: var(--text-main);
  font-weight: 500;
}

.text-success {
  color: #10b981 !important;
}

.go-matrix-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 700;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  border: none;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.35);
  transition: all 0.2s ease;
}

.go-matrix-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.45);
}

@media (max-width: 992px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .sidebar-col {
    position: static;
  }
}
</style>
