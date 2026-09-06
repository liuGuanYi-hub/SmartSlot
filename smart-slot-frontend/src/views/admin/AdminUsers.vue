<template>
  <div class="admin-users-page">
    <div class="page-header">
      <div>
        <h2 class="title">全馆会员与权限中台</h2>
        <p class="subtitle">全馆注册会员档案、实名手机、账户余额授信、履约信用评级与 RBAC 权限协同治理</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" plain @click="fetchUsers">
          <el-icon><Refresh /></el-icon> 刷新会员档案
        </el-button>
      </div>
    </div>

    <!-- 筛选过滤工具条 -->
    <el-card class="filter-card card-shadow" :body-style="{ padding: '18px 20px' }">
      <el-form :inline="true" :model="queryParams" class="filter-form">
        <el-form-item label="会员搜索">
          <el-input 
            v-model="queryParams.keyword" 
            placeholder="用户名 / 昵称 / 手机号" 
            clearable 
            @keyup.enter="handleSearch"
            style="width: 220px;"
          />
        </el-form-item>

        <el-form-item label="身份角色">
          <el-select v-model="queryParams.role" placeholder="全部角色" clearable style="width: 160px;">
            <el-option label="普通会员 (USER)" value="ROLE_USER" />
            <el-option label="前台核销员 (VERIFIER)" value="ROLE_VERIFIER" />
            <el-option label="场馆店长 (MANAGER)" value="ROLE_MANAGER" />
            <el-option label="系统超管 (ADMIN)" value="ROLE_ADMIN" />
          </el-select>
        </el-form-item>

        <el-form-item label="账号状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px;">
            <el-option label="正常启用" :value="1" />
            <el-option label="已封禁锁定" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 会员数据表格 -->
    <el-card class="table-card card-shadow" :body-style="{ padding: '16px 20px' }">
      <el-table 
        :data="userList" 
        v-loading="loading" 
        stripe 
        style="width: 100%; border-radius: 12px; overflow: hidden;"
      >
        <el-table-column prop="id" label="会员 ID" width="90">
          <template #default="{ row }">
            <span class="font-mono">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column label="会员头像 / 昵称" min-width="170">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :src="row.avatar || defaultAvatar" />
              <div class="user-meta">
                <span class="nickname">{{ row.nickname || '未设昵称' }}</span>
                <span class="username font-mono">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="phone" label="联系手机" width="130">
          <template #default="{ row }">
            <span>{{ row.phone || '未绑定' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="role" label="RBAC 角色" width="140">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)" effect="light">
              {{ getRoleName(row.role) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="balance" label="账户余额" width="130">
          <template #default="{ row }">
            <span class="balance-text">￥{{ (row.balance || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="creditScore" label="履约信用分" width="120">
          <template #default="{ row }">
            <el-tag :type="getCreditTagType(row.creditScore)" round size="small">
              {{ row.creditScore ?? 100 }} 分
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="账号状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="dark" size="small">
              {{ row.status === 1 ? '正常' : '已封禁' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="注册时间" width="165">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <!-- 管理操作列 -->
        <el-table-column label="管理操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="primary" 
              link 
              @click="openBalanceDialog(row)"
            >
              调账充值
            </el-button>
            <el-button 
              size="small" 
              type="warning" 
              link 
              @click="openRoleDialog(row)"
            >
              设角色
            </el-button>
            <el-button 
              size="small" 
              type="info" 
              link 
              @click="openCreditDialog(row)"
            >
              信用分
            </el-button>
            <el-button 
              size="small" 
              :type="row.status === 1 ? 'danger' : 'success'" 
              link 
              @click="toggleUserStatus(row)"
            >
              {{ row.status === 1 ? '封禁' : '解封' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>

    <!-- 模态框 1: 人工调账 / 充值对话框 -->
    <el-dialog v-model="balanceDialogVisible" title="会员余额人工调整与授信" width="460px" destroy-on-close>
      <div v-if="selectedUser" class="dialog-content">
        <el-alert
          title="调账须知：调账金额支持正数（增加余额/充值）与负数（扣减金额），操作将记录进对账流水并入审计日志。"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
        />
        <div class="user-mini-card">
          <span>会员：<strong>{{ selectedUser.nickname }} ({{ selectedUser.username }})</strong></span>
          <span>当前余额：<strong class="balance-text">￥{{ selectedUser.balance }}</strong></span>
        </div>
        <el-form label-position="top" style="margin-top: 16px;">
          <el-form-item label="调整金额 (正数增加，负数扣除)">
            <el-input-number v-model="balanceForm.amount" :step="50" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="调账原因 / 审计备注">
            <el-input v-model="balanceForm.reason" placeholder="如：现金收银充值、线下赛事津贴补发、违规补偿扣除等" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="balanceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingBalance" @click="submitBalanceAdjust">
          确认调整
        </el-button>
      </template>
    </el-dialog>

    <!-- 模态框 2: RBAC 角色分配对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配会员 RBAC 角色" width="440px" destroy-on-close>
      <div v-if="selectedUser" class="dialog-content">
        <div class="user-mini-card">
          <span>会员：<strong>{{ selectedUser.nickname }} ({{ selectedUser.username }})</strong></span>
          <span>当前角色：<strong>{{ getRoleName(selectedUser.role) }}</strong></span>
        </div>
        <el-form label-position="top" style="margin-top: 16px;">
          <el-form-item label="目标分配角色">
            <el-select v-model="selectedRole" style="width: 100%;">
              <el-option label="普通会员 (ROLE_USER) - 预约选座、钱包充值" value="ROLE_USER" />
              <el-option label="前台核销员 (ROLE_VERIFIER) - 扫码入场核销、现场放行" value="ROLE_VERIFIER" />
              <el-option label="场馆店长 (ROLE_MANAGER) - 场地配置、营收看板、调账运营" value="ROLE_MANAGER" />
              <el-option label="系统超管 (ROLE_ADMIN) - 系统全量最高权限" value="ROLE_ADMIN" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="submittingRole" @click="submitRoleChange">
          确认分配角色
        </el-button>
      </template>
    </el-dialog>

    <!-- 模态框 3: 信用分维护对话框 -->
    <el-dialog v-model="creditDialogVisible" title="维护会员履约信用分" width="420px" destroy-on-close>
      <div v-if="selectedUser" class="dialog-content">
        <div class="user-mini-card">
          <span>会员：<strong>{{ selectedUser.nickname }} ({{ selectedUser.username }})</strong></span>
          <span>当前信用分：<strong>{{ selectedUser.creditScore || 100 }} 分</strong></span>
        </div>
        <el-form label-position="top" style="margin-top: 16px;">
          <el-form-item label="设定信用分 (0 ~ 120 分)">
            <el-input-number v-model="creditScoreInput" :min="0" :max="120" style="width: 100%;" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="creditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingCredit" @click="submitCreditAdjust">
          保存信用分
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminUsersPage, updateAdminUserStatus, updateAdminUserRole, adjustAdminUserBalance, adjustAdminUserCredit } from '@/api/user'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const userList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const queryParams = reactive({
  keyword: '',
  role: '',
  status: null
})

// 调账模态框
const balanceDialogVisible = ref(false)
const selectedUser = ref(null)
const balanceForm = reactive({
  amount: 100,
  reason: '会员线下现金充值授信'
})
const submittingBalance = ref(false)

// 角色模态框
const roleDialogVisible = ref(false)
const selectedRole = ref('')
const submittingRole = ref(false)

// 信用模态框
const creditDialogVisible = ref(false)
const creditScoreInput = ref(100)
const submittingCredit = ref(false)

function formatTime(t) {
  if (!t) return ''
  return dayjs(t).format('YYYY-MM-DD HH:mm')
}

function getRoleName(role) {
  switch (role) {
    case 'ROLE_ADMIN': return '超级管理员'
    case 'ROLE_MANAGER': return '场馆店长'
    case 'ROLE_VERIFIER': return '前台核销员'
    case 'ROLE_USER': return '普通会员'
    default: return role || '普通会员'
  }
}

function getRoleTagType(role) {
  switch (role) {
    case 'ROLE_ADMIN': return 'danger'
    case 'ROLE_MANAGER': return 'warning'
    case 'ROLE_VERIFIER': return 'success'
    case 'ROLE_USER': return 'info'
    default: return 'info'
  }
}

function getCreditTagType(score) {
  if (score >= 90) return 'success'
  if (score >= 70) return 'warning'
  return 'danger'
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getAdminUsersPage({
      current: currentPage.value,
      size: pageSize.value,
      keyword: queryParams.keyword || undefined,
      role: queryParams.role || undefined,
      status: queryParams.status !== null ? queryParams.status : undefined
    })
    userList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchUsers()
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.role = ''
  queryParams.status = null
  currentPage.value = 1
  fetchUsers()
}

async function toggleUserStatus(row) {
  const targetStatus = row.status === 1 ? 0 : 1
  const actionText = targetStatus === 1 ? '解封启用' : '封禁锁定'
  try {
    await ElMessageBox.confirm(
      `确定要将用户【${row.nickname || row.username}】账号${actionText}吗？${targetStatus === 0 ? '封禁后该用户将无法登录与抢占时段。' : ''}`,
      '操作确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: targetStatus === 1 ? 'info' : 'warning'
      }
    )
    await updateAdminUserStatus(row.id, targetStatus)
    ElMessage.success(`用户已成功${actionText}`)
    fetchUsers()
  } catch (e) {
    // canceled
  }
}

function openBalanceDialog(user) {
  selectedUser.value = user
  balanceForm.amount = 100
  balanceForm.reason = '会员线下充值授信'
  balanceDialogVisible.value = true
}

async function submitBalanceAdjust() {
  if (!balanceForm.amount) {
    ElMessage.warning('请输入有效调账金额')
    return
  }
  submittingBalance.value = true
  try {
    await adjustAdminUserBalance(selectedUser.value.id, balanceForm)
    ElMessage.success('用户余额调整成功')
    balanceDialogVisible.value = false
    fetchUsers()
  } catch (e) {
    console.error(e)
  } finally {
    submittingBalance.value = false
  }
}

function openRoleDialog(user) {
  selectedUser.value = user
  selectedRole.value = user.role || 'ROLE_USER'
  roleDialogVisible.value = true
}

async function submitRoleChange() {
  submittingRole.value = true
  try {
    await updateAdminUserRole(selectedUser.value.id, selectedRole.value)
    ElMessage.success('用户角色分配成功')
    roleDialogVisible.value = false
    fetchUsers()
  } catch (e) {
    console.error(e)
  } finally {
    submittingRole.value = false
  }
}

function openCreditDialog(user) {
  selectedUser.value = user
  creditScoreInput.value = user.creditScore || 100
  creditDialogVisible.value = true
}

async function submitCreditAdjust() {
  submittingCredit.value = true
  try {
    await adjustAdminUserCredit(selectedUser.value.id, creditScoreInput.value)
    ElMessage.success('信用分维护成功')
    creditDialogVisible.value = false
    fetchUsers()
  } catch (e) {
    console.error(e)
  } finally {
    submittingCredit.value = false
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.admin-users-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
}

.subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.filter-card {
  border-radius: 16px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  margin-bottom: 20px;
}

.table-card {
  border-radius: 16px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-meta {
  display: flex;
  flex-direction: column;
}

.nickname {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.username {
  font-size: 11px;
  color: var(--text-muted);
}

.balance-text {
  font-weight: 700;
  color: #10b981;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.user-mini-card {
  background: var(--card-bg-elevated);
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--border-subtle);
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
</style>
