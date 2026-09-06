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

        <!-- 用户登录与个人状态与主题切换 -->
        <div class="user-status-zone">
          <!-- 主题模式切换按钮 (View Transitions 圆形水波转场) -->
          <el-tooltip :content="isDark ? '切换至晨曦浅色模式' : '切换至暗黑极夜模式'" placement="bottom">
            <button 
              class="theme-toggle-btn gpu-accel" 
              @click="toggleTheme($event)" 
              :title="isDark ? '切换至晨曦浅色模式' : '切换至暗黑极夜模式'"
              aria-label="Toggle theme"
            >
              <el-icon :size="17" class="theme-icon">
                <Sunny v-if="isDark" />
                <Moon v-else />
              </el-icon>
            </button>
          </el-tooltip>

          <template v-if="userStore.isLoggedIn">
            <div class="balance-pill clickable-pill" @click="$router.push('/profile')" title="点击进入个人中心与钱包充值">
              <span class="balance-label">账户余额</span>
              <span class="balance-val">￥{{ (userStore.userInfo?.balance || 0).toFixed ? (userStore.userInfo?.balance || 0).toFixed(2) : (userStore.userInfo?.balance || 0) }}</span>
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
                  <el-dropdown-item @click="$router.push('/profile')">
                    <el-icon><User /></el-icon> 个人中心 & 我的钱包
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/my-bookings')">
                    <el-icon><List /></el-icon> 我的预约历程
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isStaff" @click="$router.push('/admin/dashboard')">
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

          <!-- 移动端汉堡折叠按钮 -->
          <button class="mobile-menu-btn" @click="mobileDrawer = true" aria-label="Open mobile menu">
            <el-icon :size="20"><MenuIcon /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <!-- 移动端抽屉侧滑导航 (极致响应式触控) -->
    <el-drawer
      v-model="mobileDrawer"
      title="SmartSlot 快捷导航"
      direction="rtl"
      size="280px"
      :with-header="true"
    >
      <div class="mobile-drawer-content">
        <div v-if="userStore.isLoggedIn" class="mobile-user-card">
          <el-avatar :size="48" :src="userStore.userInfo?.avatar" />
          <div class="mobile-user-info">
            <span class="m-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
            <span class="m-bal">余额: ￥{{ userStore.userInfo?.balance || 0 }}</span>
          </div>
        </div>

        <nav class="mobile-nav-list">
          <router-link to="/" class="m-nav-item" @click="mobileDrawer = false">
            <el-icon><Calendar /></el-icon> 场馆全景
          </router-link>
          <router-link to="/matrix" class="m-nav-item" @click="mobileDrawer = false">
            <el-icon><Calendar /></el-icon> 日历时段矩阵
          </router-link>
          <router-link to="/my-bookings" class="m-nav-item" @click="mobileDrawer = false">
            <el-icon><List /></el-icon> 我的预约行程
          </router-link>
          <router-link 
            v-if="userStore.isAdmin" 
            to="/admin/dashboard" 
            class="m-nav-item admin-m-link" 
            @click="mobileDrawer = false"
          >
            <el-icon><Platform /></el-icon> 运营控制台
          </router-link>
        </nav>

        <div class="mobile-drawer-footer">
          <button class="m-theme-toggle" @click="toggleTheme($event)">
            <el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon>
            <span>{{ isDark ? '切换至晨曦浅色' : '切换至极夜暗黑' }}</span>
          </button>
          <el-button 
            v-if="userStore.isLoggedIn" 
            type="danger" 
            plain 
            style="width: 100%; margin-top: 12px;" 
            @click="handleLogout(); mobileDrawer = false;"
          >
            安全退出登录
          </el-button>
          <el-button 
            v-else 
            type="primary" 
            style="width: 100%; margin-top: 12px;" 
            @click="$router.push('/login'); mobileDrawer = false;"
          >
            登录 / 注册
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 页面主体容器 -->
    <main class="page-body">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Calendar, List, Platform, SwitchButton, Sunny, Moon, Menu as MenuIcon, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { initTheme, useTheme } from '@/composables/useTheme'

const userStore = useUserStore()
const router = useRouter()
const { isDark, toggleTheme } = useTheme()
const mobileDrawer = ref(false)

function handleLogout() {
  userStore.logout()
  ElMessage.success('已安全登出')
  router.push('/login')
}

onMounted(() => {
  initTheme()
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
  color: var(--text-main);
  letter-spacing: -0.5px;
  line-height: 1.1;
}

.brand-sub {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
  letter-spacing: 0.5px;
  margin-top: 2px;
}

.live-engine-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--pill-bg);
  border: 1px solid var(--pill-border);
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  color: var(--pill-text);
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
  color: var(--text-secondary);
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
  gap: 14px;
}

.theme-toggle-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--border-subtle);
  background: var(--card-bg-elevated);
  color: var(--text-main);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.theme-toggle-btn:hover {
  transform: rotate(15deg) scale(1.08);
  border-color: var(--primary-light);
  color: var(--primary-color);
}

.balance-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--card-bg-elevated);
  border: 1px solid var(--border-subtle);
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 13px;
}

.balance-label {
  color: var(--text-muted);
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
  background: var(--card-bg-elevated);
}

.user-name-role {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.username {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
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

.mobile-menu-btn {
  display: none;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid var(--border-subtle);
  background: var(--card-bg-elevated);
  color: var(--text-main);
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.mobile-drawer-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: space-between;
}

.mobile-user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--card-bg-elevated);
  border-radius: 12px;
  margin-bottom: 20px;
}

.mobile-user-info {
  display: flex;
  flex-direction: column;
}

.m-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}

.m-bal {
  font-size: 12px;
  color: #10b981;
  font-weight: 600;
}

.mobile-nav-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.m-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.m-nav-item:hover, .m-nav-item.router-link-active {
  background: var(--card-bg-elevated);
  color: var(--primary-color);
}

.m-theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid var(--border-subtle);
  background: var(--card-bg-elevated);
  color: var(--text-main);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 768px) {
  .nav-links, .live-engine-pill, .balance-pill {
    display: none;
  }
  .mobile-menu-btn {
    display: flex;
  }
  .nav-content {
    padding: 0 16px;
  }
}

.page-body {
  flex: 1;
}
</style>
