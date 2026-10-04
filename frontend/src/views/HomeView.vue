<template>
  <div class="page">
    <div class="page-title">请选择要照护的老人</div>

    <div v-loading="loading">
      <el-empty v-if="!loading && elders.length === 0" description="还没有老人档案，先添加一位吧">
        <el-button type="primary" size="large" @click="$router.push('/elders')">
          去添加老人档案
        </el-button>
      </el-empty>

      <el-row v-else :gutter="20">
        <el-col v-for="elder in elders" :key="elder.id" :xs="24" :sm="12" :md="8">
          <div
            class="card-block elder-card"
            :class="{ 'is-active': state.elder && state.elder.id === elder.id }"
            @click="selectElder(elder)"
          >
            <div class="elder-name">
              {{ elder.name }}
              <el-tag v-if="state.elder && state.elder.id === elder.id" type="success" effect="dark">
                当前
              </el-tag>
            </div>
            <div class="elder-meta">年龄：{{ elder.age ?? '未填写' }} 岁</div>
            <div class="elder-meta">基础疾病：{{ elder.chronicDisease || '无' }}</div>
            <div class="elder-meta">联系电话：{{ elder.phone || '未填写' }}</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="card-block">
      <h3>要做什么？</h3>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="6">
          <el-button class="entry-button" type="primary" @click="go('/ocr')">
            📷 拍照识药
          </el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-button class="entry-button" type="success" @click="go('/checkin')">
            ✅ 今日打卡
          </el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-button class="entry-button" type="warning" @click="go('/safety')">
            ⚠️ 用药安全
          </el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-button class="entry-button entry-button--plan" @click="go('/plans')">
            💊 用药计划
          </el-button>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listElders } from '../api'
import { useCurrentElder } from '../stores/currentElder'
import { speak } from '../utils/speech'

const router = useRouter()
const { state, setElder } = useCurrentElder()

const elders = ref([])
const loading = ref(false)

async function loadElders() {
  loading.value = true
  try {
    elders.value = (await listElders()) || []
  } finally {
    loading.value = false
  }
}

function selectElder(elder) {
  setElder(elder)
  ElMessage.success(`已选择 ${elder.name}`)
  speak(`已选择${elder.name}`)
}

function go(path) {
  if (!state.elder) {
    ElMessage.warning('请先选择（或添加）一位老人')
    return
  }
  router.push(path)
}

onMounted(loadElders)
</script>

<style scoped>
.elder-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-primary);
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.elder-meta {
  font-size: 17px;
  color: #33404d;
}
</style>
