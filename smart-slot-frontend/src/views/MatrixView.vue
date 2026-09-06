<template>
  <div class="matrix-view-page">
    <div class="page-header">
      <div>
        <h2 class="title">场地日历时段矩阵看板</h2>
        <p class="subtitle">实时呈现各场馆全天时段占用状态，点击绿色空闲时段即可立即加锁预约</p>
      </div>
      <div class="header-actions">
        <MagneticButton type="outline" @click="showFloorPlan = !showFloorPlan">
          <el-icon><MapLocation /></el-icon> {{ showFloorPlan ? '收起全景俯视图' : '展开 2.5D 场馆俯视图' }}
        </MagneticButton>
        <MagneticButton type="primary" @click="handleRefresh" style="margin-left: 12px;">
          <el-icon><Refresh /></el-icon> 刷新实时状态
        </MagneticButton>
      </div>
    </div>

    <!-- Week 2 亮点: 场馆 2.5D 立体平面交互选区地图 -->
    <transition name="fade-slide">
      <VenueFloorPlan 
        v-if="showFloorPlan" 
        :selected-venue-id="activeVenueId"
        @select-venue="handleSelectVenue"
      />
    </transition>

    <!-- 核心组件: 日历时段网格矩阵 -->
    <SlotMatrix 
      ref="matrixRef"
      @select-slot="handleSelectSlot"
      @view-order="handleViewMyOrder"
    />

    <!-- 核心组件: 预约核对与时段锁定抽屉 -->
    <BookingDrawer 
      ref="drawerRef"
      @success="handleBookingSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh, MapLocation } from '@element-plus/icons-vue'
import SlotMatrix from '@/components/SlotMatrix.vue'
import BookingDrawer from '@/components/BookingDrawer.vue'
import VenueFloorPlan from '@/components/VenueFloorPlan.vue'
import MagneticButton from '@/components/MagneticButton.vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const matrixRef = ref(null)
const drawerRef = ref(null)
const showFloorPlan = ref(true)
const activeVenueId = ref(null)

onMounted(() => {
  if (route.query.focusVenueId) {
    const targetId = Number(route.query.focusVenueId)
    setTimeout(() => {
      activeVenueId.value = targetId
      matrixRef.value?.focusVenue(targetId)
    }, 350)
  }
})

function handleSelectSlot(slotInfo) {
  drawerRef.value?.open(slotInfo)
}

function handleSelectVenue(venueId) {
  activeVenueId.value = venueId
  matrixRef.value?.focusVenue(venueId)
  ElMessage.success(`已联动定位至下方时段矩阵【场地 #${venueId}】`)
}

function handleViewMyOrder(slotInfo) {
  router.push('/my-bookings')
}

function handleBookingSuccess() {
  matrixRef.value?.fetchMatrixData()
}

function handleRefresh() {
  matrixRef.value?.fetchMatrixData()
  ElMessage.success('已刷新最新时段占用与协同状态')
}
</script>

<style scoped>
.matrix-view-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.header-actions {
  display: flex;
  align-items: center;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
</style>
