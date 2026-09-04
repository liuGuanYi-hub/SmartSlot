<template>
  <div class="home-container">
    <!-- Hero Banner -->
    <div class="hero-section card-shadow">
      <div class="hero-content">
        <el-tag effect="dark" type="primary" class="hero-badge">智能时段预约 · 防冲突防超卖</el-tag>
        <h1 class="hero-title">SmartSlot 智能场馆与时段调度系统</h1>
        <p class="hero-desc">
          采用组件化日历时段矩阵，支持羽毛球、网球、篮球及会议空间分秒级精准预约与 6 位无感核销。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/matrix')">
            进入日历时段矩阵选场
            <el-icon class="el-icon--right"><Calendar /></el-icon>
          </el-button>
          <el-button size="large" @click="scrollToVenues">
            浏览场馆详情
          </el-button>
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
          全部运动场馆
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
          placeholder="搜索场地名称或设施..." 
          clearable 
          @input="fetchVenues"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
    </div>

    <!-- 场地卡片网格 -->
    <div v-loading="loading" class="venues-grid">
      <div 
        v-for="v in venues" 
        :key="v.id" 
        class="venue-card-item card-shadow"
      >
        <div class="venue-img-wrap">
          <img :src="v.coverImage || 'https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800'" :alt="v.name" />
          <span class="category-chip">{{ v.categoryName }}</span>
        </div>

        <div class="venue-info">
          <div class="venue-name-row">
            <h3 class="name">{{ v.name }}</h3>
            <span class="price">￥{{ v.pricePerHour }}<small>/小时</small></span>
          </div>

          <p class="desc">{{ v.description || '标准国际专业设施，全天候恒温与优质照明。' }}</p>

          <div class="facility-tags">
            <el-tag 
              v-for="f in (v.facilities ? v.facilities.split(',') : ['专业防滑'])" 
              :key="f" 
              size="small" 
              type="info"
            >
              {{ f }}
            </el-tag>
          </div>

          <div class="card-footer">
            <span class="open-time">
              <el-icon><Clock /></el-icon> {{ v.openTime }} - {{ v.closeTime }}
            </span>
            <el-button 
              type="primary" 
              plain 
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
import { Calendar, Search, Clock } from '@element-plus/icons-vue'
import { getCategories, getVenues } from '@/api/venue'

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

function scrollToVenues() {
  document.getElementById('venue-list-anchor')?.scrollIntoView({ behavior: 'smooth' })
}

onMounted(() => {
  loadInitData()
})
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.hero-section {
  background: linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4338ca 100%);
  border-radius: 16px;
  padding: 48px 40px;
  color: #ffffff;
  margin-bottom: 32px;
  border: none;
}

.hero-badge {
  font-size: 13px;
  margin-bottom: 12px;
  background-color: rgba(255, 255, 255, 0.2);
  border: none;
}

.hero-title {
  font-size: 36px;
  font-weight: 800;
  line-height: 1.25;
  margin-bottom: 14px;
  letter-spacing: -0.5px;
}

.hero-desc {
  font-size: 16px;
  opacity: 0.85;
  max-width: 680px;
  margin-bottom: 28px;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 16px;
}

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
  gap: 8px;
  flex-wrap: wrap;
}

.pill-item {
  padding: 8px 16px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pill-item:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.pill-item.active {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
}

.search-input {
  width: 280px;
}

.venues-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.venue-card-item {
  border-radius: 14px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  background: #ffffff;
}

.venue-card-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 20px -5px rgba(0, 0, 0, 0.1);
}

.venue-img-wrap {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.venue-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.venue-card-item:hover .venue-img-wrap img {
  transform: scale(1.05);
}

.category-chip {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(15, 23, 42, 0.75);
  color: #ffffff;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  backdrop-filter: blur(4px);
}

.venue-info {
  padding: 20px;
}

.venue-name-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}

.venue-name-row .name {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.venue-name-row .price {
  font-size: 20px;
  font-weight: 800;
  color: #4f46e5;
}

.venue-name-row .price small {
  font-size: 12px;
  color: #64748b;
  font-weight: normal;
}

.desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.facility-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.open-time {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
