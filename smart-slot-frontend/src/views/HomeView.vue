<template>
  <div class="home-container">
    <!-- 顶部状态指示胶囊 (Landing.love 灵感) -->
    <div class="top-status-ticker">
      <div class="ticker-pill">
        <span class="live-dot"></span>
        <span class="ticker-text"><strong>SmartSlot 实时调度中</strong>：全馆支持 13 个标准时段 · Redis 原子预占锁保障 0 冲突 0 超卖</span>
        <el-tag size="small" type="success" effect="light" class="shimmer-badge">毫秒级同步</el-tag>
      </div>
    </div>

    <!-- 核心 Bento 网格展台 (Land-book & Awwwards 灵感) -->
    <div class="bento-hero-grid">
      <!-- 1. 主推王牌场地巨卡 (占 2/3 宽度) -->
      <div class="bento-card bento-feature card-shadow" @click="$router.push('/matrix?categoryId=1')">
        <div class="feature-bg-image" style="background-image: url('https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=1200&auto=format&fit=crop&q=80');"></div>
        <div class="feature-overlay"></div>
        <div class="feature-content">
          <div class="badge-row">
            <span class="featured-chip shimmer-badge">🔥 热门主推场馆</span>
            <span class="rating-chip">★ 4.98 国际赛事级</span>
          </div>
          <h2 class="feature-title">羽毛球 1 号场 · 奥运专业防滑地胶</h2>
          <p class="feature-desc">全馆配置进口抗疲劳龙骨减震结构与 300 Lux 漫反射无眩晕球场灯光，支持小时级精准预约。</p>
          <div class="feature-footer">
            <div class="price-box">
              <span class="currency">￥</span>
              <span class="number">60.00</span>
              <span class="unit">/ 小时</span>
            </div>
            <MagneticButton type="primary" class="feature-magnetic-btn">
              即刻进入时段矩阵选场
              <el-icon><ArrowRight /></el-icon>
            </MagneticButton>
          </div>
        </div>
      </div>

      <!-- 2. 右侧 Bento 指标卡：实时场馆利用看板 (占 1/3 宽度) -->
      <div class="bento-card bento-stats card-shadow">
        <div class="stats-header">
          <span class="sub-label">实时运营状态</span>
          <span class="badge-live">LIVE</span>
        </div>
        <div class="occupancy-wrap">
          <div class="occupancy-number"><NumberTicker :value="76" /><small>%</small></div>
          <div class="occupancy-meta">
            <div class="meta-title">今日高峰预约率</div>
            <div class="meta-desc">黄金时段(18:00-21:00)紧张</div>
          </div>
        </div>
        <el-divider style="margin: 14px 0;" />
        <div class="fast-action-box">
          <div class="hint-text">💡 建议预定非高峰时段 (13:00~17:00) 可享更佳静谧体验</div>
          <el-button size="default" style="width: 100%; margin-top: 10px;" @click="$router.push('/matrix')">
            查看今日全时段看板
          </el-button>
        </div>
      </div>

      <!-- 3. 右下 Bento 特性卡：全天候硬件承诺 -->
      <div class="bento-card bento-hardware card-shadow">
        <div class="hw-item">
          <div class="hw-icon"><el-icon><Sunny /></el-icon></div>
          <div class="hw-text">
            <strong>恒温 22℃</strong>
            <span>全天候新风循环系统</span>
          </div>
        </div>
        <div class="hw-item">
          <div class="hw-icon"><el-icon><Lock /></el-icon></div>
          <div class="hw-text">
            <strong>Redis 锁预占</strong>
            <span>15分钟专属支付保护</span>
          </div>
        </div>
        <div class="hw-item">
          <div class="hw-icon"><el-icon><Key /></el-icon></div>
          <div class="hw-text">
            <strong>6位专属码</strong>
            <span>到场前台扫码秒核销</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 场馆分类导航与搜索 -->
    <div id="venue-list-anchor" class="filter-bar">
      <div class="category-pills">
        <span 
          class="pill-item" 
          :class="{ active: selectedCategoryId === null }"
          @click="selectCategory(null)"
        >
          全部场馆 ({{ venues.length }})
        </span>
        <span 
          v-for="c in categories" 
          :key="c.id" 
          class="pill-item"
          :class="{ active: selectedCategoryId === c.id }"
          @click="selectCategory(c.id)"
        >
          {{ c.name }}
        </span>
      </div>

      <div class="search-input">
        <el-input 
          v-model="keyword" 
          placeholder="搜索场馆名称、特色设施..." 
          clearable 
          @input="fetchVenues"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
    </div>

    <!-- 场地精选网格 (21st.dev / Lapa Ninja 灵感) -->
    <div v-loading="loading" class="venues-grid">
      <div 
        v-for="v in venues" 
        :key="v.id" 
        class="venue-card-item card-shadow glow-on-hover"
      >
        <div class="venue-img-wrap">
          <img :src="v.coverImage || 'https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800'" :alt="v.name" loading="lazy" />
          <div class="img-gradient"></div>
          <span class="category-chip">{{ v.categoryName }}</span>
          <span class="capacity-chip"><el-icon><User /></el-icon> 容纳{{ v.capacity }}人</span>
        </div>

        <div class="venue-info">
          <div class="venue-name-row">
            <h3 class="name">{{ v.name }}</h3>
            <div class="price">
              <span class="p-symbol">￥</span>
              <span class="p-num">{{ v.pricePerHour }}</span>
              <small>/h</small>
            </div>
          </div>

          <p class="desc">{{ v.description || '配置国际标准减震防滑地胶与专业柔光照明体系。' }}</p>

          <div class="facility-tags">
            <span 
              v-for="f in (v.facilities ? v.facilities.split(',') : ['专业防滑', '独立空调'])" 
              :key="f" 
              class="facility-pill"
            >
              {{ f }}
            </span>
          </div>

          <div class="card-footer">
            <span class="open-time">
              <el-icon><Clock /></el-icon> {{ v.openTime }} ~ {{ v.closeTime }}
            </span>
            <el-button 
              type="primary" 
              class="book-btn shimmer-badge" 
              size="small" 
              @click="$router.push(`/matrix?categoryId=${v.categoryId}`)"
            >
              时段排期预约
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowRight, Search, Clock, User, Sunny, Lock, Key } from '@element-plus/icons-vue'
import { getCategories, getVenues } from '@/api/venue'
import NumberTicker from '@/components/NumberTicker.vue'
import MagneticButton from '@/components/MagneticButton.vue'

const categories = ref([])
const venues = ref([])
const selectedCategoryId = ref(null)
const keyword = ref('')
const loading = ref(false)

async function loadInitData() {
  try {
    categories.value = await getCategories()
    await fetchVenues()
  } catch (e) {
    console.error(e)
  }
}

async function fetchVenues() {
  loading.value = true
  try {
    venues.value = await getVenues({
      categoryId: selectedCategoryId.value,
      keyword: keyword.value
    })
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function selectCategory(id) {
  selectedCategoryId.value = id
  fetchVenues()
}

onMounted(() => {
  loadInitData()
})
</script>

<style scoped>
.home-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px 24px 60px;
}

/* 顶部状态胶囊 */
.top-status-ticker {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.ticker-pill {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  padding: 8px 18px;
  border-radius: 999px;
  box-shadow: var(--shadow-sm);
}

.ticker-text {
  font-size: 13px;
  color: var(--text-main);
}

/* Bento Hero Grid (Awwwards 风格异构网格) */
.bento-hero-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  grid-template-rows: auto auto;
  gap: 20px;
  margin-bottom: 36px;
}

.bento-card {
  border-radius: 20px;
  overflow: hidden;
  position: relative;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
}

.bento-feature {
  grid-row: span 2;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 40px;
  color: #ffffff;
  cursor: pointer;
  border: none;
}

.feature-bg-image {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.bento-feature:hover .feature-bg-image {
  transform: scale(1.04);
}

.feature-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.2) 0%, rgba(15, 23, 42, 0.85) 70%, rgba(15, 23, 42, 0.98) 100%);
}

.feature-content {
  position: relative;
  z-index: 2;
}

.badge-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.featured-chip {
  background: #4f46e5;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 20px;
}

.rating-chip {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  color: #fbbf24;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 20px;
}

.feature-title {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.25;
  margin-bottom: 10px;
  letter-spacing: -0.5px;
}

.feature-desc {
  font-size: 14px;
  color: #cbd5e1;
  max-width: 580px;
  line-height: 1.6;
  margin-bottom: 24px;
}

.feature-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.price-box {
  display: flex;
  align-items: baseline;
}

.price-box .currency {
  font-size: 20px;
  font-weight: 700;
  color: #38bdf8;
}

.price-box .number {
  font-size: 36px;
  font-weight: 900;
  color: #ffffff;
  letter-spacing: -1px;
}

.price-box .unit {
  font-size: 14px;
  color: #94a3b8;
  margin-left: 4px;
}

.feature-btn {
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 700;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.4);
}

/* Bento Stats Card */
.bento-stats {
  background: var(--card-bg);
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sub-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}

.badge-live {
  font-size: 11px;
  font-weight: 800;
  color: #10b981;
  background: rgba(16, 185, 129, 0.12);
  padding: 2px 8px;
  border-radius: 10px;
}

.occupancy-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 14px;
}

.occupancy-number {
  font-size: 48px;
  font-weight: 900;
  color: var(--text-main);
  letter-spacing: -2px;
}

.occupancy-number small {
  font-size: 24px;
  color: #4f46e5;
}

.meta-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}

.meta-desc {
  font-size: 12px;
  color: #ef4444;
  margin-top: 2px;
}

.hint-text {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}

/* Bento Hardware Card */
.bento-hardware {
  background: linear-gradient(135deg, var(--card-bg-elevated) 0%, var(--card-bg) 100%);
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  gap: 12px;
}

.hw-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hw-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--card-bg);
  color: #4f46e5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.hw-text {
  display: flex;
  flex-direction: column;
}

.hw-text strong {
  font-size: 13px;
  color: var(--text-main);
}

.hw-text span {
  font-size: 11px;
  color: var(--text-muted);
}

/* 筛选工具栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
}

.category-pills {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.pill-item {
  padding: 8px 18px;
  border-radius: 999px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.pill-item:hover {
  background: var(--card-bg-elevated);
  color: var(--text-main);
  transform: translateY(-1px);
}

.pill-item.active {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.search-input {
  width: 300px;
}

/* 场地卡片网格 */
.venues-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.venue-card-item {
  border-radius: 18px;
  overflow: hidden;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.venue-card-item:hover {
  transform: translateY(-5px);
  border-color: var(--border-hover);
}

.venue-img-wrap {
  position: relative;
  height: 220px;
  overflow: hidden;
}

.venue-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.venue-card-item:hover .venue-img-wrap img {
  transform: scale(1.06);
}

.img-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0,0,0,0) 40%, rgba(15,23,42,0.6) 100%);
}

.category-chip {
  position: absolute;
  top: 14px;
  left: 14px;
  background: rgba(15, 23, 42, 0.75);
  color: #ffffff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  backdrop-filter: blur(8px);
}

.capacity-chip {
  position: absolute;
  bottom: 12px;
  left: 14px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 2;
}

.venue-info {
  padding: 22px;
}

.venue-name-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 10px;
}

.venue-name-row .name {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-main);
}

.venue-name-row .price {
  color: #4f46e5;
  font-weight: 900;
}

.venue-name-row .price .p-symbol {
  font-size: 14px;
}

.venue-name-row .price .p-num {
  font-size: 22px;
}

.venue-name-row .price small {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.desc {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.facility-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.facility-pill {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--card-bg-elevated);
  padding: 3px 8px;
  border-radius: 6px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
}

.open-time {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 4px;
}

.book-btn {
  border-radius: 8px;
  font-weight: 700;
  padding: 8px 16px;
}

@media (max-width: 900px) {
  .bento-hero-grid {
    grid-template-columns: 1fr;
  }
  .bento-feature {
    grid-row: auto;
  }
}

@media (max-width: 640px) {
  .home-container {
    padding: 12px 14px 40px;
  }
  .bento-feature {
    padding: 24px 20px;
    min-height: 320px;
  }
  .feature-title {
    font-size: 22px;
  }
  .search-input {
    width: 100%;
  }
  .venues-grid {
    grid-template-columns: 1fr;
  }
}
</style>
