import { ref } from 'vue'

const THEME_KEY = 'smartslot-theme'

const isDark = ref(false)

// 初始化主题
export function initTheme() {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved) {
    isDark.value = saved === 'dark'
  } else {
    isDark.value = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  applyTheme(isDark.value)

  // 监听系统深色模式变更
  if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (!localStorage.getItem(THEME_KEY)) {
        isDark.value = e.matches
        applyTheme(e.matches)
      }
    })
  }
}

function applyTheme(dark) {
  if (dark) {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
}

export function useTheme() {
  function toggleTheme(event) {
    const willBeDark = !isDark.value

    // 判断浏览器是否支持 View Transitions API 且无减弱动画偏好
    const isAppearanceTransition =
      document.startViewTransition &&
      !window.matchMedia('(prefers-reduced-motion: reduce)').matches

    if (!isAppearanceTransition || !event) {
      isDark.value = willBeDark
      applyTheme(willBeDark)
      localStorage.setItem(THEME_KEY, willBeDark ? 'dark' : 'light')
      return
    }

    const x = event.clientX
    const y = event.clientY
    const endRadius = Math.hypot(
      Math.max(x, innerWidth - x),
      Math.max(y, innerHeight - y)
    )

    const transition = document.startViewTransition(async () => {
      isDark.value = willBeDark
      applyTheme(willBeDark)
      localStorage.setItem(THEME_KEY, willBeDark ? 'dark' : 'light')
    })

    transition.ready.then(() => {
      const clipPath = [
        `circle(0px at ${x}px ${y}px)`,
        `circle(${endRadius}px at ${x}px ${y}px)`
      ]
      document.documentElement.animate(
        {
          clipPath: willBeDark ? clipPath : [...clipPath].reverse()
        },
        {
          duration: 400,
          easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
          pseudoElement: willBeDark
            ? '::view-transition-new(root)'
            : '::view-transition-old(root)'
        }
      )
    })
  }

  return {
    isDark,
    toggleTheme
  }
}
