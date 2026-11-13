<template>
  <el-dialog
    v-model="visible"
    title="场馆前台 · 快捷核销中心"
    width="480px"
    destroy-on-close
  >
    <div class="verify-modal-body">
      <div class="input-tip">请输入入场会员出示的 6 位数字核销码：</div>
      <div class="code-input-wrapper">
        <el-input
          v-model="verifyCode"
          placeholder="例如: 839201"
          maxlength="6"
          class="large-code-input"
          clearable
          @keyup.enter="handleVerify"
        >
          <template #prefix>
            <el-icon><Ticket /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="action-btn">
        <el-button
          type="primary"
          size="large"
          style="width: 100%;"
          :loading="loading"
          @click="handleVerify"
        >
          立即核销并允许入场
        </el-button>
      </div>

      <!-- 成功核销反馈卡片 -->
      <transition name="el-fade-in">
        <div v-if="verifiedOrder" class="verified-card card-shadow">
          <div class="verified-header">
            <el-icon color="#10b981" size="20"><CircleCheckFilled /></el-icon>
            <span class="verified-title">核销成功 · 允许通行</span>
          </div>
          <el-divider style="margin: 10px 0;" />
          <div class="info-row">
            <span>预约场地：</span>
            <strong>{{ verifiedOrder.venueName || '指定场地' }}</strong>
          </div>
          <div class="info-row">
            <span>预约时段：</span>
            <span>{{ verifiedOrder.bookDate }} ({{ verifiedOrder.timeSlot }})</span>
          </div>
          <div class="info-row">
            <span>联系人：</span>
            <span>{{ verifiedOrder.contactName }} ({{ verifiedOrder.contactPhone }})</span>
          </div>
          <div class="info-row">
            <span>订单编号：</span>
            <span class="code-font">{{ verifiedOrder.orderNo }}</span>
          </div>
        </div>
      </transition>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { Ticket, CircleCheckFilled } from '@element-plus/icons-vue'
import { verifyOrderByCode } from '@/api/booking'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['verified'])
const visible = ref(false)
const verifyCode = ref('')
const loading = ref(false)
const verifiedOrder = ref(null)

function open(presetCode = '') {
  verifyCode.value = presetCode
  verifiedOrder.value = null
  visible.value = true
}

async function handleVerify() {
  if (!verifyCode.value || verifyCode.value.trim().length !== 6) {
    ElMessage.warning('请输入完整的 6 位数字核销码')
    return
  }

  loading.value = true
  try {
    const res = await verifyOrderByCode(verifyCode.value.trim())
    verifiedOrder.value = res
    ElMessage.success('核销成功！用户已可正常入场使用。')
    emit('verified', res)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

defineExpose({
  open
})
</script>

<style scoped>
.verify-modal-body {
  padding: 10px 0;
}

.input-tip {
  font-size: 14px;
  color: #475569;
  margin-bottom: 12px;
}

.code-input-wrapper {
  margin-bottom: 20px;
}

:deep(.large-code-input .el-input__inner) {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 4px;
  text-align: center;
  height: 52px;
  font-family: monospace;
}

.verified-card {
  margin-top: 20px;
  padding: 16px;
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.verified-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.verified-title {
  font-weight: 700;
  color: #166534;
  font-size: 15px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
  color: #334155;
}

.code-font {
  font-family: monospace;
}
</style>
