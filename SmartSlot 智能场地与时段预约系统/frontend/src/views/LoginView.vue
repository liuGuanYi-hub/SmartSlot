<template>
  <div class="login-page">
    <div class="login-box card-shadow">
      <div class="login-header">
        <div class="logo-badge">
          <el-icon :size="24"><Calendar /></el-icon>
        </div>
        <h2 class="title">{{ isRegister ? '注册 SmartSlot 账号' : '登录 SmartSlot 预约系统' }}</h2>
        <p class="subtitle">{{ isRegister ? '加入场馆会员，享受智能时段预订与特权' : '欢迎回来，请输入您的账号凭证' }}</p>
      </div>

      <!-- 快速填入演示账号按钮 (现代卡片网格排版，杜绝溢出) -->
      <div v-if="!isRegister" class="quick-fill-box">
        <div class="quick-header">
          <div class="quick-title">
            <el-icon><Key /></el-icon>
            <span>一键填入演示账号</span>
          </div>
          <span class="quick-pwd-tag">默认密码: 123456</span>
        </div>
        <div class="quick-tags-grid">
          <button 
            type="button" 
            class="account-chip chip-user" 
            @click="quickFill('user', '123456', '普通会员')"
            title="会员用户: 场地选座、预约、充值支付"
          >
            <span class="chip-badge">会员</span>
            <span class="chip-name">user</span>
          </button>
          <button 
            type="button" 
            class="account-chip chip-user1" 
            @click="quickFill('user1', '123456', '新会员')"
            title="新会员用户: 协同多端预约体验"
          >
            <span class="chip-badge">会员2</span>
            <span class="chip-name">user1</span>
          </button>
          <button 
            type="button" 
            class="account-chip chip-admin" 
            @click="quickFill('admin', '123456', '超级管理员')"
            title="超管: 全局运营大屏、道闸控制、审计日志"
          >
            <span class="chip-badge">超管</span>
            <span class="chip-name">admin</span>
          </button>
          <button 
            type="button" 
            class="account-chip chip-manager" 
            @click="quickFill('manager', '123456', '运营店长')"
            title="店长: 场地排期调度、订单检索"
          >
            <span class="chip-badge">店长</span>
            <span class="chip-name">manager</span>
          </button>
          <button 
            type="button" 
            class="account-chip chip-verifier" 
            @click="quickFill('verifier', '123456', '前台核销员')"
            title="核销员: 扫码/6位核销码入场核验"
          >
            <span class="chip-badge">核销员</span>
            <span class="chip-name">verifier</span>
          </button>
        </div>
      </div>

      <!-- 表单 (登录模式) -->
      <el-form
        v-if="!isRegister"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" style="width: 100%; margin-top: 8px;" @click="handleLogin">
          立即登录
        </el-button>
      </el-form>

      <!-- 表单 (注册模式) -->
      <el-form
        v-else
        ref="regFormRef"
        :model="regForm"
        :rules="regRules"
        label-position="top"
        @keyup.enter="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="regForm.username" placeholder="3-20 位字母/数字" />
        </el-form-item>
        <el-form-item label="用户昵称" prop="nickname">
          <el-input v-model="regForm.nickname" placeholder="例如: 羽球达人" />
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="regForm.phone" placeholder="11 位手机号码" maxlength="11" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="regForm.password" type="password" show-password placeholder="至少 6 位密码" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" style="width: 100%; margin-top: 8px;" @click="handleRegister">
          立即注册新账号
        </el-button>
      </el-form>

      <!-- 切换登录/注册 -->
      <div class="toggle-mode">
        <span v-if="!isRegister">
          还没有账号？<el-link type="primary" :underline="false" @click="isRegister = true">立即注册</el-link>
        </span>
        <span v-else>
          已有账号？<el-link type="primary" :underline="false" @click="isRegister = false">直接登录</el-link>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Calendar, Key } from '@element-plus/icons-vue'
import { register } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const isRegister = ref(false)
const loading = ref(false)

const loginFormRef = ref(null)
const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regFormRef = ref(null)
const regForm = reactive({
  username: '',
  nickname: '',
  phone: '',
  password: ''
})

const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需在 3-20 位之间', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的11位手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
}

function quickFill(user, pwd, roleName = '') {
  loginForm.username = user
  loginForm.password = pwd
  if (roleName) {
    ElMessage.success(`已填入【${roleName}】演示账号凭证`)
  }
}

async function handleLogin() {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate()

  loading.value = true
  try {
    const res = await userStore.loginAction(loginForm)
    ElMessage.success(`欢迎回来，${res.nickname || res.username}！`)
    if (res.role === 'ROLE_ADMIN' || res.role === 'ROLE_MANAGER') {
      router.push('/admin/dashboard')
    } else if (res.role === 'ROLE_VERIFIER') {
      router.push('/admin/orders')
    } else {
      router.push('/matrix')
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!regFormRef.value) return
  await regFormRef.value.validate()

  loading.value = true
  try {
    await register(regForm)
    ElMessage.success('注册成功！已自动切换至登录，请输入密码登录')
    loginForm.username = regForm.username
    loginForm.password = ''
    isRegister.value = false
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 120px);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
}

.login-box {
  width: 100%;
  max-width: 480px;
  padding: 36px 32px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 30px -5px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(0, 0, 0, 0.04);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-badge {
  width: 52px;
  height: 52px;
  background: #4f46e5;
  color: #ffffff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.3);
}

.title {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 6px;
}

/* 快速填入演示账号面板 */
.quick-fill-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 22px;
}

.quick-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.quick-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.quick-pwd-tag {
  font-size: 11px;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
  font-family: monospace;
}

.quick-tags-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.account-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-sizing: border-box;
}

.account-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.chip-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.chip-name {
  font-family: monospace;
  font-weight: 600;
  font-size: 12px;
  color: #1e293b;
}

/* 角色专属色彩 */
.chip-user .chip-badge {
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
}
.chip-user:hover {
  border-color: #3b82f6;
}

.chip-user1 .chip-badge {
  background: rgba(14, 165, 233, 0.12);
  color: #0284c7;
}
.chip-user1:hover {
  border-color: #0ea5e9;
}

.chip-admin .chip-badge {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
}
.chip-admin:hover {
  border-color: #10b981;
}

.chip-manager .chip-badge {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}
.chip-manager:hover {
  border-color: #f59e0b;
}

.chip-verifier .chip-badge {
  background: rgba(139, 92, 246, 0.12);
  color: #7c3aed;
}
.chip-verifier:hover {
  border-color: #8b5cf6;
}

.toggle-mode {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  margin-top: 20px;
}
</style>
