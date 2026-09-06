<template>
  <div class="venue-floor-plan-card card-shadow">
    <div class="floor-plan-header">
      <div class="header-left">
        <div class="radar-tag">
          <span class="pulse-ring"></span>
          <span class="tag-text">2.5D 场馆全景交互俯视图</span>
        </div>
        <span class="header-sub">点击具体场地可快速聚焦定位时段网格；悬停查看实时硬件设施与容纳人数</span>
      </div>
      <div class="header-right">
        <el-tag effect="plain" type="info" round class="status-tip">
          绿色：今日空闲充足 · 橙色：余量紧俏
        </el-tag>
      </div>
    </div>

    <!-- 交互式 SVG 场馆立体平面图 -->
    <div class="svg-map-wrapper">
      <svg 
        viewBox="0 0 960 360" 
        class="floor-plan-svg"
        xmlns="http://www.w3.org/2000/svg"
      >
        <!-- 场馆背景外廓与高精质感地胶网格 -->
        <defs>
          <pattern id="floor-grid" width="20" height="20" patternUnits="userSpaceOnUse">
            <path d="M 20 0 L 0 0 0 20" fill="none" stroke="rgba(226, 232, 240, 0.6)" stroke-width="0.75" />
          </pattern>
          <linearGradient id="badmintonGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#ecfdf5" />
            <stop offset="100%" stop-color="#d1fae5" />
          </linearGradient>
          <linearGradient id="tennisGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#eff6ff" />
            <stop offset="100%" stop-color="#dbeafe" />
          </linearGradient>
          <linearGradient id="basketGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#fffbeb" />
            <stop offset="100%" stop-color="#fef3c7" />
          </linearGradient>
          <linearGradient id="roomGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#f5f3ff" />
            <stop offset="100%" stop-color="#ede9fe" />
          </linearGradient>
          <filter id="glow-hover" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="6" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>

        <!-- 场馆地板基底 -->
        <rect x="10" y="10" width="940" height="340" rx="16" fill="#f8fafc" stroke="#e2e8f0" stroke-width="1.5" />
        <rect x="10" y="10" width="940" height="340" rx="16" fill="url(#floor-grid)" />

        <!-- 场馆主过道与指示 -->
        <path d="M 30 180 L 930 180" stroke="#cbd5e1" stroke-width="2" stroke-dasharray="6 6" />
        <text x="480" y="175" text-anchor="middle" font-size="10" fill="#94a3b8" letter-spacing="4">MAIN CONCOURSE · 中央动线主过道</text>

        <!-- 1. 羽毛球 1 号场 (专业地胶) id:1 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 1 }"
          @mouseenter="hoveredVenueId = 1"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(1)"
        >
          <rect x="35" y="30" width="130" height="130" rx="12" fill="url(#badmintonGrad)" stroke="#10b981" stroke-width="1.5" />
          <!-- 羽毛球标准球场白线 -->
          <rect x="45" y="40" width="110" height="110" fill="none" stroke="rgba(16, 185, 129, 0.4)" stroke-width="1.2" />
          <line x1="45" y1="95" x2="155" y2="95" stroke="#10b981" stroke-width="1.8" stroke-dasharray="4 2" />
          <circle cx="100" cy="95" r="4" fill="#10b981" />
          <text x="100" y="65" text-anchor="middle" font-size="12" font-weight="700" fill="#065f46">羽毛球 1 号场</text>
          <text x="100" y="130" text-anchor="middle" font-size="10" fill="#047857">专业地胶 · ￥60/h</text>
        </g>

        <!-- 2. 羽毛球 2 号场 (双打标准) id:2 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 2 }"
          @mouseenter="hoveredVenueId = 2"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(2)"
        >
          <rect x="180" y="30" width="130" height="130" rx="12" fill="url(#badmintonGrad)" stroke="#10b981" stroke-width="1.5" />
          <rect x="190" y="40" width="110" height="110" fill="none" stroke="rgba(16, 185, 129, 0.4)" stroke-width="1.2" />
          <line x1="190" y1="95" x2="300" y2="95" stroke="#10b981" stroke-width="1.8" stroke-dasharray="4 2" />
          <circle cx="245" cy="95" r="4" fill="#10b981" />
          <text x="245" y="65" text-anchor="middle" font-size="12" font-weight="700" fill="#065f46">羽毛球 2 号场</text>
          <text x="245" y="130" text-anchor="middle" font-size="10" fill="#047857">双打标准 · ￥50/h</text>
        </g>

        <!-- 3. 羽毛球 3 号场 (训练场) id:3 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 3 }"
          @mouseenter="hoveredVenueId = 3"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(3)"
        >
          <rect x="325" y="30" width="130" height="130" rx="12" fill="url(#badmintonGrad)" stroke="#10b981" stroke-width="1.5" />
          <rect x="335" y="40" width="110" height="110" fill="none" stroke="rgba(16, 185, 129, 0.4)" stroke-width="1.2" />
          <line x1="335" y1="95" x2="445" y2="95" stroke="#10b981" stroke-width="1.8" stroke-dasharray="4 2" />
          <circle cx="390" cy="95" r="4" fill="#10b981" />
          <text x="390" y="65" text-anchor="middle" font-size="12" font-weight="700" fill="#065f46">羽毛球 3 号场</text>
          <text x="390" y="130" text-anchor="middle" font-size="10" fill="#047857">发球机配备 · ￥50/h</text>
        </g>

        <!-- 4. 网球 1 号场 (硬地) id:4 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 4 }"
          @mouseenter="hoveredVenueId = 4"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(4)"
        >
          <rect x="475" y="30" width="220" height="130" rx="12" fill="url(#tennisGrad)" stroke="#3b82f6" stroke-width="1.5" />
          <!-- 网球场中线与发球线 -->
          <rect x="490" y="42" width="190" height="106" fill="none" stroke="rgba(59, 130, 246, 0.4)" stroke-width="1.2" />
          <line x1="585" y1="42" x2="585" y2="148" stroke="#3b82f6" stroke-width="2" />
          <line x1="535" y1="95" x2="635" y2="95" stroke="#3b82f6" stroke-width="1" />
          <text x="585" y="70" text-anchor="middle" font-size="13" font-weight="700" fill="#1e3a8a">网球 1 号场 (硬地赛事级)</text>
          <text x="585" y="125" text-anchor="middle" font-size="10" fill="#2563eb">全天候夜场照明 · ￥120/h</text>
        </g>

        <!-- 5. 篮球全场/半场 A (木地板) id:5 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 5 }"
          @mouseenter="hoveredVenueId = 5"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(5)"
        >
          <rect x="35" y="200" width="420" height="130" rx="12" fill="url(#basketGrad)" stroke="#f59e0b" stroke-width="1.5" />
          <!-- 篮球场三分线与三秒区 -->
          <rect x="50" y="210" width="390" height="110" fill="none" stroke="rgba(245, 158, 11, 0.4)" stroke-width="1.2" />
          <circle cx="245" cy="265" r="26" fill="none" stroke="#f59e0b" stroke-width="1.2" />
          <path d="M 50 240 A 25 25 0 0 1 50 290 Z" fill="none" stroke="#f59e0b" stroke-width="1.2" />
          <path d="M 440 240 A 25 25 0 0 0 440 290 Z" fill="none" stroke="#f59e0b" stroke-width="1.2" />
          <text x="245" y="250" text-anchor="middle" font-size="13" font-weight="700" fill="#78350f">篮球半场 A (NBA 级实木地板)</text>
          <text x="245" y="295" text-anchor="middle" font-size="10" fill="#b45309">专业更衣淋浴 · ￥80/h</text>
        </g>

        <!-- 6. 智能多功能会议室 (20人) id:6 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 6 }"
          @mouseenter="hoveredVenueId = 6"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(6)"
        >
          <rect x="475" y="200" width="450" height="130" rx="12" fill="url(#roomGrad)" stroke="#8b5cf6" stroke-width="1.5" />
          <circle cx="700" cy="265" r="32" fill="none" stroke="#8b5cf6" stroke-width="1.2" stroke-dasharray="4 2" />
          <rect x="650" y="250" width="100" height="30" rx="6" fill="#8b5cf6" opacity="0.15" />
          <text x="700" y="245" text-anchor="middle" font-size="13" font-weight="700" fill="#4c1d95">智能多功能会议室 (20人)</text>
          <text x="700" y="295" text-anchor="middle" font-size="10" fill="#6d28d9">4K双屏投影 · 远程视频会议 · ￥150/h</text>
        </g>

        <!-- 右上角场馆服务中心前台 -->
        <g class="reception-box">
          <rect x="715" y="30" width="210" height="130" rx="12" fill="#ffffff" stroke="#94a3b8" stroke-width="1.2" stroke-dasharray="3 3" />
          <text x="820" y="65" text-anchor="middle" font-size="12" font-weight="700" fill="#334155">SERVICES · 综合服务中心</text>
          <text x="820" y="90" text-anchor="middle" font-size="10" fill="#64748b">前台核销入场 · 运动饮品站</text>
          <text x="820" y="110" text-anchor="middle" font-size="10" fill="#64748b">急救箱 · 护具器械租借</text>
          <text x="820" y="135" text-anchor="middle" font-size="9" fill="#059669" font-weight="600">● 7x24小时智能值守</text>
        </g>
      </svg>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['select-venue'])
const hoveredVenueId = ref(null)

function selectVenue(id) {
  emit('select-venue', id)
}
</script>

<style scoped>
.venue-floor-plan-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 20px 24px;
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.floor-plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.radar-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid #86efac;
}

.pulse-ring {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: radarPulse 2s infinite;
}

@keyframes radarPulse {
  0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
  100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.tag-text {
  font-size: 13px;
  font-weight: 700;
  color: #065f46;
}

.header-sub {
  margin-left: 12px;
  font-size: 12px;
  color: #64748b;
}

.svg-map-wrapper {
  width: 100%;
  overflow-x: auto;
  border-radius: 12px;
  background: #f8fafc;
}

.floor-plan-svg {
  width: 100%;
  min-width: 780px;
  height: auto;
  display: block;
}

.court-group {
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: center;
}

.court-group:hover rect:first-child {
  stroke-width: 2.5px;
  filter: drop-shadow(0 6px 14px rgba(0, 0, 0, 0.08));
  transform: translateY(-2px);
}

.court-group.active rect:first-child {
  stroke-width: 3px;
}
</style>
