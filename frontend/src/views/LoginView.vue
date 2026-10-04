<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">💊</div>
      <div class="login-title">AI 用药提醒与用药安全助手</div>
      <div class="login-sub">该演示系统已开启访问口令，请输入口令后使用</div>

      <el-input
        v-model="password"
        type="password"
        size="large"
        placeholder="请输入访问口令"
        show-password
        @keyup.enter="submit"
      />
      <el-button type="primary" size="large" class="login-button" :loading="loading" @click="submit">
        进 入 系 统
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'
import { markLoggedIn } from '../stores/auth'

const router = useRouter()
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!password.value) {
    ElMessage.warning('请输入访问口令')
    return
  }
  loading.value = true
  try {
    await login(password.value)
    markLoggedIn()
    ElMessage.success('欢迎使用')
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
  max-width: 520px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
  text-align: center;
}

.login-logo {
  font-size: 56px;
  line-height: 1;
  margin-bottom: 12px;
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--app-primary);
  margin-bottom: 10px;
}

.login-sub {
  font-size: 17px;
  color: #5a6672;
  margin-bottom: 28px;
}

.login-button {
  width: 100%;
  margin-top: 22px;
  font-weight: 700;
}
</style>

