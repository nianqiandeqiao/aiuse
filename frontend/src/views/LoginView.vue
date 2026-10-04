<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">💊</div>
      <div class="login-title">AI 用药提醒与用药安全助手</div>
      <div class="login-sub">登录后只能看到并管理自己的老人与用药，互不干扰</div>

      <el-tabs v-model="mode" stretch class="login-tabs">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane v-if="allowRegister" label="注册新账号" name="register" />
      </el-tabs>

      <!-- 登录 -->
      <template v-if="mode === 'login'">
        <el-input
          v-model="loginForm.username"
          size="large"
          placeholder="用户名"
          class="login-field"
          @keyup.enter="doLogin"
        >
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
        <el-input
          v-model="loginForm.password"
          type="password"
          size="large"
          placeholder="密码"
          show-password
          class="login-field"
          @keyup.enter="doLogin"
        >
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>

        <div class="login-extra">
          <el-checkbox v-model="rememberMe" size="large">记住我的用户名</el-checkbox>
        </div>

        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="doLogin">
          登 录
        </el-button>

        <div v-if="allowRegister" class="login-switch">
          还没有账号？
          <el-button link type="primary" @click="mode = 'register'">点这里注册一个</el-button>
        </div>
      </template>

      <!-- 注册 -->
      <template v-else>
        <el-input
          v-model="registerForm.username"
          size="large"
          placeholder="用户名（3-20 个字，字母/数字/中文）"
          class="login-field"
        >
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
        <el-input
          v-model="registerForm.displayName"
          size="large"
          placeholder="怎么称呼您？（选填，如：王奶奶的家属）"
          class="login-field"
        >
          <template #prefix><el-icon><Avatar /></el-icon></template>
        </el-input>
        <el-input
          v-model="registerForm.password"
          type="password"
          size="large"
          placeholder="密码（至少 6 位）"
          show-password
          class="login-field"
        >
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          size="large"
          placeholder="再输一次密码"
          show-password
          class="login-field"
          @keyup.enter="doRegister"
        >
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>

        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="doRegister">
          注册并进入系统
        </el-button>

        <div class="login-switch">
          已经有账号了？
          <el-button link type="primary" @click="mode = 'login'">返回登录</el-button>
        </div>
      </template>

      <div class="login-tip">忘记密码请联系系统管理员重置；建议用纸笔记下用户名和密码。</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginUser, registerUser } from '../api'
import { ensureAuthChecked, markLoggedIn, useAuth } from '../stores/auth'

const USERNAME_KEY = 'med-reminder:last-username'

const router = useRouter()
const auth = useAuth()

const mode = ref('login')
const loading = ref(false)
const allowRegister = ref(true)
const rememberMe = ref(true)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', displayName: '', password: '', confirmPassword: '' })

onMounted(async () => {
  const status = await ensureAuthChecked(true)
  allowRegister.value = status.allowRegister
  if (status.loggedIn) {
    router.replace('/home')
    return
  }
  // 便民：记住上次登录的用户名，下次只需输密码
  const saved = localStorage.getItem(USERNAME_KEY)
  if (saved) {
    loginForm.username = saved
  }
})

async function doLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const user = await loginUser({ username: loginForm.username, password: loginForm.password })
    if (rememberMe.value) {
      localStorage.setItem(USERNAME_KEY, loginForm.username)
    } else {
      localStorage.removeItem(USERNAME_KEY)
    }
    markLoggedIn(user)
    ElMessage.success(`欢迎回来，${user.displayName || user.username}`)
    router.replace('/home')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const user = await registerUser({
      username: registerForm.username,
      password: registerForm.password,
      displayName: registerForm.displayName
    })
    localStorage.setItem(USERNAME_KEY, user.username)
    markLoggedIn(user)
    ElMessage.success('注册成功，已自动登录')
    router.replace('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #1f4e79 0%, #2d6da3 100%);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 560px;
  background: #fff;
  border-radius: 16px;
  padding: 36px 34px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
  text-align: center;
}

.login-logo {
  font-size: 54px;
  line-height: 1;
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--app-primary);
  margin: 10px 0 8px;
}

.login-sub {
  font-size: 16px;
  color: #5a6672;
  margin-bottom: 18px;
}

.login-tabs {
  margin-bottom: 6px;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 19px;
  font-weight: 600;
}

.login-field {
  margin-bottom: 16px;
}

.login-extra {
  text-align: left;
  margin-bottom: 6px;
}

.login-button {
  width: 100%;
  margin-top: 12px;
  font-weight: 700;
  letter-spacing: 2px;
}

.login-switch {
  margin-top: 14px;
  font-size: 16px;
  color: #5a6672;
}

.login-tip {
  margin-top: 18px;
  font-size: 14px;
  color: #8a939b;
  line-height: 1.6;
}
</style>

