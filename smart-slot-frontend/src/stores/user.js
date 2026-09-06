import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, getMyProfile } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('smart_slot_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('smart_slot_user') || 'null'))

  const user = computed(() => userInfo.value)
  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || 'ROLE_USER')
  const isAdmin = computed(() => role.value === 'ROLE_ADMIN')
  const isManager = computed(() => role.value === 'ROLE_MANAGER' || isAdmin.value)
  const isVerifier = computed(() => role.value === 'ROLE_VERIFIER' || isManager.value || isAdmin.value)
  const isStaff = computed(() => isAdmin.value || role.value === 'ROLE_MANAGER' || role.value === 'ROLE_VERIFIER')

  function hasRole(roles) {
    if (!roles || roles.length === 0) return true
    if (isAdmin.value) return true
    return roles.includes(role.value)
  }

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

  const getUserInfo = fetchCurrentUser

  function updateBalance(newBalance) {
    if (userInfo.value) {
      userInfo.value.balance = Number(newBalance)
      localStorage.setItem('smart_slot_user', JSON.stringify(userInfo.value))
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
    user,
    role,
    isLoggedIn,
    isAdmin,
    isManager,
    isVerifier,
    isStaff,
    hasRole,
    loginAction,
    fetchCurrentUser,
    getUserInfo,
    updateBalance,
    logout
  }
})
