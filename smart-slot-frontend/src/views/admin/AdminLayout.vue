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
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>运营统计看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/venues">
          <el-icon><Menu /></el-icon>
          <span>场地配置管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon>
          <span>订单检索与核销</span>
        </el-menu-item>
      </el-menu>

      <!-- 快速核销按钮挂载在侧边栏底部 -->
      <div class="sidebar-verify-box">
        <el-button type="success" size="default" style="width: 100%;" @click="openQuickVerify">
          <el-icon><Ticket /></el-icon> 前台扫码核销
        </el-button>
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
import { Platform, DataLine, Menu, List, Ticket } from '@element-plus/icons-vue'
import VerifyModal from '@/components/VerifyModal.vue'

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

.sidebar-verify-box {
  padding: 16px;
  border-top: 1px solid var(--border-subtle);
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
