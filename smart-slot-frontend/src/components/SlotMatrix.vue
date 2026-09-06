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

    <!-- 运动分类快速过滤 Pills -->
    <div class="category-filter-row" v-if="categories.length">
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

    <!-- 移动端手势横滑提示 -->
    <div class="mobile-swipe-tip">
      <el-icon><Right /></el-icon>
      <span>移动端支持左右手势平滑滑动查看所有场地排期</span>
    </div>

    <!-- 核心日历矩阵看板表格 -->
    <div v-loading="loading" class="matrix-grid-scroll-wrap">
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
            class="board-cell venue-col-header"
          >
            <div class="v-name" :title="v.venueName">{{ v.venueName }}</div>
            <div class="v-tags">
              <span class="v-category-badge">{{ v.categoryName }}</span>
              <span class="v-price-badge">￥{{ v.pricePerHour }}/h</span>
            </div>
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
            :class="[getSlotClass(venue.slots[sIdx]), { 'slot-live-flash': flashingSlotKey === (venue.venueId + '_' + slotTime) }]"
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
                    码: {{ venue.slots[sIdx].verifyCode }}
                  </span>
                </div>
              </template>

              <!-- 已被预约 -->
              <template v-else-if="venue.slots[sIdx]?.status === 2">
                <span class="status-title text-booked">已售出</span>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
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
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
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
})

onUnmounted(() => {
  stopHeartbeat()
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (socket) {
    socket.onclose = null
    socket.close()
  }
})

defineExpose({
  fetchMatrixData
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
  gap: 12px;
  margin-bottom: 20px;
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

.matrix-grid-scroll-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  touch-action: pan-x pan-y;
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
  background: var(--card-bg);
}

.matrix-board {
  min-width: 820px;
  display: flex;
  flex-direction: column;
}

.board-row {
  display: flex;
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
}

.time-col-header, .time-col-label {
  width: 136px;
  min-width: 136px;
  background: var(--card-bg-elevated);
  border-right: 2px solid var(--border-subtle);
  position: sticky;
  left: 0;
  z-index: 5;
}

.time-col-header {
  flex-direction: row;
  gap: 6px;
  font-weight: 700;
  color: var(--text-main);
  font-size: 13px;
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
  flex: 1;
  min-width: 130px;
}

.v-name {
  font-size: 14px;
  font-weight: 800;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

.v-tags {
  margin-top: 4px;
  display: flex;
  gap: 6px;
  align-items: center;
}

.v-category-badge {
  font-size: 11px;
  background: rgba(99, 102, 241, 0.15);
  color: #818cf8;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
}

.v-price-badge {
  font-size: 11px;
  color: #10b981;
  font-weight: 700;
}

.data-row {
  border-bottom: 1px solid var(--border-subtle);
  transition: background 0.15s ease;
}

.slot-cell {
  flex: 1;
  min-width: 130px;
  min-height: 52px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  border-radius: 4px;
  margin: 2px;
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
</style>
