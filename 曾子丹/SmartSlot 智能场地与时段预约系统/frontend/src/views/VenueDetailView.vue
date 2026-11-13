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

          <!-- 4. 美团团购风真实口碑与全维度评价中心 (Meituan Authentic Reviews System) -->
          <div class="reviews-section-card card-shadow" id="reviews-section">
            <div class="reviews-header">
              <div class="header-left">
                <div class="meituan-brand-pill">
                  <span class="mt-icon">团</span>
                  <span class="mt-txt">美团 · 大众点评合作星选场馆</span>
                </div>
                <h3 class="block-title mt-title">
                  球友真实口碑与消费评价
                  <span class="verified-tag"><el-icon><CircleCheckFilled /></el-icon> 平台认证 · 100% 真实核销后评价</span>
                </h3>
                <p class="sub-caption">每一条评价均来自按约到场核销消费的真实会员，拒绝虚假刷单与刷评</p>
              </div>

              <!-- 右侧写评价操作入口 -->
              <div class="header-right">
                <el-button 
                  type="warning" 
                  size="default" 
                  round 
                  class="write-review-btn" 
                  @click="openWriteReviewDialog"
                >
                  <el-icon style="margin-right: 4px;"><EditPen /></el-icon> 写评价 · 拿积分
                </el-button>
              </div>
            </div>

            <!-- 美团风评分看板 (Meituan Rating Scoreboard Banner) -->
            <div class="meituan-scoreboard-banner">
              <!-- 左侧大分值展示 -->
              <div class="overall-score-box">
                <div class="score-value-row">
                  <span class="giant-score">{{ averageRating }}</span>
                  <span class="score-total">/ 5.0</span>
                </div>
                <div class="score-stars-row">
                  <el-rate :model-value="Number(averageRating)" disabled allow-half size="small" />
                </div>
                <div class="score-rank-tag">
                  <span>超赞 · 高于同城 98.6% 同类场馆</span>
                </div>
                <div class="good-percent-badge">
                  <span>🏆 {{ highRatingRate }}% 高分好评率</span>
                </div>
              </div>

              <!-- 中间细分打分进度条 -->
              <div class="dimension-progress-box">
                <div class="dim-item">
                  <span class="dim-label">场地环境</span>
                  <div class="dim-progress-wrap">
                    <div class="dim-bar-fill" :style="{ width: `${Math.min(100, Math.round(Number(avgEnvRating) * 20))}%` }"></div>
                  </div>
                  <span class="dim-val">{{ avgEnvRating }}</span>
                </div>
                <div class="dim-item">
                  <span class="dim-label">设施器材</span>
                  <div class="dim-progress-wrap">
                    <div class="dim-bar-fill" :style="{ width: `${Math.min(100, Math.round(Number(avgFacilityRating) * 20))}%` }"></div>
                  </div>
                  <span class="dim-val">{{ avgFacilityRating }}</span>
                </div>
                <div class="dim-item">
                  <span class="dim-label">卫生清洁</span>
                  <div class="dim-progress-wrap">
                    <div class="dim-bar-fill" style="width: 100%;"></div>
                  </div>
                  <span class="dim-val">5.0</span>
                </div>
                <div class="dim-item">
                  <span class="dim-label">服务态度</span>
                  <div class="dim-progress-wrap">
                    <div class="dim-bar-fill" :style="{ width: `${Math.min(100, Math.round(Number(avgServiceRating) * 20))}%` }"></div>
                  </div>
                  <span class="dim-val">{{ avgServiceRating }}</span>
                </div>
              </div>

              <!-- 右侧美团放心订官方权益 -->
              <div class="safe-booking-perks">
                <div class="perk-badge-title">
                  <el-icon color="#ff6600"><CircleCheckFilled /></el-icon>
                  <span>美团放心订官方保障</span>
                </div>
                <ul class="safe-perks-list">
                  <li><span class="dot">✔</span> 未消费极速退 · 资金秒级原路返还</li>
                  <li><span class="dot">✔</span> 智能物联扫码闸机 · 0 等待即扫即通</li>
                  <li><span class="dot">✔</span> 免费停车 2 小时 · 恒温淋浴更衣配套</li>
                </ul>
              </div>
            </div>

            <!-- 美团风标签快捷筛选栏 (Tag Filter Chips) -->
            <div class="meituan-filter-chips-row">
              <button 
                v-for="tag in tagOptions" 
                :key="tag.name"
                class="filter-chip-btn"
                :class="{ active: currentFilterTag === tag.name }"
                @click="currentFilterTag = tag.name"
              >
                <span>{{ tag.name }}</span>
                <span class="tag-count">({{ tag.count }})</span>
              </button>
            </div>

            <!-- 评价列表 -->
            <div v-if="filteredReviews.length" class="reviews-list">
              <div v-for="rev in filteredReviews" :key="rev.id" class="meituan-review-card">
                <!-- 用户头像与美团达人标 -->
                <div class="rev-user-header">
                  <div class="user-avatar-wrap">
                    <el-avatar :size="46" :src="rev.userAvatar || defaultAvatar" />
                    <span class="mt-level-badge">Lv.6</span>
                  </div>
                  <div class="user-meta-info">
                    <div class="name-badge-row">
                      <span class="user-name">{{ rev.userNickname || '尊享会员' }}</span>
                      <span class="vip-author-badge" v-if="rev.userRole === 'ROLE_ADMIN'">美团资深大V</span>
                      <span class="vip-author-badge" v-else>羽网先锋达人</span>
                    </div>
                    <div class="stars-sub-row">
                      <el-rate :model-value="rev.rating" disabled size="small" />
                      <span class="star-desc">{{ getRatingText(rev.rating) }}</span>
                    </div>
                  </div>
                  <div class="consume-tag-badge">
                    <el-icon><Calendar /></el-icon>
                    <span>体验项目: {{ venue?.name }}</span>
                  </div>
                </div>

                <!-- 评价标签 -->
                <div v-if="rev.tags" class="rev-tags-cloud">
                  <span v-for="t in splitTags(rev.tags)" :key="t" class="rev-tag-chip">
                    # {{ t }}
                  </span>
                </div>

                <!-- 评语文本 -->
                <div class="rev-content-text">
                  {{ rev.content }}
                </div>

                <!-- 实拍照片画廊 (支持点击大图画廊预览) -->
                <div v-if="getReviewImages(rev).length" class="rev-photos-grid">
                  <el-image
                    v-for="(img, idx) in getReviewImages(rev)"
                    :key="idx"
                    :src="img"
                    :preview-src-list="getReviewImages(rev)"
                    :initial-index="idx"
                    fit="cover"
                    loading="lazy"
                    preview-teleported
                    class="rev-photo-thumb"
                  />
                </div>

                <!-- 商家官方回复 (Meituan Merchant Reply Bubble) -->
                <div v-if="rev.merchantReply" class="merchant-reply-bubble">
                  <div class="reply-header">
                    <span class="reply-badge">场馆掌柜暖心回复</span>
                    <span class="reply-time">{{ formatTime(rev.createTime) }}</span>
                  </div>
                  <p class="reply-content">{{ rev.merchantReply }}</p>
                </div>

                <!-- 底部交互行: 发表时间与点赞有用 -->
                <div class="rev-footer-row">
                  <span class="publish-time">{{ formatTime(rev.createTime) }} · 预约核销后发表</span>
                  <div class="interaction-buttons">
                    <button 
                      class="like-btn" 
                      :class="{ liked: likedReviews[rev.id] }"
                      @click="handleLike(rev)"
                    >
                      <span class="like-icon">👍</span>
                      <span>觉得有用 ({{ rev.likes || 0 }})</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 空评价提示 -->
            <div v-else class="empty-reviews">
              <el-empty description="该筛选标签下暂无评价，快来发布第一条吧！" :image-size="100" />
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

    <!-- 美团风发表评价模态框 (Write Review Dialog) -->
    <el-dialog 
      v-model="writeReviewVisible" 
      title="写评价 · 评价场馆设施与服务" 
      width="560px" 
      align-center 
      destroy-on-close
    >
      <div class="write-review-modal-content">
        <div class="review-venue-banner" v-if="venue">
          <img :src="venue.coverImage" class="mini-venue-img" />
          <div class="mini-venue-meta">
            <span class="name">{{ venue.name }}</span>
            <span class="sub">给更多球友提供真实客观的消费打球参考</span>
          </div>
        </div>

        <el-form label-position="top" style="margin-top: 18px;">
          <!-- 总体评分 -->
          <el-form-item label="总体评价 (必选)">
            <div class="rate-large-box">
              <el-rate v-model="newReviewForm.rating" size="large" show-text :texts="['极差', '较差', '一般', '推荐', '超赞 · 强烈推荐']" />
            </div>
          </el-form-item>

          <!-- 细分维度打分 -->
          <div class="sub-ratings-grid">
            <div class="sub-rate-item">
              <span class="label">场地环境:</span>
              <el-rate v-model="newReviewForm.envRating" size="small" />
            </div>
            <div class="sub-rate-item">
              <span class="label">设施器材:</span>
              <el-rate v-model="newReviewForm.facilityRating" size="small" />
            </div>
            <div class="sub-rate-item">
              <span class="label">服务态度:</span>
              <el-rate v-model="newReviewForm.serviceRating" size="small" />
            </div>
          </div>

          <!-- 快捷印象标签点选 -->
          <el-form-item label="选择体验标签 (支持多选)">
            <div class="preset-tags-wrap">
              <span 
                v-for="item in presetImpressionTags" 
                :key="item" 
                class="tag-select-chip"
                :class="{ active: newReviewForm.selectedTags.includes(item) }"
                @click="toggleTagSelection(item)"
              >
                + {{ item }}
              </span>
            </div>
          </el-form-item>

          <!-- 评语文本 -->
          <el-form-item label="详细评语 (至少 10 个字)">
            <el-input 
              v-model="newReviewForm.content" 
              type="textarea" 
              rows="4" 
              placeholder="从灯光防眩晕、地板地胶减震、恒温空调、更衣室热水、前台服务等方面分享您的打球感受..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <!-- 实拍晒图 -->
          <el-form-item label="上传实拍图片 (支持粘贴图片链接或选择示例晒图)">
            <el-input 
              v-model="newReviewForm.imageInput" 
              placeholder="输入图片URL (以 http/https 开头，支持逗号分隔多张)"
            />
            <div class="quick-sample-photos" style="margin-top: 8px;">
              <span class="quick-tip">快捷附带实拍示例图：</span>
              <el-button 
                size="small" 
                plain 
                type="primary"
                @click="newReviewForm.imageInput = venue?.coverImage"
              >
                + 附带场馆实拍
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="writeReviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="handleSubmitReview">
          发布真实评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getVenueDetail, getVenueReviews, likeReview, addVenueReview } from '@/api/venue'
import { Back, CircleCheckFilled, InfoFilled, ChatLineRound, Check, Calendar, EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const venue = ref(null)
const reviews = ref([])
const currentPreviewImage = ref('')

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const currentFilterTag = ref('全部')
const likedReviews = reactive({})

// 细分打分计算
const avgEnvRating = computed(() => {
  if (!reviews.value.length) return '5.0'
  const list = reviews.value.map(r => r.envRating || r.rating || 5)
  return (list.reduce((a, b) => a + b, 0) / list.length).toFixed(1)
})

const avgFacilityRating = computed(() => {
  if (!reviews.value.length) return '4.9'
  const list = reviews.value.map(r => r.facilityRating || r.rating || 5)
  return (list.reduce((a, b) => a + b, 0) / list.length).toFixed(1)
})

const avgServiceRating = computed(() => {
  if (!reviews.value.length) return '4.9'
  const list = reviews.value.map(r => r.serviceRating || r.rating || 5)
  return (list.reduce((a, b) => a + b, 0) / list.length).toFixed(1)
})

const highRatingRate = computed(() => {
  if (!reviews.value.length) return '99.2'
  const goodCount = reviews.value.filter(r => (r.rating || 5) >= 4).length
  return Math.min(100, Math.max(90, Math.round((goodCount / reviews.value.length) * 100)))
})

// 提取所有标签选项与数量
const tagOptions = computed(() => {
  const total = reviews.value.length
  const good = reviews.value.filter(r => (r.rating || 5) >= 5).length
  const withPic = reviews.value.filter(r => getReviewImages(r).length > 0).length

  const map = {}
  reviews.value.forEach(r => {
    if (r.tags) {
      r.tags.split(/[,，]/).map(t => t.trim()).filter(Boolean).forEach(t => {
        map[t] = (map[t] || 0) + 1
      })
    }
  })

  const list = [
    { name: '全部', count: total },
    { name: '超赞好评', count: good },
    { name: '有图实拍', count: withPic }
  ]

  Object.keys(map).forEach(k => {
    list.push({ name: k, count: map[k] })
  })

  return list
})

// 筛选后的评价列表
const filteredReviews = computed(() => {
  if (currentFilterTag.value === '全部') {
    return reviews.value
  }
  if (currentFilterTag.value === '超赞好评') {
    return reviews.value.filter(r => (r.rating || 5) >= 5)
  }
  if (currentFilterTag.value === '有图实拍') {
    return reviews.value.filter(r => getReviewImages(r).length > 0)
  }
  return reviews.value.filter(r => {
    if (!r.tags) return false
    return r.tags.includes(currentFilterTag.value)
  })
})

function splitTags(tags) {
  if (!tags) return []
  return tags.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}

function getReviewImages(rev) {
  if (!rev || !rev.images) return []
  if (typeof rev.images === 'string') {
    return rev.images.split(',').map(s => s.trim()).filter(Boolean)
  }
  if (Array.isArray(rev.images)) return rev.images
  return []
}

function getRatingText(score) {
  if (score >= 5) return '极佳超赞'
  if (score >= 4) return '非常满意'
  if (score >= 3) return '满意良好'
  return '一般'
}

// 觉得有用/点赞
async function handleLike(rev) {
  if (likedReviews[rev.id]) {
    ElMessage.info('您已经点赞过这条评价了')
    return
  }
  likedReviews[rev.id] = true
  rev.likes = (rev.likes || 0) + 1
  try {
    await likeReview(rev.id)
    ElMessage.success('感谢您的认同，评价有用数 +1')
  } catch (e) {
    console.error(e)
  }
}

// 写评价弹窗状态
const writeReviewVisible = ref(false)
const submittingReview = ref(false)
const presetImpressionTags = ref([
  '奥运专业地胶', '挑高视野开阔', '灯光防眩晕', '空调给力恒温',
  '抓地减震护膝', '更衣室干净', '24h恒温热水', '停车方便免费', '前台热情专业'
])

const newReviewForm = reactive({
  rating: 5,
  envRating: 5,
  facilityRating: 5,
  serviceRating: 5,
  selectedTags: ['奥运专业地胶', '灯光防眩晕'],
  content: '',
  imageInput: ''
})

function openWriteReviewDialog() {
  newReviewForm.rating = 5
  newReviewForm.envRating = 5
  newReviewForm.facilityRating = 5
  newReviewForm.serviceRating = 5
  newReviewForm.selectedTags = ['奥运专业地胶', '灯光防眩晕']
  newReviewForm.content = ''
  newReviewForm.imageInput = ''
  writeReviewVisible.value = true
}

function toggleTagSelection(tag) {
  const idx = newReviewForm.selectedTags.indexOf(tag)
  if (idx >= 0) {
    newReviewForm.selectedTags.splice(idx, 1)
  } else {
    newReviewForm.selectedTags.push(tag)
  }
}

async function handleSubmitReview() {
  if (!newReviewForm.content.trim()) {
    ElMessage.warning('请填写至少 10 字真实打球评价心得')
    return
  }
  if (newReviewForm.content.trim().length < 10) {
    ElMessage.warning('评价内容过于简短，请至少输入 10 个字以帮助其他球友')
    return
  }
  submittingReview.value = true
  try {
    await addVenueReview(venue.value.id, {
      venueId: venue.value.id,
      rating: newReviewForm.rating,
      envRating: newReviewForm.envRating,
      facilityRating: newReviewForm.facilityRating,
      serviceRating: newReviewForm.serviceRating,
      tags: newReviewForm.selectedTags.join(','),
      images: newReviewForm.imageInput,
      content: newReviewForm.content
    })
    ElMessage.success('🎉 评价发布成功！已实时展示至美团口碑墙')
    writeReviewVisible.value = false
    const reviewsRes = await getVenueReviews(venue.value.id)
    reviews.value = reviewsRes || []
  } catch (e) {
    console.error(e)
  } finally {
    submittingReview.value = false
  }
}

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

/* 美团团购风全维度真实口碑墙 */
.reviews-section-card {
  background: var(--card-bg);
  border-radius: 24px;
  padding: 30px;
  border: 1px solid var(--border-subtle);
  margin-bottom: 24px;
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 22px;
  flex-wrap: wrap;
  gap: 16px;
}

.meituan-brand-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, rgba(255, 102, 0, 0.12), rgba(255, 170, 0, 0.15));
  border: 1px solid rgba(255, 102, 0, 0.3);
  padding: 3px 10px;
  border-radius: 20px;
  margin-bottom: 8px;
}

.mt-icon {
  background: #ff6600;
  color: #ffffff;
  font-size: 11px;
  font-weight: 900;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.mt-txt {
  font-size: 11.5px;
  color: #e65c00;
  font-weight: 700;
}

.mt-title {
  font-size: 19px;
  font-weight: 800;
  color: var(--text-main);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 6px;
}

.verified-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 6px;
}

.sub-caption {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

.write-review-btn {
  font-weight: 700;
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.3);
  padding: 8px 18px;
}

/* 美团评分大看板 */
.meituan-scoreboard-banner {
  display: grid;
  grid-template-columns: 200px 1fr 240px;
  gap: 24px;
  background: var(--card-bg-elevated);
  border: 1px solid var(--border-subtle);
  border-radius: 20px;
  padding: 24px 28px;
  margin-bottom: 22px;
  align-items: center;
}

@media (max-width: 960px) {
  .meituan-scoreboard-banner {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}

.overall-score-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-right: 1px solid var(--border-subtle);
  padding-right: 20px;
  text-align: center;
}

@media (max-width: 960px) {
  .overall-score-box {
    border-right: none;
    border-bottom: 1px solid var(--border-subtle);
    padding-right: 0;
    padding-bottom: 18px;
  }
}

.score-value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.giant-score {
  font-size: 46px;
  font-weight: 900;
  color: #ff6600;
  line-height: 1;
  font-family: var(--font-display, inherit);
}

.score-total {
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 600;
}

.score-stars-row {
  margin: 8px 0 6px;
}

.score-rank-tag {
  font-size: 11.5px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.good-percent-badge {
  background: rgba(255, 102, 0, 0.1);
  color: #ff6600;
  font-size: 11.5px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 20px;
  border: 1px solid rgba(255, 102, 0, 0.25);
}

/* 细分打分进度条 */
.dimension-progress-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dim-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dim-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary);
  width: 60px;
}

.dim-progress-wrap {
  flex: 1;
  height: 8px;
  background: var(--border-subtle);
  border-radius: 999px;
  overflow: hidden;
}

.dim-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff9900, #ff5500);
  border-radius: 999px;
  transition: width 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.dim-val {
  font-size: 13px;
  font-weight: 800;
  color: #ff6600;
  width: 28px;
  text-align: right;
}

/* 美团放心订小看板 */
.safe-booking-perks {
  background: var(--card-bg);
  border: 1px dashed rgba(255, 102, 0, 0.35);
  border-radius: 14px;
  padding: 16px;
}

.perk-badge-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 10px;
}

.safe-perks-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.safe-perks-list li {
  font-size: 11.5px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.safe-perks-list .dot {
  color: #10b981;
  font-weight: 800;
}

/* 标签筛选栏 */
.meituan-filter-chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 22px;
}

.filter-chip-btn {
  background: var(--card-bg-elevated);
  border: 1px solid var(--border-subtle);
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s ease;
}

.filter-chip-btn .tag-count {
  font-size: 11.5px;
  color: var(--text-muted);
}

.filter-chip-btn:hover {
  border-color: #ff6600;
  color: #ff6600;
}

.filter-chip-btn.active {
  background: #ff6600;
  border-color: #ff6600;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(255, 102, 0, 0.3);
}

.filter-chip-btn.active .tag-count {
  color: rgba(255, 255, 255, 0.85);
}

/* 评价卡片 */
.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.meituan-review-card {
  background: var(--card-bg-elevated);
  border-radius: 18px;
  padding: 22px 24px;
  border: 1px solid var(--border-subtle);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.meituan-review-card:hover {
  border-color: rgba(255, 102, 0, 0.25);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
}

.rev-user-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.user-avatar-wrap {
  position: relative;
}

.mt-level-badge {
  position: absolute;
  bottom: -4px;
  right: -4px;
  background: linear-gradient(135deg, #ff9900, #ff5500);
  color: #ffffff;
  font-size: 9.5px;
  font-weight: 800;
  padding: 1px 5px;
  border-radius: 8px;
  border: 1.5px solid var(--card-bg-elevated);
}

.user-meta-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.name-badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}

.vip-author-badge {
  font-size: 10.5px;
  background: linear-gradient(135deg, #fbbf24, #d97706);
  color: #ffffff;
  padding: 1px 7px;
  border-radius: 999px;
  font-weight: 700;
}

.stars-sub-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.star-desc {
  font-size: 12px;
  color: #ff6600;
  font-weight: 600;
}

.consume-tag-badge {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 5px;
  background: var(--pill-bg);
  padding: 4px 10px;
  border-radius: 8px;
  border: 1px solid var(--pill-border);
}

.rev-tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.rev-tag-chip {
  font-size: 12px;
  color: #ff6600;
  background: rgba(255, 102, 0, 0.08);
  padding: 2px 9px;
  border-radius: 6px;
  font-weight: 600;
}

.rev-content-text {
  font-size: 14.5px;
  color: var(--text-main);
  line-height: 1.75;
  margin-bottom: 14px;
}

.rev-photos-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.rev-photo-thumb {
  width: 130px;
  height: 96px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border-subtle);
  transition: transform 0.25s ease, filter 0.25s ease;
}

.rev-photo-thumb:hover {
  transform: scale(1.04);
  filter: brightness(1.05);
}

/* 商家掌柜回复气泡 */
.merchant-reply-bubble {
  background: var(--pill-bg);
  border-left: 3px solid #ff6600;
  border-radius: 4px 12px 12px 4px;
  padding: 12px 16px;
  margin-bottom: 14px;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.reply-badge {
  font-size: 12px;
  font-weight: 700;
  color: #ff6600;
}

.reply-time {
  font-size: 11px;
  color: var(--text-muted);
}

.reply-content {
  font-size: 13.5px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* 底部点赞交互 */
.rev-footer-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px dashed var(--border-subtle);
}

.publish-time {
  font-size: 12px;
  color: var(--text-muted);
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  padding: 5px 14px;
  border-radius: 999px;
  font-size: 12.5px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.like-btn:hover {
  border-color: #ff6600;
  color: #ff6600;
}

.like-btn.liked {
  background: rgba(255, 102, 0, 0.1);
  border-color: #ff6600;
  color: #ff6600;
  font-weight: 700;
}

/* 写评价模态框 */
.review-venue-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--card-bg-elevated);
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
}

.mini-venue-img {
  width: 64px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
}

.mini-venue-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mini-venue-meta .name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}

.mini-venue-meta .sub {
  font-size: 12px;
  color: var(--text-muted);
}

.sub-ratings-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  background: var(--card-bg-elevated);
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 18px;
  border: 1px solid var(--border-subtle);
}

.sub-rate-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sub-rate-item .label {
  font-size: 12px;
  color: var(--text-secondary);
}

.preset-tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-select-chip {
  background: var(--card-bg-elevated);
  border: 1px solid var(--border-subtle);
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12.5px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.tag-select-chip:hover {
  border-color: #ff6600;
  color: #ff6600;
}

.tag-select-chip.active {
  background: #ff6600;
  border-color: #ff6600;
  color: #ffffff;
  font-weight: 600;
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
