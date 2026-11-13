<template>
  <button 
    ref="btnRef"
    class="magnetic-btn-wrap"
    :class="[typeClass, { 'btn-disabled': disabled }]"
    :disabled="disabled"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
    @click="$emit('click', $event)"
  >
    <span class="magnetic-glow-bg"></span>
    <span class="magnetic-content" :style="contentStyle">
      <slot></slot>
    </span>
  </button>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'primary' // 'primary', 'gradient', 'outline'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  strength: {
    type: Number,
    default: 0.35 // 磁吸吸引力度
  }
})

defineEmits(['click'])

const btnRef = ref(null)
const offsetX = ref(0)
const offsetY = ref(0)

const typeClass = computed(() => `magnetic-${props.type}`)

const contentStyle = computed(() => {
  return {
    transform: `translate(${offsetX.value}px, ${offsetY.value}px)`,
    transition: 'transform 0.15s cubic-bezier(0.2, 0, 0, 1)'
  }
})

function handleMouseMove(e) {
  if (props.disabled || !btnRef.value) return
  const rect = btnRef.value.getBoundingClientRect()
  const x = e.clientX - rect.left - rect.width / 2
  const y = e.clientY - rect.top - rect.height / 2
  offsetX.value = x * props.strength
  offsetY.value = y * props.strength
}

function handleMouseLeave() {
  offsetX.value = 0
  offsetY.value = 0
}
</script>

<style scoped>
.magnetic-btn-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 22px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  outline: none;
  user-select: none;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.magnetic-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 2;
  will-change: transform;
}

.magnetic-primary {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.35);
}

.magnetic-primary:hover {
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.5);
  transform: translateY(-1px);
}

.magnetic-gradient {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
}

.magnetic-gradient:hover {
  box-shadow: 0 6px 20px rgba(124, 58, 237, 0.5);
  transform: translateY(-1px);
}

.magnetic-outline {
  background: rgba(255, 255, 255, 0.9);
  color: #334155;
  border: 1px solid #cbd5e1;
}

.magnetic-outline:hover {
  border-color: #94a3b8;
  background: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.btn-disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none !important;
}
</style>
