<template>
  <div class="matrix-view-page">
    <div class="page-header">
      <div>
        <h2 class="title">场地日历时段矩阵看板</h2>
        <p class="subtitle">实时呈现各场馆全天时段占用状态，点击绿色空闲时段即可立即加锁预约</p>
      </div>
      <el-button @click="handleRefresh">
        <el-icon><Refresh /></el-icon> 刷新实时状态
      </el-button>
    </div>

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
import { ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import SlotMatrix from '@/components/SlotMatrix.vue'
import BookingDrawer from '@/components/BookingDrawer.vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const matrixRef = ref(null)
const drawerRef = ref(null)

function handleSelectSlot(slotInfo) {
  drawerRef.value?.open(slotInfo)
}

function handleViewMyOrder(slotInfo) {
  router.push('/my-bookings')
}

function handleBookingSuccess() {
  matrixRef.value?.fetchMatrixData()
}

function handleRefresh() {
  matrixRef.value?.fetchMatrixData()
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
</style>
