<template>
  <div class="page">
    <div class="page-title">服药报告</div>

    <div class="card-block">
      <div class="toolbar">
        <span class="label">老人：</span>
        <el-select v-model="elderId" size="large" style="width: 200px" @change="load">
          <el-option v-for="e in elders" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
        <el-radio-group v-model="period" size="large" @change="load">
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
        </el-radio-group>
        <el-date-picker v-model="anchor" type="date" size="large" value-format="YYYY-MM-DD" @change="load" />
      </div>

      <el-alert v-if="!elderId" title="请先选择一位老人" type="info" :closable="false" show-icon />

      <template v-else>
        <el-row :gutter="16" class="stat-row">
          <el-col :xs="12" :md="6">
            <div class="stat">
              <span class="stat-num">{{ report.taken ?? 0 }}</span>
              <span class="stat-label">已服（次）</span>
            </div>
          </el-col>
          <el-col :xs="12" :md="6">
            <div class="stat stat-missed">
              <span class="stat-num">{{ report.missed ?? 0 }}</span>
              <span class="stat-label">漏服（次）</span>
            </div>
          </el-col>
          <el-col :xs="12" :md="6">
            <div class="stat">
              <span class="stat-num">{{ ratePercent }}%</span>
              <span class="stat-label">依从性</span>
            </div>
          </el-col>
          <el-col :xs="12" :md="6">
            <div class="stat">
              <span class="stat-num">{{ report.missedDays ?? 0 }}</span>
              <span class="stat-label">有漏服的天数</span>
            </div>
          </el-col>
        </el-row>

        <div class="summary-line">{{ report.summary }}</div>

        <div ref="chartRef" class="chart"></div>
      </template>
    </div>

    <div v-if="elderId" class="card-block">
      <h3>按药品看依从性（给医生看）</h3>
      <el-table :data="report.byDrug || []" style="width: 100%" empty-text="该区间没有用药记录">
        <el-table-column prop="drugName" label="药品" min-width="150" />
        <el-table-column prop="dosage" label="剂量" width="120" />
        <el-table-column prop="expected" label="应服" width="100" />
        <el-table-column prop="taken" label="已服" width="100" />
        <el-table-column label="漏服" width="100">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.missed > 0 }">{{ row.missed }}</span>
          </template>
        </el-table-column>
        <el-table-column label="依从性" min-width="160">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.rate * 100)" :stroke-width="18" />
          </template>
        </el-table-column>
      </el-table>
      <div class="muted tip">
        统计区间：{{ report.from }} ~ {{ report.to }}；依从性 = 已服次数 ÷ 应服次数。医生可据此判断用药执行情况。
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { adherenceReport, listElders } from '../api'
import { useCurrentElder } from '../stores/currentElder'
import { todayStr } from '../utils/date'

const { state } = useCurrentElder()

const elders = ref([])
const elderId = ref(state.elder ? state.elder.id : null)
const period = ref('week')
const anchor = ref(todayStr())
const report = ref({})
const chartRef = ref(null)
let chart = null

const ratePercent = computed(() => Math.round(((report.value.rate ?? 0) * 100 + Number.EPSILON) * 10) / 10)

async function loadElders() {
  elders.value = (await listElders()) || []
  if (!elderId.value && elders.value.length > 0) {
    elderId.value = elders.value[0].id
  }
}

async function load() {
  if (!elderId.value) return
  report.value = (await adherenceReport(elderId.value, period.value, anchor.value)) || {}
  await nextTick()
  renderChart()
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const daily = report.value.daily || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['应服', '已服'], textStyle: { fontSize: 16 } },
    grid: { left: 50, right: 24, top: 50, bottom: 40 },
    xAxis: {
      type: 'category',
      data: daily.map((d) => d.date.slice(5)),
      axisLabel: { fontSize: 14 }
    },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 14 } },
    series: [
      {
        name: '应服',
        type: 'bar',
        data: daily.map((d) => d.expected),
        itemStyle: { color: '#c8d6e5' },
        barMaxWidth: 40
      },
      {
        name: '已服',
        type: 'bar',
        data: daily.map((d) => d.taken),
        itemStyle: { color: '#1f4e79' },
        barMaxWidth: 40
      }
    ]
  })
}

function onResize() {
  if (chart) chart.resize()
}

onMounted(async () => {
  await loadElders()
  await load()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

.label {
  font-weight: 600;
}

.stat-row {
  margin-bottom: 18px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 8px;
  background: var(--app-primary-light);
  border-radius: var(--app-radius);
  margin-bottom: 12px;
}

.stat-missed {
  background: #fdecea;
}

.stat-missed .stat-num {
  color: var(--app-danger);
}

.stat-num {
  font-size: 40px;
  font-weight: 700;
  color: var(--app-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 16px;
  color: #40505f;
}

.summary-line {
  font-size: 20px;
  font-weight: 600;
  margin: 10px 0 18px;
}

.chart {
  width: 100%;
  height: 340px;
}

.tip {
  margin-top: 14px;
}

.text-danger {
  color: var(--app-danger);
  font-weight: 700;
}
</style>

