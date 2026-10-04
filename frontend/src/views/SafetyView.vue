<template>
  <div class="page">
    <div class="page-title">用药安全提醒</div>

    <div class="card-block">
      <div class="toolbar">
        <span class="label">老人：</span>
        <el-select v-model="elderId" placeholder="请选择老人" size="large" style="width: 220px" @change="onElderChange">
          <el-option v-for="e in elders" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
        <el-button type="danger" size="large" :loading="checking" :disabled="!elderId" @click="runCheck">
          🔍 检查用药冲突
        </el-button>
      </div>

      <el-alert v-if="!elderId" title="请先选择一位老人" type="info" :closable="false" show-icon />

      <template v-else-if="checked">
        <el-alert
          v-if="result.conflictCount === 0"
          type="success"
          show-icon
          :closable="false"
          :title="`未发现用药冲突（已检查 ${result.drugCount} 种药品）`"
        />
        <el-alert
          v-else
          :type="result.maxSeverity === 1 ? 'error' : 'warning'"
          show-icon
          :closable="false"
          :title="`发现 ${result.conflictCount} 条用药风险，最高等级：${result.maxSeverityLabel}`"
        />

        <div class="drug-line muted">
          本次检查的药品：{{ (result.drugs || []).join('、') }}
        </div>

        <div
          v-for="(conflict, index) in result.conflicts"
          :key="index"
          class="severity-card"
          :class="'severity-' + conflict.severity"
        >
          <div class="conflict-head">
            <span class="severity-tag" :class="'severity-tag-' + conflict.severity">
              {{ conflict.severityLabel }}
            </span>
            <span class="conflict-drugs">{{ conflict.drugA }} + {{ conflict.drugB }}</span>
            <el-tag size="small" type="info" v-if="conflict.type === 'DUPLICATE'">重复用药</el-tag>
          </div>
          <div class="conflict-title">{{ conflict.title }}</div>
          <div class="conflict-row"><b>机制：</b>{{ conflict.description }}</div>
          <div class="conflict-row"><b>建议：</b>{{ conflict.advice }}</div>
          <div class="conflict-row muted"><b>依据：</b>{{ conflict.source }}</div>
        </div>

        <el-button size="large" @click="speakResult">🔊 语音播报结果</el-button>
      </template>
    </div>

    <div class="card-block">
      <h3>相互作用规则库（共 {{ rules.length }} 条）</h3>
      <el-table :data="rules" style="width: 100%" max-height="420">
        <el-table-column prop="drugA" label="药品 A" width="140" />
        <el-table-column prop="drugB" label="药品 B" width="140" />
        <el-table-column label="严重程度" width="130">
          <template #default="{ row }">
            <span class="severity-tag" :class="'severity-tag-' + row.severity">
              {{ row.severityLabel }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="风险" min-width="180" />
        <el-table-column prop="advice" label="建议" min-width="220" />
        <el-table-column prop="source" label="依据来源" min-width="200" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { checkInteractions, listElders, listRules } from '../api'
import { useCurrentElder } from '../stores/currentElder'
import { speak } from '../utils/speech'

const { state } = useCurrentElder()

const elders = ref([])
const elderId = ref(state.elder ? state.elder.id : null)
const rules = ref([])
const result = ref({})
const checked = ref(false)
const checking = ref(false)

async function loadElders() {
  elders.value = (await listElders()) || []
  if (!elderId.value && elders.value.length > 0) {
    elderId.value = elders.value[0].id
  }
}

function onElderChange() {
  checked.value = false
  result.value = {}
}

async function runCheck() {
  checking.value = true
  try {
    const data = await checkInteractions(elderId.value)
    result.value = data
    checked.value = true
    if (data.conflictCount === 0) {
      ElMessage.success('未发现用药冲突')
    } else if (data.maxSeverity === 1) {
      const first = data.conflicts[0]
      ElMessageBox.alert(
        `${first.drugA} + ${first.drugB}：${first.title}\n\n建议：${first.advice}`,
        '⚠️ 严重用药风险',
        { type: 'error', confirmButtonText: '我知道了' }
      )
      speak(`警告，发现严重用药风险，${first.drugA}和${first.drugB}，${first.title}`)
    }
  } finally {
    checking.value = false
  }
}

function speakResult() {
  const data = result.value
  if (!data.conflictCount) {
    speak('未发现用药冲突')
    return
  }
  const first = data.conflicts[0]
  speak(`发现${data.conflictCount}条用药风险，最高等级${data.maxSeverityLabel}。${first.drugA}和${first.drugB}，${first.title}。建议${first.advice}`)
}

onMounted(async () => {
  await loadElders()
  rules.value = (await listRules()) || []
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

.drug-line {
  margin: 16px 0;
}

.conflict-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.conflict-drugs {
  font-size: 22px;
  font-weight: 700;
}

.conflict-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 10px;
}

.conflict-row {
  margin-bottom: 6px;
  font-size: 17px;
}
</style>

