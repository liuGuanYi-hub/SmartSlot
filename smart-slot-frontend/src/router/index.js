import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import MatrixView from '@/views/MatrixView.vue'
import MyBookingsView from '@/views/MyBookingsView.vue'
import LoginView from '@/views/LoginView.vue'
import AdminLayout from '@/views/admin/AdminLayout.vue'
import AdminDashboard from '@/views/admin/AdminDashboard.vue'
import AdminVenues from '@/views/admin/AdminVenues.vue'
import AdminOrders from '@/views/admin/AdminOrders.vue'
import AdminLogs from '@/views/admin/AdminLogs.vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomeView
  },
  {
    path: '/matrix',
    name: 'Matrix',
    component: MatrixView
  },
  {
    path: '/venue/:id',
    name: 'VenueDetail',
    component: () => import('@/views/VenueDetailView.vue')
  },
  {
    path: '/my-bookings',
    name: 'MyBookings',
    component: MyBookingsView,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: () => import('@/views/UserProfileView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: { roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_VERIFIER'] },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: AdminDashboard,
        meta: { roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] }
      },
      {
        path: 'venues',
        name: 'AdminVenues',
        component: AdminVenues,
        meta: { roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: AdminOrders,
        meta: { roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_VERIFIER'] }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsers.vue'),
        meta: { roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] }
      },
      {
        path: 'iot-gate',
        name: 'AdminIotGate',
        component: () => import('@/views/admin/IotGateConsole.vue'),
        meta: { roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_VERIFIER'] }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: AdminLogs,
        meta: { roles: ['ROLE_ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// RBAC 路由前置守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 1. 检查需要登录但未登录
  if (to.meta?.requiresAuth && !userStore.token) {
    ElMessage.warning('请先登录后再进行操作')
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 2. 检查角色权限匹配
  if (to.meta?.roles && to.meta.roles.length > 0) {
    if (!userStore.token) {
      ElMessage.warning('请先登录管理凭证')
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
    if (!userStore.hasRole(to.meta.roles)) {
      ElMessage.error('权限不足：您的账号角色无权访问该管理模块')
      // 如果是普通会员去往首页，如果是核销员跳转到订单核销页
      if (userStore.isVerifier) {
        return next('/admin/orders')
      }
      return next(userStore.isStaff ? '/admin/dashboard' : '/matrix')
    }
  }

  next()
})

export default router
