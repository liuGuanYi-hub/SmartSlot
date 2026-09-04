<template>
  <div class="app-root">
    <!-- 顶部现代玻璃态导航栏 -->
    <header class="top-nav glass-nav">
      <div class="nav-content">
        <!-- Logo -->
        <router-link to="/" class="brand-logo">
          <div class="logo-icon">
            <el-icon :size="20"><Calendar /></el-icon>
          </div>
          <span class="brand-text">SmartSlot <small>预约系统</small></span>
        </router-link>

        <!-- 导航链接 -->
        <nav class="nav-links">
          <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">
            场馆首页
          </router-link>
          <router-link to="/matrix" class="nav-item" :class="{ active: $route.path === '/matrix' }">
            <el-badge is-dot class="badge-dot">时段矩阵</el-badge>
          </router-link>
          <router-link to="/my-bookings" class="nav-item" :class="{ active: $route.path === '/my-bookings' }">
            我的预约
          </router-link>
          <router-link 
            v-if="userStore.isAdmin" 
            to="/admin/dashboard" 
            class="nav-item admin-link" 
            :class="{ active: $route.path.startsWith('/admin') }"
          >
            管理控制台
          </router-link>
        </nav>

        <!-- 用户登录与个人状态 -->
        <div class="user-status-zone">
          <template v-if="userStore.isLoggedIn">
            <div class="balance-tag">
              <span>余额: </span>
              <strong>￥{{ userStore.userInfo?.balance || 0 }}</strong>
            </div>

            <el-dropdown trigger="click">
              <div class="user-profile-btn">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar" />
                <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/my-bookings')">
                    <el-icon><List /></el-icon> 我的预约历史
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" @click="$router.push('/admin/dashboard')">
                    <el-icon><Platform /></el-icon> 进入管理后台
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon> 退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <el-button type="primary" size="default" @click="$router.push('/login')">
              登录 / 注册
            </el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 页面主体容器 -->
    <main class="page-body">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { Calendar, List, Platform, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()

function handleLogout() {
  userStore.logout()
  ElMessage.success('已安全退出')
  router.push('/login')
}

onMounted(() => {
  userStore.fetchCurrentUser()
})
</script>

<style scoped>
.app-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.top-nav {
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-content {
  max-width: 1240px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: #4f46e5;
  color: #ffffff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-text {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.brand-text small {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  margin-left: 4px;
}

.nav-links {
  display: flex;
  gap: 28px;
  align-items: center;
}

.nav-item {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  transition: color 0.15s ease;
  position: relative;
  padding: 8px 0;
}

.nav-item:hover, .nav-item.active {
  color: #4f46e5;
}

.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: #4f46e5;
  border-radius: 2px;
}

.admin-link {
  color: #059669;
}

.user-status-zone {
  display: flex;
  align-items: center;
  gap: 16px;
}

.balance-tag {
  background: #f1f5f9;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 13px;
  color: #475569;
}

.balance-tag strong {
  color: #059669;
}

.user-profile-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.page-body {
  flex: 1;
}
</style>
