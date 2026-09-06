<template>
  <span class="number-ticker">{{ displayValue }}</span>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  value: {
    type: Number,
    required: true,
    default: 0
  },
  duration: {
    type: Number,
    default: 1200
  },
  decimals: {
    type: Number,
    default: 0
  }
})

const displayValue = ref(0)

function animate() {
  const start = 0
  const end = props.value
  const startTime = performance.now()

  function update(now) {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / props.duration, 1)
    // EaseOutExpo 平滑减速曲线
    const easeProgress = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress)
    const current = start + (end - start) * easeProgress

    displayValue.value = current.toFixed(props.decimals)

    if (progress < 1) {
      requestAnimationFrame(update)
    }
  }

  requestAnimationFrame(update)
}

watch(() => props.value, () => {
  animate()
})

onMounted(() => {
  animate()
})
</script>

<style scoped>
.number-ticker {
  font-variant-numeric: tabular-nums;
  font-weight: inherit;
  letter-spacing: -0.02em;
}
</style>
