<template>
  <div class="app-root">
    <!-- 顶部现代玻璃态导航栏 -->
    <header class="top-nav glass-nav">
      <div class="nav-content">
        <!-- Logo 与实时引擎状态徽标 -->
        <div class="brand-left">
          <router-link to="/" class="brand-logo">
            <div class="logo-icon">
              <el-icon :size="20"><Calendar /></el-icon>
            </div>
            <div class="brand-text-wrap">
              <span class="brand-text">SmartSlot</span>
              <span class="brand-sub">智能时段预约系统</span>
            </div>
          </router-link>

          <!-- 实时引擎雷达徽章 (Landing.love 灵感) -->
          <div class="live-engine-pill">
            <span class="live-dot"></span>
            <span class="pill-text">排期防冲突引擎已就绪</span>
          </div>
        </div>

        <!-- 导航链接 -->
        <nav class="nav-links">
          <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">
            场馆全景
          </router-link>
          <router-link to="/matrix" class="nav-item matrix-nav-item" :class="{ active: $route.path === '/matrix' }">
            <span>日历时段矩阵</span>
            <span class="hot-badge shimmer-badge">NEW</span>
          </router-link>
          <router-link to="/my-bookings" class="nav-item" :class="{ active: $route.path === '/my-bookings' }">
            我的预约行程
          </router-link>
          <router-link 
            v-if="userStore.isAdmin" 
            to="/admin/dashboard" 
            class="nav-item admin-link" 
            :class="{ active: $route.path.startsWith('/admin') }"
          >
            运营控制台
          </router-link>
        </nav>

        <!-- 用户登录与个人状态 -->
        <div class="user-status-zone">
          <template v-if="userStore.isLoggedIn">
            <div class="balance-pill">
              <span class="balance-label">账户余额</span>
              <span class="balance-val">￥{{ userStore.userInfo?.balance || 0 }}</span>
            </div>

            <el-dropdown trigger="click">
              <div class="user-profile-btn">
                <el-avatar :size="34" :src="userStore.userInfo?.avatar" />
                <div class="user-name-role">
                  <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
                  <span class="user-role-tag">{{ userStore.isAdmin ? '系统管理员' : '尊享会员' }}</span>
                </div>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/my-bookings')">
                    <el-icon><List /></el-icon> 我的预约历程
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" @click="$router.push('/admin/dashboard')">
                    <el-icon><Platform /></el-icon> 运营数据控制台
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon> 退出当前登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <el-button type="primary" class="login-btn shimmer-badge" @click="$router.push('/login')">
              会员登录 / 注册
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
  ElMessage.success('已安全登出')
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
  height: 70px;
  position: sticky;
  top: 0;
  z-index: 100;
  transition: all 0.2s ease;
}

.nav-content {
  max-width: 1280px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.brand-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #ffffff;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.35);
}

.brand-text-wrap {
  display: flex;
  flex-direction: column;
}

.brand-text {
  font-size: 19px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.5px;
  line-height: 1.1;
}

.brand-sub {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin-top: 2px;
}

.live-engine-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  color: #166534;
  font-weight: 500;
}

.nav-links {
  display: flex;
  gap: 32px;
  align-items: center;
}

.nav-item {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  transition: all 0.2s ease;
  position: relative;
  padding: 8px 0;
  display: flex;
  align-items: center;
  gap: 6px;
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
  height: 2.5px;
  background: linear-gradient(90deg, #4f46e5, #818cf8);
  border-radius: 2px;
}

.hot-badge {
  font-size: 10px;
  background: linear-gradient(135deg, #ef4444, #f59e0b);
  color: #ffffff;
  padding: 1px 5px;
  border-radius: 10px;
  font-weight: 700;
}

.admin-link {
  color: #059669;
}

.user-status-zone {
  display: flex;
  align-items: center;
  gap: 16px;
}

.balance-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 13px;
}

.balance-label {
  color: #64748b;
  font-size: 12px;
}

.balance-val {
  color: #059669;
  font-weight: 800;
}

.user-profile-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 24px;
  transition: background 0.2s ease;
}

.user-profile-btn:hover {
  background: #f1f5f9;
}

.user-name-role {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.username {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.user-role-tag {
  font-size: 11px;
  color: #6366f1;
  font-weight: 600;
}

.login-btn {
  border-radius: 20px;
  font-weight: 600;
  padding: 8px 20px;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
}

.page-body {
  flex: 1;
}
</style>
