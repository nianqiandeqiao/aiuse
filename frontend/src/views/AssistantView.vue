<template>
  <div class="page">
    <div class="page-title">问药师（语音/文字）</div>

    <div class="card-block">
      <div class="toolbar">
        <span class="label">老人：</span>
        <el-select v-model="elderId" size="large" style="width: 190px">
          <el-option v-for="e in elders" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
        <span class="label">说话方式：</span>
        <el-select v-model="dialect" size="large" style="width: 170px">
          <el-option label="河南话" value="henan" />
          <el-option label="普通话" value="mandarin" />
          <el-option label="四川话" value="sichuan" />
          <el-option label="粤语" value="cantonese" />
        </el-select>
        <el-tag :type="aiMode === 'LLM' ? 'success' : 'info'" size="large" effect="dark">
          {{ aiMode === 'LLM' ? '大模型模式' : '内置规则模式' }}
        </el-tag>
      </div>

      <div class="ask-area">
        <el-button
          class="mic-button"
          :type="listening ? 'danger' : 'primary'"
          size="large"
          @click="startVoice"
        >
          {{ listening ? '正在听…请说话' : '🎤 点一下开始说话' }}
        </el-button>
        <el-input
          v-model="question"
          size="large"
          class="ask-input"
          placeholder="也可以直接打字，例如：阿司匹林能和感冒药一起吃吗？"
          @keyup.enter="ask"
        />
        <el-button type="success" size="large" :loading="asking" @click="ask">提问</el-button>
      </div>

      <div class="quick-questions">
        <span class="muted">常用问题：</span>
        <el-button v-for="q in quickQuestions" :key="q" size="large" plain @click="askQuick(q)">
          {{ q }}
        </el-button>
      </div>

      <div v-if="answer" class="answer-card">
        <div class="answer-head">
          <el-tag :type="answer.source === 'LLM' ? 'success' : 'info'" effect="dark">
            {{ answer.source === 'LLM' ? 'AI 大模型回答' : '内置规则回答' }}
          </el-tag>
          <el-button size="large" @click="speak(answer.spokenText || answer.answer)">🔊 读给我听</el-button>
        </div>
        <div class="answer-text">{{ answer.answer }}</div>
        <div v-if="answer.references && answer.references.length" class="answer-refs">
          <div class="muted">依据：</div>
          <div v-for="(ref, i) in answer.references" :key="i" class="muted">· {{ ref }}</div>
        </div>
      </div>
    </div>

    <div class="card-block">
      <h3>拍药盒，让 AI 看说明书</h3>
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept="image/*"
        :on-change="onFileChange"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">把药盒或说明书拍下来，拖到这里，或 <em>点击选择/拍照</em></div>
      </el-upload>

      <div v-if="previewUrl" class="preview-area">
        <img :src="previewUrl" class="preview-img" alt="药盒预览" />
        <el-button type="primary" size="large" :loading="recognizing" @click="analyze">
          识别并让 AI 分析
        </el-button>
      </div>

      <div v-if="analysis" class="answer-card">
        <div class="answer-head">
          <el-tag :type="analysis.source === 'LLM' ? 'success' : 'info'" effect="dark">
            {{ analysis.source === 'LLM' ? 'AI 大模型分析' : '内置规则分析' }}
          </el-tag>
          <el-button size="large" @click="speakAnalysis">🔊 读给我听</el-button>
        </div>
        <div class="analysis-summary">{{ analysis.summary }}</div>
        <div v-if="analysis.expireDate" class="expire-line" :class="{ 'is-expired': analysis.expired }">
          {{ analysis.expired ? '⚠️ 已过期：' : '有效期：' }}{{ analysis.expireDate }}
        </div>
        <div v-for="(advice, i) in analysis.advices" :key="i" class="advice-line">{{ advice }}</div>
        <div v-if="analysis.conflicts && analysis.conflicts.length" class="conflict-box">
          <div class="muted">检测到的用药冲突：</div>
          <div v-for="(c, i) in analysis.conflicts" :key="i" class="conflict-line">
            <el-tag :type="c.severity === 1 ? 'danger' : 'warning'" effect="dark">{{ c.severityLabel }}</el-tag>
            {{ c.drugA }} + {{ c.drugB }}：{{ c.title }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiAnalyzeMedicine, aiAsk, aiStatus, listElders, ocrDrug } from '../api'
import { useCurrentElder } from '../stores/currentElder'
import { speak } from '../utils/speech'

const { state } = useCurrentElder()

const elders = ref([])
const elderId = ref(state.elder ? state.elder.id : null)
const dialect = ref('henan')
const aiMode = ref('RULE')
const question = ref('')
const asking = ref(false)
const listening = ref(false)
const answer = ref(null)

const file = ref(null)
const previewUrl = ref('')
const recognizing = ref(false)
const analysis = ref(null)

const quickQuestions = [
  '这个药能和感冒药一起吃吗',
  '饭前吃还是饭后吃',
  '这个药过期了吗',
  '今天还有几次药没吃'
]

function askQuick(text) {
  question.value = text
  ask()
}

async function ask() {
  if (!elderId.value) {
    ElMessage.warning('请先选择老人')
    return
  }
  if (!question.value.trim()) {
    ElMessage.warning('请说一句话或输入问题')
    return
  }
  asking.value = true
  try {
    answer.value = await aiAsk({ elderId: elderId.value, question: question.value, dialect: dialect.value })
    speak(answer.value.spokenText || answer.value.answer)
  } finally {
    asking.value = false
  }
}

function startVoice() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    ElMessage.warning('当前浏览器不支持语音识别，请用电脑版 Chrome 或 Edge 打开')
    return
  }
  const recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.interimResults = false
  recognition.maxAlternatives = 1
  recognition.onresult = (event) => {
    question.value = event.results[0][0].transcript
    ask()
  }
  recognition.onerror = () => {
    listening.value = false
    ElMessage.warning('没听清，请再说一遍')
  }
  recognition.onend = () => {
    listening.value = false
  }
  recognition.start()
  listening.value = true
}

function onFileChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  file.value = raw
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(raw)
  analysis.value = null
}

async function analyze() {
  if (!elderId.value) {
    ElMessage.warning('请先选择老人')
    return
  }
  if (!file.value) {
    ElMessage.warning('请先拍照或选择药盒图片')
    return
  }
  recognizing.value = true
  try {
    const ocr = await ocrDrug(file.value)
    if (!ocr.rawText) {
      ElMessage.warning('没有识别到文字，请重新拍一张更清晰的照片')
      return
    }
    analysis.value = await aiAnalyzeMedicine({ elderId: elderId.value, text: ocr.rawText })
    speakAnalysis()
  } finally {
    recognizing.value = false
  }
}

function speakAnalysis() {
  if (!analysis.value) return
  const parts = [analysis.value.summary]
  if (analysis.value.expired) parts.push('这盒药已经过期，请不要再吃了')
  parts.push(...(analysis.value.advices || []))
  speak(parts.join('。'))
}

onMounted(async () => {
  elders.value = (await listElders()) || []
  if (!elderId.value && elders.value.length > 0) {
    elderId.value = elders.value[0].id
  }
  const status = await aiStatus()
  aiMode.value = status.mode
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

.ask-area {
  display: flex;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
}

.mic-button {
  min-width: 260px;
  height: 72px;
  font-size: 22px;
  font-weight: 700;
  border-radius: 999px;
}

.ask-input {
  flex: 1;
  min-width: 260px;
}

.quick-questions {
  margin-top: 18px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.answer-card {
  margin-top: 22px;
  background: #f7fafd;
  border: 2px solid #d6e3f0;
  border-radius: var(--app-radius);
  padding: 22px;
}

.answer-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.answer-text {
  font-size: 21px;
  line-height: 1.9;
  white-space: pre-wrap;
}

.answer-refs {
  margin-top: 14px;
  font-size: 16px;
}

.preview-area {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: flex-start;
}

.preview-img {
  max-width: 420px;
  max-height: 300px;
  border-radius: var(--app-radius);
  border: 2px solid #dfe4ea;
}

.analysis-summary {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 10px;
}

.expire-line {
  font-size: 20px;
  color: var(--app-primary);
  font-weight: 700;
  margin-bottom: 10px;
}

.expire-line.is-expired {
  color: var(--app-danger);
}

.advice-line {
  font-size: 19px;
  line-height: 1.8;
  margin-bottom: 6px;
}

.conflict-box {
  margin-top: 14px;
}

.conflict-line {
  font-size: 18px;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
