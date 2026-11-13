<template>
  <div class="admin-logs-page">
    <div class="page-header">
      <div>
        <h2 class="title">操作审计与安全日志</h2>
        <p class="subtitle">基于 Spring AOP @LogRecord 全程追踪管理与敏感操作，保障企业数据合规与追溯</p>
      </div>
      <el-button type="primary" plain @click="fetchLogs">
        <el-icon><Refresh /></el-icon> 刷新日志
      </el-button>
    </div>

    <!-- 筛选条件栏 -->
    <el-card shadow="never" class="filter-card card-shadow">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="操作模块">
          <el-input v-model="filterModule" placeholder="如: 场地配置、订单核销" clearable style="width: 180px;" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="filterUsername" placeholder="用户名" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchLogs">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志列表 -->
    <el-card shadow="never" class="table-card card-shadow">
      <el-table :data="logList" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="操作人" width="140">
          <template #default="{ row }">
            <div class="user-cell">
              <strong>{{ row.username }}</strong>
              <el-tag size="small" :type="getRoleTagType(row.role)" effect="light">
                {{ formatRole(row.role) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="110">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作描述" min-width="150" />
        <el-table-column prop="method" label="调用方法" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <code class="code-method">{{ row.method }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="params" label="入参(脱敏)" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="params-text">{{ row.params || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="执行状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result?.startsWith('SUCCESS') ? 'success' : 'danger'" size="small">
              {{ row.result?.startsWith('SUCCESS') ? '成功' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时" width="90">
          <template #default="{ row }">
            <span :class="row.durationMs > 200 ? 'text-warn' : 'text-ok'">{{ row.durationMs }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="客户端 IP" width="120" />
        <el-table-column prop="createTime" label="操作时间" min-width="160" />
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchLogs"
          @current-change="fetchLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getOperationLogs } from '@/api/admin'

const logList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filterModule = ref('')
const filterUsername = ref('')

async function fetchLogs() {
  loading.value = true
  try {
    const res = await getOperationLogs({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      module: filterModule.value || undefined,
      username: filterUsername.value || undefined
    })
    logList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function searchLogs() {
  currentPage.value = 1
  fetchLogs()
}

function resetSearch() {
  filterModule.value = ''
  filterUsername.value = ''
  currentPage.value = 1
  fetchLogs()
}

function formatRole(role) {
  if (role === 'ROLE_ADMIN') return '超级管理员'
  if (role === 'ROLE_MANAGER') return '场馆店长'
  if (role === 'ROLE_VERIFIER') return '核销员'
  return '会员'
}

function getRoleTagType(role) {
  if (role === 'ROLE_ADMIN') return 'danger'
  if (role === 'ROLE_MANAGER') return 'warning'
  if (role === 'ROLE_VERIFIER') return 'success'
  return 'info'
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
}

.subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
  padding: 8px 16px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
}

.table-card {
  border-radius: 12px;
  padding: 8px 16px 20px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
}

.user-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.code-method {
  font-family: monospace;
  font-size: 12px;
  color: #6366f1;
}

.params-text {
  font-family: monospace;
  font-size: 12px;
  color: var(--text-muted);
}

.text-ok {
  color: #10b981;
  font-weight: 600;
  font-family: monospace;
}

.text-warn {
  color: #f59e0b;
  font-weight: 700;
  font-family: monospace;
}

.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
