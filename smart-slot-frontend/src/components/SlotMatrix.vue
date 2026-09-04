<template>
  <div class="slot-matrix-container card-shadow">
    <!-- 头部工具栏: 日期切换与分类筛选 -->
    <div class="matrix-header">
      <div class="date-controls">
        <el-button-group>
          <el-button :icon="ArrowLeft" @click="changeDate(-1)" size="default">前一天</el-button>
          <el-button @click="resetToToday" size="default" :type="isToday ? 'primary' : 'default'">今天</el-button>
          <el-button @click="changeDate(1)" size="default">后一天<el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button>
        </el-button-group>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择预约日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :disabled-date="disabledDate"
          @change="fetchMatrixData"
          style="width: 160px; margin-left: 12px;"
        />
      </div>

      <!-- 图例说明 -->
      <div class="status-legend">
        <div class="legend-item"><span class="badge available"></span>空闲可选</div>
        <div class="legend-item"><span class="badge pending"></span>锁定中</div>
        <div class="legend-item"><span class="badge booked"></span>已被预约</div>
        <div class="legend-item"><span class="badge mine"></span>我的预约</div>
        <div class="legend-item"><span class="badge maintenance"></span>维护停用</div>
      </div>
    </div>

    <!-- 场地分类过滤标签 -->
    <div class="category-tabs" v-if="categories.length">
      <span class="tab-label">运动分类：</span>
      <el-radio-group v-model="selectedCategoryId" @change="fetchMatrixData" size="small">
        <el-radio-button :value="null">全部分类</el-radio-button>
        <el-radio-button v-for="c in categories" :key="c.id" :value="c.id">
          {{ c.name }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 核心矩阵网格区域 -->
    <div v-loading="loading" class="matrix-grid-wrapper">
      <div v-if="matrixData?.venues?.length" class="matrix-table">
        <!-- 表头：第一格为时段，后续为各场地 -->
        <div class="matrix-row header-row">
          <div class="matrix-cell time-header">时段 / 场地</div>
          <div 
            v-for="v in matrixData.venues" 
            :key="v.venueId" 
            class="matrix-cell venue-header"
          >
            <div class="venue-name" :title="v.venueName">{{ v.venueName }}</div>
            <div class="venue-sub">
              <span class="category-tag">{{ v.categoryName }}</span>
              <span class="price-tag">￥{{ v.pricePerHour }}/h</span>
            </div>
          </div>
        </div>

        <!-- 表体：每行对应一个时段 (09:00~22:00) -->
        <div 
          v-for="(slotTime, sIdx) in matrixData.timeSlots" 
          :key="slotTime" 
          class="matrix-row body-row"
        >
          <!-- 左侧时段时间标签 -->
          <div class="matrix-cell time-label">
            <el-icon><Clock /></el-icon>
            <span>{{ slotTime }}</span>
          </div>

          <!-- 各场地的该时段格子 -->
          <div 
            v-for="venue in matrixData.venues" 
            :key="venue.venueId + '_' + slotTime"
            class="matrix-cell slot-cell"
            :class="getSlotClass(venue.slots[sIdx])"
            @click="handleSlotClick(venue, venue.slots[sIdx], slotTime)"
          >
            <div class="slot-content">
              <template v-if="venue.venueStatus === 0 || venue.slots[sIdx]?.status === 3">
                <span class="text-status">维护</span>
              </template>
              <template v-else-if="venue.slots[sIdx]?.isMine">
                <span class="text-status mine-text">
                  <el-icon><Check /></el-icon> 我的
                </span>
                <span v-if="venue.slots[sIdx]?.verifyCode" class="verify-code-badge">
                  码:{{ venue.slots[sIdx].verifyCode }}
                </span>
              </template>
              <template v-else-if="venue.slots[sIdx]?.status === 2">
                <span class="text-status">已约</span>
              </template>
              <template v-else-if="venue.slots[sIdx]?.status === 1">
                <span class="text-status pending-text">
                  <el-icon><Lock /></el-icon> 锁定时段
                </span>
              </template>
              <template v-else>
                <span class="text-status free-text">可预约</span>
                <span class="price-mini">￥{{ venue.pricePerHour }}</span>
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
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, ArrowRight, Clock, Check, Lock } from '@element-plus/icons-vue'
import { getSlotMatrix, getCategories } from '@/api/venue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['select-slot', 'view-order'])

const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const selectedCategoryId = ref(null)
const categories = ref([])
const matrixData = ref(null)
const loading = ref(false)

const isToday = computed(() => selectedDate.value === dayjs().format('YYYY-MM-DD'))

const disabledDate = (time) => {
  return dayjs(time).isBefore(dayjs().startOf('day'))
}

function changeDate(days) {
  const next = dayjs(selectedDate.value).add(days, 'day')
  if (next.isBefore(dayjs().startOf('day'))) {
    ElMessage.info('不能查看过去的日期')
    return
  }
  selectedDate.value = next.format('YYYY-MM-DD')
  fetchMatrixData()
}

function resetToToday() {
  selectedDate.value = dayjs().format('YYYY-MM-DD')
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
    ElMessage.warning('该场地此时间段正在进行检修维护，暂不可预约')
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
    ElMessage.info('该时段已被他人预约')
    return
  }
  if (slot.status === 1) {
    ElMessage.info('该时段当前正在被其他用户下单锁定中（15分钟超时将自动释放）')
    return
  }

  // 可预约状态
  emit('select-slot', {
    venue,
    timeSlot,
    date: selectedDate.value,
    price: venue.pricePerHour
  })
}

onMounted(() => {
  loadCategories()
  fetchMatrixData()
})

defineExpose({
  fetchMatrixData
})
</script>

<style scoped>
.slot-matrix-container {
  padding: 20px;
  border-radius: 14px;
}

.matrix-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.date-controls {
  display: flex;
  align-items: center;
}

.status-legend {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
  color: #475569;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.badge {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}

.badge.available { background-color: #10b981; }
.badge.pending { background-color: #f59e0b; }
.badge.booked { background-color: #ef4444; }
.badge.mine { background-color: #8b5cf6; }
.badge.maintenance { background-color: #94a3b8; }

.category-tabs {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.matrix-grid-wrapper {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
}

.matrix-table {
  min-width: 780px;
  display: flex;
  flex-direction: column;
}

.matrix-row {
  display: flex;
}

.header-row {
  background: #f8fafc;
  border-bottom: 2px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 2;
}

.matrix-cell {
  padding: 10px 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-right: 1px solid #f1f5f9;
}

.time-header, .time-label {
  width: 130px;
  min-width: 130px;
  background: #f8fafc;
  font-weight: 600;
  color: #334155;
  border-right: 2px solid #e2e8f0;
}

.time-label {
  flex-direction: row;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

.venue-header {
  flex: 1;
  min-width: 120px;
}

.venue-name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 130px;
}

.venue-sub {
  margin-top: 4px;
  display: flex;
  gap: 6px;
  align-items: center;
}

.category-tag {
  font-size: 11px;
  background: #e0e7ff;
  color: #4338ca;
  padding: 1px 5px;
  border-radius: 4px;
}

.price-tag {
  font-size: 11px;
  color: #059669;
  font-weight: 600;
}

.body-row {
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.15s ease;
}

.body-row:hover {
  background-color: #fafafa;
}

.slot-cell {
  flex: 1;
  min-width: 120px;
  min-height: 48px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.slot-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.text-status {
  font-size: 12px;
  font-weight: 600;
}

.price-mini {
  font-size: 11px;
  opacity: 0.8;
}

/* 状态色块定制 */
.slot-available {
  background-color: #ecfdf5;
  color: #065f46;
}
.slot-available:hover {
  background-color: #10b981;
  color: #ffffff;
  transform: scale(0.98);
  border-radius: 6px;
}
.slot-available:hover .price-mini {
  color: #ffffff;
}

.slot-pending {
  background-color: #fffbeb;
  color: #b45309;
  cursor: not-allowed;
}

.slot-booked {
  background-color: #fef2f2;
  color: #b91c1c;
  cursor: not-allowed;
}

.slot-mine {
  background-color: #f5f3ff;
  color: #6d28d9;
  border: 1.5px solid #8b5cf6;
}
.slot-mine:hover {
  background-color: #ede9fe;
}
.verify-code-badge {
  font-size: 10px;
  background: #7c3aed;
  color: #ffffff;
  padding: 1px 4px;
  border-radius: 3px;
}

.slot-maintenance {
  background-color: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
}
</style>
