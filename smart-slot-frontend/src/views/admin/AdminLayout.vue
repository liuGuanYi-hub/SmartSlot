<template>
  <div class="admin-container">
    <!-- 侧边栏导航 -->
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <el-icon :size="20" color="#4f46e5"><Platform /></el-icon>
        <span>SmartSlot 控制台</span>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        class="admin-menu"
      >
        <el-menu-item index="/admin/dashboard" v-permission="['ROLE_ADMIN', 'ROLE_MANAGER']">
          <el-icon><DataLine /></el-icon>
          <span>运营统计看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/venues" v-permission="['ROLE_ADMIN', 'ROLE_MANAGER']">
          <el-icon><Menu /></el-icon>
          <span>场地配置管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders" v-permission="['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_VERIFIER']">
          <el-icon><List /></el-icon>
          <span>订单检索与核销</span>
        </el-menu-item>
        <el-menu-item index="/admin/iot-gate" v-permission="['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_VERIFIER']">
          <el-icon><Cpu /></el-icon>
          <span>智能道闸中控</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs" v-permission="['ROLE_ADMIN']">
          <el-icon><Document /></el-icon>
          <span>操作审计日志</span>
        </el-menu-item>
      </el-menu>

      <!-- 角色标识与快速核销按钮挂载在侧边栏底部 -->
      <div class="sidebar-footer-box">
        <div class="current-role-badge">
          <span class="role-desc">当前身份:</span>
          <el-tag size="small" :type="userStore.isAdmin ? 'danger' : userStore.isManager ? 'warning' : 'success'">
            {{ userStore.isAdmin ? '超级管理员' : userStore.isManager ? '运营店长' : '核销前台' }}
          </el-tag>
        </div>
        <div class="sidebar-verify-box" v-permission="['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_VERIFIER']">
          <el-button type="success" size="default" style="width: 100%;" @click="openQuickVerify">
            <el-icon><Ticket /></el-icon> 前台扫码核销
          </el-button>
        </div>
      </div>
    </aside>

    <!-- 右侧主体内容区域 -->
    <main class="admin-main">
      <router-view />
    </main>

    <!-- 全局快捷核销弹窗组件 -->
    <VerifyModal ref="verifyModalRef" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Platform, DataLine, Menu, List, Ticket, Document, Cpu } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import VerifyModal from '@/components/VerifyModal.vue'

const userStore = useUserStore()
const verifyModalRef = ref(null)

function openQuickVerify() {
  verifyModalRef.value?.open()
}
</script>

<style scoped>
.admin-container {
  display: flex;
  min-height: calc(100vh - 70px);
}

.admin-sidebar {
  width: 220px;
  background: var(--card-bg);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
}

.admin-brand {
  padding: 20px 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 15px;
  color: var(--text-main);
  border-bottom: 1px solid var(--border-subtle);
}

.admin-menu {
  border-right: none;
  flex: 1;
  background: transparent;
}

.sidebar-footer-box {
  border-top: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
}

.current-role-badge {
  padding: 12px 16px 4px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.role-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.sidebar-verify-box {
  padding: 12px 16px 16px 16px;
}

.admin-main {
  flex: 1;
  padding: 24px;
  background: var(--bg-color);
  overflow-y: auto;
}

@media (max-width: 768px) {
  .admin-container {
    flex-direction: column;
  }
  .admin-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid var(--border-subtle);
  }
  .admin-main {
    padding: 14px;
  }
}
</style>
