import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, getMyProfile } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('smart_slot_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('smart_slot_user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ROLE_ADMIN')

  async function loginAction(credentials) {
    const res = await apiLogin(credentials)
    token.value = res.token
    userInfo.value = {
      userId: res.userId,
      username: res.username,
      nickname: res.nickname,
      role: res.role,
      avatar: res.avatar,
      balance: res.balance
    }
    localStorage.setItem('smart_slot_token', res.token)
    localStorage.setItem('smart_slot_user', JSON.stringify(userInfo.value))
    return res
  }

  async function fetchCurrentUser() {
    if (!token.value) return
    try {
      const user = await getMyProfile()
      userInfo.value = {
        ...userInfo.value,
        ...user
      }
      localStorage.setItem('smart_slot_user', JSON.stringify(userInfo.value))
    } catch (e) {
      console.error('Failed to sync profile', e)
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('smart_slot_token')
    localStorage.removeItem('smart_slot_user')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    loginAction,
    fetchCurrentUser,
    logout
  }
})
