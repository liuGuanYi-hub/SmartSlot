import { useUserStore } from '@/stores/user'

/**
 * RBAC 按钮级权限指令
 * 用法: v-permission="['ROLE_ADMIN', 'ROLE_MANAGER']"
 */
export default {
  mounted(el, binding) {
    const { value } = binding
    const userStore = useUserStore()
    const role = userStore.userInfo?.role || 'ROLE_USER'

    if (value && Array.isArray(value) && value.length > 0) {
      // 超级管理员拥有全部特权
      if (role === 'ROLE_ADMIN') {
        return
      }

      const hasPermission = value.includes(role)
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  }
}
