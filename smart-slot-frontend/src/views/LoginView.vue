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

      <!-- 快速填入演示账号按钮 -->
      <div v-if="!isRegister" class="quick-fill-row">
        <span class="quick-label">快速体验：</span>
        <el-button size="small" @click="quickFill('user', '123456')">会员 (user)</el-button>
        <el-button size="small" type="primary" plain @click="quickFill('user1', '123456')">会员 (user1)</el-button>
        <el-button size="small" type="success" plain @click="quickFill('admin', '123456')">超管 (admin)</el-button>
        <el-button size="small" type="warning" plain @click="quickFill('manager', '123456')">店长 (manager)</el-button>
        <el-button size="small" type="info" plain @click="quickFill('verifier', '123456')">核销员 (verifier)</el-button>
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
import { Calendar } from '@element-plus/icons-vue'
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

function quickFill(user, pwd) {
  loginForm.username = user
  loginForm.password = pwd
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
  max-width: 440px;
  padding: 36px 32px;
  border-radius: 16px;
  background: #ffffff;
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

.quick-fill-row {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 18px;
}

.quick-label {
  font-size: 12px;
  color: #64748b;
}

.toggle-mode {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  margin-top: 20px;
}
</style>
