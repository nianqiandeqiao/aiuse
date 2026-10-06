<template>
  <el-dialog
    v-model="visible"
    width="620px"
    :close-on-click-modal="false"
    :show-close="false"
    align-center
    class="reminder-dialog"
  >
    <div class="reminder-body">
      <div class="reminder-clock">⏰ {{ current.remindTime }}</div>
      <div class="reminder-title">{{ elderName }}，该吃药了</div>
      <div class="reminder-drug">{{ current.drugName }}</div>
      <div class="reminder-detail">
        {{ current.dosage || '' }}<span v-if="current.takeRelation"> · {{ current.takeRelation }}</span>
      </div>

      <div class="reminder-actions">
        <el-button type="primary" size="large" class="big-btn" :loading="saving" @click="confirmTaken">
          已经吃了
        </el-button>
        <el-button size="large" class="big-btn" @click="snooze">等一会儿</el-button>
        <el-button
          size="large"
          class="big-btn"
          :type="listening ? 'danger' : 'default'"
          @click="startVoice"
        >
          {{ listening ? '正在听…' : '🎤 我要说话' }}
        </el-button>
      </div>

      <div class="reminder-hint">{{ hint }}</div>
      <div v-if="!secureContext" class="reminder-warn">
        当前用 http 打开，浏览器不允许用麦克风；请改用 http://localhost:8081 或 https 网址，
        也可以直接点「已经吃了」。
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { asrRecognize, checkin, todayDoses } from '../api'
import { startRecording, stopRecording } from '../utils/asr'
import { useCurrentElder } from '../stores/currentElder'
import { speak } from '../utils/speech'

const { state } = useCurrentElder()

const visible = ref(false)
const current = ref({})
const saving = ref(false)
const listening = ref(false)
const hint = ref('到点提醒：可以直接说「吃了」或者「等一会儿」')
const secureContext = ref(true)

/** 被推迟的提醒：key -> 到什么时候再提醒 */
const snoozed = reactive({})

const POLL_MS = 30000
const SNOOZE_MS = 10 * 60 * 1000
let timer = null

function todayStr() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

function currentTimeStr() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(now.getHours())}:${pad(now.getMinutes())}`
}

async function poll() {
  if (visible.value || !state.elder) {
    return
  }
  let summary
  try {
    summary = await todayDoses(state.elder.id, todayStr())
  } catch (e) {
    return
  }
  const now = currentTimeStr()
  const due = (summary.items || []).find((item) => {
    if (item.taken) return false
    if (item.remindTime > now) return false
    const key = `${item.planId}-${item.remindTime}`
    const until = snoozed[key]
    if (until && Date.now() < until) return false
    return true
  })
  if (due) {
    current.value = due
    visible.value = true
    const text = `${state.elder.name}，该吃药了。请服用${due.drugName}，${due.dosage || ''}`
      + (due.takeRelation ? `，${due.takeRelation}` : '')
    speak(text)
  }
}

async function confirmTaken() {
  saving.value = true
  try {
    await checkin({
      elderId: state.elder.id,
      planId: current.value.planId,
      takeDate: todayStr(),
      remindTime: current.value.remindTime,
      status: 'TAKEN'
    })
    ElMessage.success('已记录服药')
    speak('好的，已经记下您吃药了')
    visible.value = false
  } finally {
    saving.value = false
  }
}

function snooze() {
  const key = `${current.value.planId}-${current.value.remindTime}`
  snoozed[key] = Date.now() + SNOOZE_MS
  hint.value = '好的，十分钟后再提醒您'
  speak('好的，十分钟之后再提醒您')
  visible.value = false
}

let recordTimer = null

/** 录音 5 秒后自动上传识别（浏览器自带识别依赖谷歌服务，国内用不了） */
async function startVoice() {
  if (listening.value) {
    return
  }
  const started = await startRecording((message) => {
    hint.value = message
  })
  if (!started) {
    return
  }
  listening.value = true
  hint.value = '请说：吃了 / 等一会儿（说完会自动识别）'
  recordTimer = setTimeout(finishVoice, 5000)
}

async function finishVoice() {
  if (recordTimer) {
    clearTimeout(recordTimer)
    recordTimer = null
  }
  listening.value = false
  const audio = await stopRecording()
  if (!audio) {
    hint.value = '没有录到声音，请再试一次'
    return
  }
  try {
    const result = await asrRecognize(audio)
    const text = result.text
    hint.value = `听到：${text}`
    if (/吃|服|好|中|行|完/.test(text)) {
      confirmTaken()
    } else if (/等|一会|待会|待会儿|稍后/.test(text)) {
      snooze()
    } else if (/没|不/.test(text)) {
      hint.value = '好的，请您尽快服药，有不舒服告诉家里人'
      speak('好的，请您尽快服药，有不舒服告诉家里人')
    } else {
      hint.value = '没听清，您可以说「吃了」或者「等一会儿」'
    }
  } catch (e) {
    // 具体原因由请求拦截器提示
  }
}

onMounted(() => {
  secureContext.value = window.isSecureContext
  timer = setInterval(poll, POLL_MS)
  poll()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.reminder-body {
  text-align: center;
  padding: 6px 8px 12px;
}

.reminder-clock {
  font-size: 26px;
  font-weight: 700;
  color: var(--app-primary);
}

.reminder-title {
  font-size: 34px;
  font-weight: 700;
  margin: 12px 0;
}

.reminder-drug {
  font-size: 30px;
  font-weight: 700;
  color: var(--app-danger);
}

.reminder-detail {
  font-size: 20px;
  color: #40505f;
  margin-top: 8px;
}

.reminder-actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 26px 0 14px;
}

.big-btn {
  height: 68px;
  font-size: 24px;
  font-weight: 700;
  border-radius: 12px;
}

.reminder-hint {
  font-size: 18px;
  color: #5a6672;
  min-height: 28px;
}

.reminder-warn {
  margin-top: 10px;
  font-size: 16px;
  line-height: 1.7;
  color: #b26a00;
  background: #fff7e6;
  border-radius: 10px;
  padding: 10px 14px;
}
</style>
