<template>
  <div class="page">
    <div class="page-title">今日打卡</div>

    <div class="card-block">
      <div class="toolbar">
        <span class="label">老人：</span>
        <el-select v-model="elderId" placeholder="请选择老人" size="large" style="width: 220px" @change="onElderChange">
          <el-option v-for="e in elders" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
        <span class="label">日期：</span>
        <el-date-picker v-model="date" type="date" size="large" value-format="YYYY-MM-DD" @change="loadAll" />
        <el-button size="large" @click="speakSummary">🔊 语音播报</el-button>
      </div>

      <el-alert v-if="!elderId" title="请先选择一位老人" type="info" :closable="false" show-icon />

      <template v-else>
        <div class="progress-line">
          <el-progress
            :percentage="percentage"
            :stroke-width="26"
            :format="() => `已服 ${summary.taken} / ${summary.total} 次`"
          />
        </div>

        <div v-loading="loading">
          <el-empty v-if="!loading && summary.total === 0" description="这一天没有需要服用的药" />

          <div
            v-for="item in summary.items"
            :key="item.planId + '-' + item.remindTime"
            class="dose-item"
            :class="{ 'is-taken': item.taken }"
          >
            <div class="dose-time">{{ item.remindTime }}</div>
            <div class="dose-main">
              <div class="dose-drug">{{ item.drugName }}</div>
              <div class="muted">
                {{ item.dosage }} · {{ item.takeRelation || '服用时机未填' }} ·
                {{ item.frequency || '频次未填' }}
              </div>
            </div>
            <el-tag v-if="item.taken" type="success" effect="dark" size="large">已服用</el-tag>
            <el-tag v-else type="warning" effect="dark" size="large">待服用</el-tag>
            <el-button
              v-if="!item.taken"
              type="primary"
              size="large"
              @click="doCheckin(item)"
            >
              打卡
            </el-button>
            <el-button v-else type="danger" size="large" plain @click="undo(item)">撤销</el-button>
          </div>
        </div>
      </template>
    </div>

    <div v-if="elderId" class="card-block">
      <h3>最近 7 天按时率</h3>
      <el-row :gutter="16">
        <el-col :xs="12" :md="6">
          <div class="stat"><span class="stat-num">{{ stats.expected ?? 0 }}</span><span>应服次数</span></div>
        </el-col>
        <el-col :xs="12" :md="6">
          <div class="stat"><span class="stat-num">{{ stats.taken ?? 0 }}</span><span>已服次数</span></div>
        </el-col>
        <el-col :xs="12" :md="6">
          <div class="stat"><span class="stat-num">{{ stats.missed ?? 0 }}</span><span>漏服次数</span></div>
        </el-col>
        <el-col :xs="12" :md="6">
          <div class="stat">
            <span class="stat-num">{{ ratePercent }}%</span><span>按时率</span>
          </div>
        </el-col>
      </el-row>
      <div class="muted">统计区间：{{ stats.from }} ~ {{ stats.to }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { checkin, checkinStats, listElders, todayDoses, undoCheckin } from '../api'
import { useCurrentElder } from '../stores/currentElder'
import { speak } from '../utils/speech'

const { state, setElder } = useCurrentElder()

const today = new Date().toISOString().slice(0, 10)
const elders = ref([])
const elderId = ref(state.elder ? state.elder.id : null)
const date = ref(today)
const loading = ref(false)
const summary = ref({ total: 0, taken: 0, pending: 0, items: [] })
const stats = ref({})

const percentage = computed(() =>
  summary.value.total === 0 ? 0 : Math.round((summary.value.taken / summary.value.total) * 100)
)

const ratePercent = computed(() => Math.round(((stats.value.rate ?? 0) * 100 + Number.EPSILON) * 10) / 10)

async function loadElders() {
  elders.value = (await listElders()) || []
  if (!elderId.value && elders.value.length > 0) {
    elderId.value = elders.value[0].id
    syncCurrentElder()
  }
}

function syncCurrentElder() {
  const found = elders.value.find((e) => e.id === elderId.value)
  if (found) setElder(found)
}

async function loadAll() {
  if (!elderId.value) return
  loading.value = true
  try {
    summary.value = (await todayDoses(elderId.value, date.value)) || { total: 0, taken: 0, items: [] }
    const from = shiftDate(date.value, -6)
    stats.value = (await checkinStats(elderId.value, from, date.value)) || {}
  } finally {
    loading.value = false
  }
}

function shiftDate(dateStr, days) {
  const d = new Date(dateStr)
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}

async function onElderChange() {
  syncCurrentElder()
  await loadAll()
}

async function doCheckin(item) {
  await checkin({
    elderId: elderId.value,
    planId: item.planId,
    takeDate: date.value,
    remindTime: item.remindTime,
    status: 'TAKEN'
  })
  ElMessage.success(`${item.drugName} 打卡成功`)
  speak(`${item.drugName}，打卡成功`)
  await loadAll()
}

async function undo(item) {
  try {
    await ElMessageBox.confirm(`确定撤销「${item.drugName}」${item.remindTime} 的打卡吗？`, '撤销确认', {
      type: 'warning',
      confirmButtonText: '确定撤销',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  await undoCheckin(item.logId)
  ElMessage.success('已撤销')
  await loadAll()
}

function speakSummary() {
  const elderName = elders.value.find((e) => e.id === elderId.value)?.name || ''
  const text = summary.value.total === 0
    ? `${elderName}，今天没有需要服用的药`
    : `${elderName}，今天共需服药${summary.value.total}次，已完成${summary.value.taken}次，待服用${summary.value.pending}次`
  speak(text)
}

onMounted(async () => {
  await loadElders()
  await loadAll()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.label {
  font-weight: 600;
}

.progress-line {
  margin-bottom: 20px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  background: var(--app-primary-light);
  border-radius: var(--app-radius);
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: var(--app-primary);
}
</style>

