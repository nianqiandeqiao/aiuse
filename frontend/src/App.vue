<template>
  <router-view v-if="isLoginPage" />

  <el-container v-else class="app-shell">
    <el-header class="app-header">
      <div class="brand">
        <span class="brand-logo">💊</span>
        <div>
          <div class="brand-title">AI 用药提醒与用药安全助手</div>
          <div class="brand-sub">适老化设计 · 拍照识药 · 用药冲突提醒</div>
        </div>
      </div>
      <div class="brand-right">
        <el-tag v-if="state.elder" type="success" size="large" effect="dark">
          当前老人：{{ state.elder.name }}
        </el-tag>
        <el-tag v-else type="info" size="large">尚未选择老人</el-tag>
        <el-switch
          v-model="muted"
          size="large"
          active-text="静音"
          inactive-text="语音开启"
          @change="onMuteChange"
        />
        <el-button v-if="auth.required" size="large" @click="onLogout">退出</el-button>
      </div>
    </el-header>

    <el-menu :default-active="activeMenu" mode="horizontal" router class="app-menu">
      <el-menu-item index="/home">首页</el-menu-item>
      <el-menu-item index="/elders">老人档案</el-menu-item>
      <el-menu-item index="/plans">用药计划</el-menu-item>
      <el-menu-item index="/checkin">今日打卡</el-menu-item>
      <el-menu-item index="/ocr">拍照识药</el-menu-item>
      <el-menu-item index="/safety">用药安全</el-menu-item>
    </el-menu>

    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCurrentElder } from './stores/currentElder'
import { isMuted, setMuted, speak } from './utils/speech'
import { ensureAuthChecked, markLoggedOut, useAuth } from './stores/auth'
import { logout } from './api'

const route = useRoute()
const router = useRouter()
const { state } = useCurrentElder()
const auth = useAuth()

const muted = ref(isMuted())
const activeMenu = computed(() => route.path)
const isLoginPage = computed(() => route.path === '/login')

onMounted(async () => {
  const status = await ensureAuthChecked()
  if (status.required && !status.loggedIn && !isLoginPage.value) {
    router.replace('/login')
  }
})

async function onLogout() {
  try {
    await logout()
  } finally {
    markLoggedOut()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}

function onMuteChange(value) {
  setMuted(value)
  if (!value) {
    speak('语音播报已开启')
  }
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: auto;
  padding: 18px 24px;
  background: var(--app-primary);
  color: #fff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-logo {
  font-size: 40px;
}

.brand-title {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.4;
}

.brand-sub {
  font-size: 16px;
  opacity: 0.85;
  line-height: 1.4;
}

.brand-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.app-menu {
  font-size: var(--app-font-size);
  padding: 0 12px;
  background: #fff;
}

.app-menu :deep(.el-menu-item) {
  font-size: var(--app-font-size);
  height: 64px;
  line-height: 64px;
  font-weight: 600;
}

.app-main {
  padding: 0;
  background: var(--app-bg);
}
</style>
