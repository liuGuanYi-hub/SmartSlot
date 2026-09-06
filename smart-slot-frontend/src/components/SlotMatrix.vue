<template>
  <div class="slot-matrix-container card-shadow">
    <!-- 头部工具栏: 日期切换、图例与统计 -->
    <div class="matrix-toolbar">
      <div class="date-navigator">
        <el-button-group>
          <el-button :icon="ArrowLeft" @click="changeDate(-1)">前一天</el-button>
          <el-button @click="resetToToday" :type="isToday ? 'primary' : 'default'">
            今天 ({{ dayOfWeekText }})
          </el-button>
          <el-button @click="changeDate(1)">后一天<el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button>
        </el-button-group>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :disabled-date="disabledDate"
          @change="fetchMatrixData"
          style="width: 150px; margin-left: 12px;"
        />
      </div>

      <!-- 全网协同毫秒级广播状态胶囊 (Week 1 企业级协同亮点) -->
      <div class="live-sync-indicator" :class="{ connected: wsConnected }">
        <span class="live-ping-pulse"></span>
        <span class="live-text">{{ wsConnected ? `协同广播中 · ${onlineUsers} 人在线` : '网络同步中...' }}</span>
      </div>

      <!-- 现代状态指示图例 (Landing.love 灵感) -->
      <div class="status-legend-bar">
        <div class="legend-chip"><span class="legend-dot dot-available"></span>可选时段</div>
        <div class="legend-chip"><span class="legend-dot dot-pending"></span>15分防刷锁定</div>
        <div class="legend-chip"><span class="legend-dot dot-booked"></span>已被抢占</div>
        <div class="legend-chip"><span class="legend-dot dot-mine"></span>我的行程</div>
        <div class="legend-chip"><span class="legend-dot dot-maint"></span>场地维护</div>
      </div>
    </div>

    <!-- 运动分类快速过滤与智能场馆搜索定位 Row -->
    <div class="category-filter-row" v-if="categories.length">
      <div class="filter-left-wrap">
        <span class="filter-caption">场馆类型筛选：</span>
        <div class="pills-container">
          <button 
            class="matrix-filter-pill"
            :class="{ active: selectedCategoryId === null }"
            @click="selectCategory(null)"
          >
            全部分类
          </button>
          <button 
            v-for="c in categories" 
            :key="c.id" 
            class="matrix-filter-pill"
            :class="{ active: selectedCategoryId === c.id }"
            @click="selectCategory(c.id)"
          >
            {{ c.name }}
          </button>
        </div>
      </div>

      <!-- 快捷搜索并自动定位场馆 -->
      <div class="venue-search-locate-wrap">
        <el-select
          v-model="searchedVenueId"
          filterable
          clearable
          placeholder="🔍 搜索场馆快速定位..."
          class="locate-select"
          @change="handleSearchLocate"
        >
          <el-option
            v-for="v in allVenuesList"
            :key="v.venueId"
            :label="v.venueName"
            :value="v.venueId"
          >
            <div class="locate-option-item">
              <span class="opt-name">{{ v.venueName }}</span>
              <span class="opt-tag">{{ v.categoryName }} · ￥{{ v.pricePerHour }}/h</span>
            </div>
          </el-option>
        </el-select>
      </div>
    </div>

    <!-- 移动端手势横滑提示 -->
    <div class="mobile-swipe-tip">
      <el-icon><Right /></el-icon>
      <span>移动端支持左右手势平滑滑动查看所有场地排期</span>
    </div>

    <!-- 增加全局多场馆横向滚动提醒与快捷跳转按钮组 -->
    <div class="matrix-scroll-hint-bar" v-if="matrixData?.venues?.length > 4">
      <div class="hint-left">
        <el-icon><Right /></el-icon>
        <span>已开放 <strong>{{ matrixData.venues.length }}</strong> 个特色运动场馆，支持<strong>滚轮阻尼横滑</strong>或点击右侧按钮跳转</span>
        <span class="hint-damping-badge">✨ 滚轮阻尼已开启</span>
      </div>
      <div class="hint-right">
        <button 
          type="button"
          class="matrix-nav-btn" 
          :disabled="!canScrollLeft" 
          @click="scrollMatrix(-1)"
          title="向左滚动"
        >
          <el-icon><ArrowLeft /></el-icon>
          <span>向左</span>
        </button>
        <button 
          type="button"
          class="matrix-nav-btn btn-primary-jump" 
          :disabled="!canScrollRight" 
          @click="scrollMatrix(1)"
          title="向右跳转查看更多场馆"
        >
          <span>向右跳转更多场馆</span>
          <el-icon><ArrowRight /></el-icon>
        </button>
        <button 
          type="button"
          class="matrix-nav-btn" 
          :disabled="!canScrollRight" 
          @click="scrollToEnd"
          title="直达最右侧场馆"
        >
          <span>直达末尾</span>
        </button>
      </div>
    </div>

    <!-- 核心日历矩阵看板表格容器 (支持浮动左右跳转按钮) -->
    <div class="matrix-table-container">
      <!-- 左侧向左回退浮动按钮 (避开左侧 120px 固定时段列) -->
      <transition name="fade">
        <div 
          v-show="canScrollLeft" 
          class="matrix-edge-jump-btn left-btn" 
          @click="scrollMatrix(-1)"
          title="向左回退场馆"
        >
          <el-icon :size="16"><ArrowLeft /></el-icon>
          <span class="jump-text">向左</span>
        </div>
      </transition>

      <!-- 核心日历矩阵看板表格 -->
      <div 
        ref="scrollWrapRef"
        v-loading="loading" 
        class="matrix-grid-scroll-wrap"
        @scroll="onMatrixScroll"
      >
        <div v-if="matrixData?.venues?.length" class="matrix-board">
          <!-- 矩阵表头: 各场地信息 -->
          <div class="board-row header-row">
            <div class="board-cell time-col-header">
              <el-icon><Clock /></el-icon>
              <span>时段 \ 场地</span>
            </div>
            <div 
              v-for="v in matrixData.venues" 
              :key="v.venueId" 
              :id="'venue-header-' + v.venueId"
              class="board-cell venue-col-header"
              :class="{ 'venue-col-focused': focusedVenueId === v.venueId }"
            >
              <div class="v-name" :title="v.venueName">{{ v.venueName }}</div>
              <div class="v-tags">
                <span class="v-category-badge">{{ v.categoryName }}</span>
                <span class="v-price-badge">￥{{ v.pricePerHour }}/h</span>
              </div>
              <span v-if="focusedVenueId === v.venueId" class="focus-pulse-tag">已聚焦</span>
            </div>
          </div>

          <!-- 矩阵各时段行 (09:00 ~ 22:00) -->
          <div 
            v-for="(slotTime, sIdx) in matrixData.timeSlots" 
            :key="slotTime" 
            class="board-row data-row"
            :class="{ 'current-hour-row': isCurrentHourSlot(slotTime) }"
          >
            <!-- 左侧固定时段列 -->
            <div class="board-cell time-col-label">
              <span v-if="isCurrentHourSlot(slotTime)" class="current-indicator-dot"></span>
              <span class="time-text">{{ slotTime }}</span>
            </div>

            <!-- 各场地对应格子 -->
            <div 
              v-for="venue in matrixData.venues" 
              :key="venue.venueId + '_' + slotTime"
              class="board-cell slot-cell glow-on-hover"
              :class="[getSlotClass(venue.slots[sIdx]), { 'slot-live-flash': flashingSlotKey === (venue.venueId + '_' + slotTime), 'venue-col-focused-cell': focusedVenueId === venue.venueId }]"
              @click="handleSlotClick(venue, venue.slots[sIdx], slotTime)"
            >
              <div class="slot-inner">
                <!-- 场地维护 -->
                <template v-if="venue.venueStatus === 0 || venue.slots[sIdx]?.status === 3">
                  <span class="status-title text-maint">维护中</span>
                </template>

                <!-- 我的预约 -->
                <template v-else-if="venue.slots[sIdx]?.isMine">
                  <div class="mine-badge-wrap">
                    <span class="status-title text-mine">
                      <el-icon><Check /></el-icon> 我的预约
                    </span>
                    <span v-if="venue.slots[sIdx]?.verifyCode" class="mini-verify-code">
                      券码: {{ venue.slots[sIdx]?.verifyCode }}
                    </span>
                  </div>
                </template>

                <!-- 已被预约 (他人) -->
                <template v-else-if="venue.slots[sIdx]?.status === 2">
                  <span class="status-title text-booked">已约满</span>
                </template>

                <!-- 待支付锁定中 (Redis 锁) -->
                <template v-else-if="venue.slots[sIdx]?.status === 1">
                  <div class="pending-badge-wrap">
                    <el-icon class="lock-icon"><Lock /></el-icon>
                    <span class="status-title text-pending">时段锁定中</span>
                  </div>
                </template>

                <!-- 空闲可约 (默认) -->
                <template v-else>
                  <span class="status-title text-free">立即预约</span>
                  <span class="slot-price-hint">￥{{ venue.pricePerHour }}</span>
                </template>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-else description="暂无场地数据或当前分类下无开放场地" />
      </div>

      <!-- 右侧向右跳转浮动按钮 (对应用户红框指出的被遮挡右边缘，具备呼吸光晕) -->
      <transition name="fade">
        <div 
          v-show="canScrollRight" 
          class="matrix-edge-jump-btn right-btn" 
          @click="scrollMatrix(1)"
          title="点击向右跳转，查看后续场馆排期"
        >
          <span class="jump-text">向右查看</span>
          <el-icon :size="16"><ArrowRight /></el-icon>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ArrowLeft, ArrowRight, Clock, Check, Lock, Right } from '@element-plus/icons-vue'
import { getSlotMatrix, getCategories } from '@/api/venue'
import dayjs from 'dayjs'
import { ElMessage, ElNotification } from 'element-plus'

const emit = defineEmits(['select-slot', 'view-order'])

const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const selectedCategoryId = ref(null)
const categories = ref([])
const matrixData = ref(null)
const loading = ref(false)

const focusedVenueId = ref(null)
let focusTimeout = null

// 场馆搜索快速定位
const searchedVenueId = ref(null)

const allVenuesList = computed(() => {
  if (!matrixData.value?.venues) return []
  return matrixData.value.venues.map(v => ({
    venueId: v.venueId,
    venueName: v.venueName,
    categoryName: v.categoryName,
    pricePerHour: v.pricePerHour
  }))
})

function handleSearchLocate(venueId) {
  if (!venueId) return
  focusVenue(venueId)
}

// 多场馆横向滚动控制与边界状态
const scrollWrapRef = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(true)

// 鼠标滚轮阻尼平滑滑动手势 (Physics Damping Inertia)
let velocity = 0
let rafId = null

function onWheel(e) {
  const el = scrollWrapRef.value
  if (!el) return
  if (e.ctrlKey || e.altKey) return

  // 若当前容器不需要横向滚动，放行默认垂直滚动
  if (el.scrollWidth <= el.clientWidth) return

  // 触摸板横向滑动手势优先判断
  const isHorizontal = Math.abs(e.deltaX) > Math.abs(e.deltaY)
  const delta = isHorizontal ? e.deltaX : e.deltaY

  // 边界状态检测：到达边缘放行垂直滚动，避免死锁
  const atLeft = el.scrollLeft <= 2
  const atRight = el.scrollLeft + el.clientWidth >= el.scrollWidth - 4

  if ((delta > 0 && atRight) || (delta < 0 && atLeft)) {
    return
  }

  e.preventDefault()

  let normalized = delta
  if (e.deltaMode === 1) normalized *= 28
  else if (e.deltaMode === 2) normalized *= 360

  // 累加带阻尼系数的初速度，钳制防眩晕
  velocity += normalized * 0.75
  velocity = Math.max(-100, Math.min(100, velocity))

  if (!rafId) {
    rafId = requestAnimationFrame(inertiaStep)
  }
}

function inertiaStep() {
  const el = scrollWrapRef.value
  if (!el) {
    rafId = null
    velocity = 0
    return
  }

  if (Math.abs(velocity) < 0.25) {
    velocity = 0
    rafId = null
    checkScrollable()
    return
  }

  el.scrollLeft += velocity
  velocity *= 0.86 // 0.86 丝滑物理摩擦阻尼衰减系数
  checkScrollable()
  rafId = requestAnimationFrame(inertiaStep)
}

function checkScrollable() {
  nextTick(() => {
    const el = scrollWrapRef.value
    if (!el) return
    canScrollLeft.value = el.scrollLeft > 10
    canScrollRight.value = el.scrollWidth > el.clientWidth && (el.scrollLeft + el.clientWidth < el.scrollWidth - 10)
  })
}

function onMatrixScroll() {
  checkScrollable()
}

function scrollMatrix(direction) {
  const el = scrollWrapRef.value
  if (!el) return
  // 一次平滑跳转 3 个场馆 (约 516px) 或容器宽度的 65%
  const step = Math.max(344, Math.floor(el.clientWidth * 0.65))
  el.scrollBy({
    left: direction * step,
    behavior: 'smooth'
  })
  setTimeout(checkScrollable, 400)
}

function scrollToEnd() {
  const el = scrollWrapRef.value
  if (!el) return
  el.scrollTo({
    left: el.scrollWidth,
    behavior: 'smooth'
  })
  setTimeout(checkScrollable, 400)
}

function scrollToStart() {
  const el = scrollWrapRef.value
  if (!el) return
  el.scrollTo({
    left: 0,
    behavior: 'smooth'
  })
  setTimeout(checkScrollable, 400)
}

// WebSocket 全网实时协同状态
const wsConnected = ref(false)
const onlineUsers = ref(1)
const flashingSlotKey = ref(null)

let socket = null
let heartbeatTimer = null
let reconnectTimer = null

const isToday = computed(() => selectedDate.value === dayjs().format('YYYY-MM-DD'))

const dayOfWeekText = computed(() => {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[dayjs(selectedDate.value).day()]
})

const disabledDate = (time) => {
  return dayjs(time).isBefore(dayjs().startOf('day'))
}

function isCurrentHourSlot(slotTime) {
  if (!isToday.value) return false
  const startHour = parseInt(slotTime.split(':')[0])
  const currentHour = dayjs().hour()
  return startHour === currentHour
}

function changeDate(days) {
  const next = dayjs(selectedDate.value).add(days, 'day')
  if (next.isBefore(dayjs().startOf('day'))) {
    ElMessage.info('不能预约过去的日期')
    return
  }
  selectedDate.value = next.format('YYYY-MM-DD')
  fetchMatrixData()
}

function resetToToday() {
  selectedDate.value = dayjs().format('YYYY-MM-DD')
  fetchMatrixData()
}

function selectCategory(id) {
  selectedCategoryId.value = id
  searchedVenueId.value = null
  fetchMatrixData()
}

async function loadCategories() {
  try {
    categories.value = await getCategories()
  } catch (e) {
    console.error(e)
  }
}

async function fetchMatrixData() {
  loading.value = true
  try {
    const res = await getSlotMatrix({
      bookDate: selectedDate.value,
      categoryId: selectedCategoryId.value
    })
    matrixData.value = res
    setTimeout(checkScrollable, 300)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
    setTimeout(checkScrollable, 350)
  }
}

function getSlotClass(slot) {
  if (!slot) return 'slot-available'
  if (slot.status === 3) return 'slot-maintenance'
  if (slot.isMine) return 'slot-mine'
  if (slot.status === 2) return 'slot-booked'
  if (slot.status === 1) return 'slot-pending'
  return 'slot-available'
}

function handleSlotClick(venue, slot, timeSlot) {
  if (venue.venueStatus === 0 || slot.status === 3) {
    ElMessage.warning('该场地当前正在进行专业地胶/灯光检修维护')
    return
  }
  if (slot.isMine) {
    emit('view-order', {
      venue,
      timeSlot,
      date: selectedDate.value,
      orderNo: slot.orderNo,
      verifyCode: slot.verifyCode
    })
    return
  }
  if (slot.status === 2) {
    ElMessage.info('该时段已被他人预约，请选择其他空闲绿色时段')
    return
  }
  if (slot.status === 1) {
    ElMessage.info('该时段当前由其他用户在 Redis 中预占支付中（15分钟未付将自动释放）')
    return
  }

  // 空闲可选
  emit('select-slot', {
    venue,
    timeSlot,
    date: selectedDate.value,
    price: venue.pricePerHour
  })
}

/**
 * 初始化 WebSocket 协同长连接
 */
function initWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  const wsUrl = `${protocol}//${host}/ws/slot`

  try {
    socket = new WebSocket(wsUrl)
    socket.onopen = () => {
      wsConnected.value = true
      startHeartbeat()
    }

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        handleRealtimeEvent(data)
      } catch (err) {
        // 忽略非 JSON 心跳
      }
    }

    socket.onclose = () => {
      wsConnected.value = false
      stopHeartbeat()
      scheduleReconnect()
    }

    socket.onerror = () => {
      socket?.close()
    }
  } catch (err) {
    scheduleReconnect()
  }
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send('PING')
    }
  }, 25000)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

function scheduleReconnect() {
  if (reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    initWebSocket()
  }, 3000)
}

/**
 * 毫秒级响应后端时段变更全网广播
 */
function handleRealtimeEvent(data) {
  if (!data) return

  // 1. 在线协同人数更新
  if (data.eventType === 'ONLINE_COUNT') {
    onlineUsers.value = Math.max(1, data.onlineCount || 1)
    return
  }

  // 2. 检查变更是否属于当前查看的日历日期
  if (data.bookDate && data.bookDate === selectedDate.value) {
    if (!matrixData.value?.venues) return

    const venue = matrixData.value.venues.find(v => v.venueId === data.venueId)
    if (venue && venue.slots) {
      const slot = venue.slots.find(s => s.timeSlot === data.timeSlot)
      if (slot) {
        slot.status = data.status

        // 触发格子平滑流体呼吸闪烁动画
        const slotKey = `${venue.venueId}_${data.timeSlot}`
        flashingSlotKey.value = slotKey
        setTimeout(() => {
          if (flashingSlotKey.value === slotKey) {
            flashingSlotKey.value = null
          }
        }, 1500)

        // 协同交互微提示通知
        if (data.eventType === 'LOCK') {
          ElNotification({
            title: '时段已被锁定',
            message: data.message || `场地【${venue.venueName}】时段 ${data.timeSlot} 刚被他人锁定`,
            type: 'warning',
            duration: 3000,
            position: 'bottom-right'
          })
        } else if (data.eventType === 'TIMEOUT' || data.eventType === 'CANCEL') {
          ElNotification({
            title: '时段已释放',
            message: data.message || `时段 ${data.timeSlot} 已由系统重新释放为空闲可选`,
            type: 'success',
            duration: 3000,
            position: 'bottom-right'
          })
        }
      }
    }
  }
}

onMounted(() => {
  loadCategories()
  fetchMatrixData()
  initWebSocket()
  window.addEventListener('resize', checkScrollable)
  nextTick(() => {
    const scrollWrap = scrollWrapRef.value
    if (scrollWrap) {
      scrollWrap.addEventListener('wheel', onWheel, { passive: false })
    }
  })
  setTimeout(checkScrollable, 500)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScrollable)
  const scrollWrap = scrollWrapRef.value
  if (scrollWrap) {
    scrollWrap.removeEventListener('wheel', onWheel)
  }
  if (rafId) cancelAnimationFrame(rafId)
  stopHeartbeat()
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (socket) {
    socket.onclose = null
    socket.close()
  }
})

async function focusVenue(venueId) {
  // 1. 若当前分类过滤阻挡了该场地，先自动恢复全部分类
  const venueExists = matrixData.value?.venues?.some(v => v.venueId === venueId)
  if (!venueExists && selectedCategoryId.value !== null) {
    selectedCategoryId.value = null
    await fetchMatrixData()
  }

  focusedVenueId.value = venueId
  searchedVenueId.value = venueId

  // 2. 平滑轻微纵向滚动页面，保证日历看板进入舒适的可视区域
  const matrixContainer = document.querySelector('.slot-matrix-container')
  if (matrixContainer) {
    const rect = matrixContainer.getBoundingClientRect()
    if (rect.top < 60 || rect.top > window.innerHeight * 0.45) {
      matrixContainer.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }

  // 3. 计算并平滑横向滚动，将聚焦的场地列滚动至容器中央
  await nextTick()
  const headerEl = document.getElementById('venue-header-' + venueId)
  const scrollWrap = scrollWrapRef.value || document.querySelector('.matrix-grid-scroll-wrap')
  if (headerEl && scrollWrap) {
    const wrapRect = scrollWrap.getBoundingClientRect()
    const elRect = headerEl.getBoundingClientRect()
    const targetScrollLeft = (elRect.left - wrapRect.left) + scrollWrap.scrollLeft - (wrapRect.width / 2) + (elRect.width / 2)
    scrollWrap.scrollTo({
      left: Math.max(0, targetScrollLeft),
      behavior: 'smooth'
    })
    setTimeout(checkScrollable, 400)

    const targetVenue = matrixData.value?.venues?.find(v => v.venueId === venueId)
    if (targetVenue) {
      ElMessage.success(`已自动聚焦定位至【${targetVenue.venueName}】`)
    }
  }

  if (focusTimeout) clearTimeout(focusTimeout)
  focusTimeout = setTimeout(() => {
    // 保持高亮展示
  }, 4000)
}

defineExpose({
  fetchMatrixData,
  focusVenue,
  scrollMatrix,
  scrollToEnd,
  scrollToStart,
  focusedVenueId
})
</script>

<style scoped>
.slot-matrix-container {
  padding: 24px;
  border-radius: 20px;
  background: #ffffff;
}

.matrix-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
}

.date-navigator {
  display: flex;
  align-items: center;
}

.status-legend-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.legend-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
}

.dot-available { background: #10b981; box-shadow: 0 0 6px rgba(16, 185, 129, 0.4); }
.dot-pending { background: #f59e0b; box-shadow: 0 0 6px rgba(245, 158, 11, 0.4); }
.dot-booked { background: #ef4444; }
.dot-mine { background: #8b5cf6; box-shadow: 0 0 6px rgba(139, 92, 246, 0.5); }
.dot-maint { background: #cbd5e1; }

.category-filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
}

.filter-left-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
}

.venue-search-locate-wrap {
  display: flex;
  align-items: center;
}

.locate-select {
  width: 260px;
}

.locate-option-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.opt-name {
  font-weight: 600;
  color: var(--text-main);
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.opt-tag {
  font-size: 11px;
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  padding: 1px 6px;
  border-radius: 4px;
}

.hint-damping-badge {
  font-size: 11px;
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.25);
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  margin-left: 6px;
}

.filter-caption {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.pills-container {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.matrix-filter-pill {
  border: 1px solid var(--border-subtle);
  background: var(--card-bg-elevated);
  padding: 5px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.matrix-filter-pill:hover {
  background: var(--border-hover);
  color: var(--text-main);
}

.matrix-filter-pill.active {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
}

.mobile-swipe-tip {
  display: none;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  padding: 0 4px;
}

.matrix-scroll-hint-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  background: rgba(79, 70, 229, 0.06);
  border: 1px solid rgba(79, 70, 229, 0.16);
  border-radius: 10px;
  padding: 8px 16px;
  margin-bottom: 12px;
  font-size: 12px;
}

.hint-left {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
}

.hint-left strong {
  color: #4f46e5;
  font-weight: 700;
}

.hint-left code {
  background: rgba(79, 70, 229, 0.12);
  color: #4f46e5;
  padding: 1px 6px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 11px;
}

.hint-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.matrix-nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--border-subtle);
  background: var(--card-bg-elevated);
  color: var(--text-main);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.matrix-nav-btn:hover:not(:disabled) {
  border-color: #6366f1;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  transform: translateY(-1px);
}

.matrix-nav-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}

.matrix-nav-btn.btn-primary-jump {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: #ffffff;
  border: none;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.35);
}

.matrix-nav-btn.btn-primary-jump:hover:not(:disabled) {
  background: linear-gradient(135deg, #4338ca, #4f46e5);
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.5);
  transform: translateY(-1px) scale(1.02);
}

.matrix-table-container {
  position: relative;
  width: 100%;
}

.matrix-edge-jump-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 999px;
  cursor: pointer;
  user-select: none;
  font-size: 13px;
  font-weight: 700;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(8px);
}

.matrix-edge-jump-btn.right-btn {
  right: -8px;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: #ffffff;
  border: 2px solid rgba(255, 255, 255, 0.65);
  animation: pulseJumpBtn 2.2s infinite ease-in-out;
}

.matrix-edge-jump-btn.right-btn:hover {
  transform: translateY(-50%) scale(1.08);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.55);
  background: linear-gradient(135deg, #4338ca, #4f46e5);
}

.matrix-edge-jump-btn.left-btn {
  left: 132px; /* 避开左侧 120px 的固定时段列 */
  background: var(--card-bg-elevated);
  color: var(--text-main);
  border: 1.5px solid var(--border-subtle);
}

.matrix-edge-jump-btn.left-btn:hover {
  transform: translateY(-50%) scale(1.08);
  border-color: #6366f1;
  color: #6366f1;
  box-shadow: 0 6px 18px rgba(99, 102, 241, 0.25);
}

@keyframes pulseJumpBtn {
  0%, 100% {
    box-shadow: 0 4px 14px rgba(79, 70, 229, 0.4);
    transform: translateY(-50%) scale(1);
  }
  50% {
    box-shadow: 0 4px 22px rgba(99, 102, 241, 0.75), 0 0 0 5px rgba(99, 102, 241, 0.15);
    transform: translateY(-50%) scale(1.04);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-50%) scale(0.9);
}

.matrix-grid-scroll-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  touch-action: pan-x pan-y;
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
  background: var(--card-bg);
  position: relative;
  scroll-behavior: smooth;
}

.matrix-board {
  width: max-content;
  min-width: 100%;
  display: flex;
  flex-direction: column;
}

.board-row {
  display: flex;
  width: 100%;
}

.header-row {
  background: var(--card-bg-elevated);
  border-bottom: 2px solid var(--border-subtle);
  position: sticky;
  top: 0;
  z-index: 10;
}

.board-cell {
  padding: 10px 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-right: 1px solid var(--border-subtle);
  box-sizing: border-box;
}

.time-col-header, .time-col-label {
  width: 120px;
  min-width: 120px;
  max-width: 120px;
  flex: 0 0 120px;
  background: var(--card-bg-elevated);
  border-right: 2px solid var(--border-subtle);
  position: sticky;
  left: 0;
  z-index: 6;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
}

.time-col-header {
  flex-direction: row;
  gap: 6px;
  font-weight: 700;
  color: var(--text-main);
  font-size: 13px;
  z-index: 12;
}

.time-col-label {
  flex-direction: row;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 600;
  font-family: monospace;
}

.current-indicator-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4f46e5;
  animation: radarPulse 1.5s infinite;
}

.current-hour-row {
  background-color: rgba(99, 102, 241, 0.08) !important;
}

.venue-col-header {
  width: 172px;
  min-width: 172px;
  max-width: 172px;
  flex: 0 0 172px;
  box-sizing: border-box;
  padding: 10px 8px;
  overflow: hidden;
  text-align: center;
}

.v-name {
  font-size: 13px;
  font-weight: 800;
  color: var(--text-main);
  line-height: 1.35;
  height: 36px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  text-align: center;
  word-break: break-word;
}

.v-tags {
  margin-top: 6px;
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  width: 100%;
  overflow: hidden;
}

.v-category-badge {
  font-size: 11px;
  background: rgba(99, 102, 241, 0.15);
  color: #818cf8;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 90px;
}

.v-price-badge {
  font-size: 11px;
  color: #10b981;
  font-weight: 700;
  white-space: nowrap;
}

.data-row {
  border-bottom: 1px solid var(--border-subtle);
  transition: background 0.15s ease;
}

.slot-cell {
  width: 172px;
  min-width: 172px;
  max-width: 172px;
  flex: 0 0 172px;
  min-height: 54px;
  box-sizing: border-box;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  border-radius: 6px;
  margin: 2px;
  overflow: hidden;
}

.slot-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.status-title {
  font-size: 12px;
  font-weight: 700;
}

.slot-price-hint {
  font-size: 10px;
  opacity: 0.85;
}

/* 各状态色阶升级 */
.slot-available {
  background-color: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}
.slot-available:hover {
  background: #10b981;
  color: #ffffff;
  transform: scale(0.97);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
  border-color: #10b981;
}
.slot-available:hover .slot-price-hint {
  color: #ffffff;
}

.slot-pending {
  background-color: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
  cursor: not-allowed;
}
.pending-badge-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
}
.lock-icon {
  font-size: 12px;
}

.slot-booked {
  background-color: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
  cursor: not-allowed;
}

.slot-mine {
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
  color: #5b21b6;
  border: 1.5px solid #8b5cf6;
  box-shadow: 0 2px 6px rgba(139, 92, 246, 0.2);
}
.mini-verify-code {
  font-size: 10px;
  background: #7c3aed;
  color: #ffffff;
  padding: 1px 5px;
  border-radius: 4px;
  font-family: monospace;
}

.slot-maintenance {
  background-color: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
  border: 1px dashed #cbd5e1;
}

/* 全网实时协同胶囊与呼吸光环 (Week 1 企业级协同亮点) */
.live-sync-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 14px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 999px;
  font-size: 12px;
  color: #059669;
  font-weight: 600;
  transition: all 0.3s ease;
}
.live-sync-indicator:not(.connected) {
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.25);
  color: #dc2626;
}
.live-ping-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: pingPulse 2s infinite;
}
.live-sync-indicator:not(.connected) .live-ping-pulse {
  background-color: #ef4444;
  animation: none;
}
@keyframes pingPulse {
  0% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  }
  70% {
    box-shadow: 0 0 0 8px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}

/* 格子实时变色与呼吸微动效 (Landing.love 灵感) */
@keyframes slotFlashAnimation {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.9);
  }
  50% {
    transform: scale(1.06);
    box-shadow: 0 0 0 12px rgba(245, 158, 11, 0);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0);
  }
}
.slot-live-flash {
  animation: slotFlashAnimation 1.2s cubic-bezier(0.16, 1, 0.3, 1) forwards !important;
  z-index: 5;
  position: relative;
}

/* 深色模式下的各状态色阶自适应 */
:global(html.dark) .slot-available {
  background-color: rgba(16, 185, 129, 0.12);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.28);
}
:global(html.dark) .slot-available:hover {
  background: #10b981;
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.45);
}
:global(html.dark) .slot-pending {
  background-color: rgba(245, 158, 11, 0.12);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.28);
}
:global(html.dark) .slot-booked {
  background-color: rgba(239, 68, 68, 0.12);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.22);
}
:global(html.dark) .slot-mine {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.25) 0%, rgba(99, 102, 241, 0.25) 100%);
  color: #c4b5fd;
  border: 1.5px solid #8b5cf6;
}
:global(html.dark) .slot-maintenance {
  background-color: rgba(100, 116, 139, 0.12);
  color: #64748b;
  border: 1px dashed rgba(100, 116, 139, 0.25);
}

@media (max-width: 768px) {
  .matrix-toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .status-legend-bar {
    flex-wrap: wrap;
    gap: 8px;
  }
  .mobile-swipe-tip {
    display: flex;
  }
}

/* 场地列聚焦高亮动效 */
.venue-col-header.venue-col-focused {
  background: rgba(79, 70, 229, 0.12) !important;
  border-bottom: 2px solid #4f46e5 !important;
  box-shadow: inset 0 0 0 1.5px #4f46e5;
  position: relative;
}

.focus-pulse-tag {
  display: inline-block;
  margin-top: 4px;
  font-size: 11px;
  background: #4f46e5;
  color: #ffffff;
  padding: 1px 8px;
  border-radius: 999px;
  font-weight: 600;
  animation: focusTagPulse 1.8s infinite;
}

@keyframes focusTagPulse {
  0% { transform: scale(0.95); opacity: 0.9; }
  50% { transform: scale(1.05); opacity: 1; box-shadow: 0 0 10px rgba(79, 70, 229, 0.6); }
  100% { transform: scale(0.95); opacity: 0.9; }
}

.board-cell.venue-col-focused-cell {
  background: rgba(79, 70, 229, 0.05);
  border-left: 1.5px dashed rgba(79, 70, 229, 0.4) !important;
  border-right: 1.5px dashed rgba(79, 70, 229, 0.4) !important;
  animation: venueColPulse 2.5s ease-in-out;
}

@keyframes venueColPulse {
  0% { background: rgba(79, 70, 229, 0.22); }
  50% { background: rgba(79, 70, 229, 0.1); }
  100% { background: rgba(79, 70, 229, 0.05); }
}

:global(html.dark) .venue-col-header.venue-col-focused {
  background: rgba(99, 102, 241, 0.2) !important;
  border-bottom: 2px solid #818cf8 !important;
  box-shadow: inset 0 0 0 1.5px #818cf8;
}

:global(html.dark) .board-cell.venue-col-focused-cell {
  background: rgba(99, 102, 241, 0.08);
  border-left: 1.5px dashed rgba(129, 140, 248, 0.5) !important;
  border-right: 1.5px dashed rgba(129, 140, 248, 0.5) !important;
}
</style>
