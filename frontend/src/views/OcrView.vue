<template>
  <div class="page">
    <div class="page-title">拍照识药</div>

    <div class="card-block">
      <h3>第一步：上传或拍摄药盒照片</h3>
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept="image/*"
        :on-change="onFileChange"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">把药盒照片拖到这里，或 <em>点击选择 / 拍照</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 jpg / png，建议单张不超过 4MB，文字拍清楚一些识别率更高</div>
        </template>
      </el-upload>

      <div v-if="previewUrl" class="preview-area">
        <img :src="previewUrl" alt="药盒预览" class="preview-img" />
        <el-button type="primary" size="large" :loading="recognizing" @click="startRecognize">
          开始识别
        </el-button>
      </div>
    </div>

    <div v-if="result" class="card-block">
      <h3>第二步：识别结果</h3>

      <el-alert
        v-if="result.drugs.length === 0"
        type="warning"
        :closable="false"
        show-icon
        title="没有识别出已知药品"
        description="可以换一张更清晰的照片；也可以手动到「用药计划」页面添加药品。"
      />

      <template v-else>
        <div class="drug-tags">
          <el-tag
            v-for="drug in result.drugs"
            :key="drug.standardName"
            type="success"
            effect="dark"
            size="large"
          >
            {{ drug.standardName }}（{{ drug.category }}）
          </el-tag>
        </div>

        <el-collapse>
          <el-collapse-item title="查看识别出的原始文字" name="raw">
            <pre class="raw-text">{{ result.rawText }}</pre>
          </el-collapse-item>
        </el-collapse>

        <h3 style="margin-top: 24px">第三步：加入用药计划</h3>
        <el-form label-width="130px">
          <el-form-item label="给哪位老人">
            <el-select v-model="planForm.elderId" size="large" placeholder="请选择老人" style="width: 260px">
              <el-option v-for="e in elders" :key="e.id" :label="e.name" :value="e.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="要加入的药">
            <el-checkbox-group v-model="planForm.selected">
              <el-checkbox
                v-for="drug in result.drugs"
                :key="drug.standardName"
                :value="drug.standardName"
                size="large"
              >
                {{ drug.standardName }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="剂量">
            <el-input v-model="planForm.dosage" size="large" style="width: 220px" placeholder="如：1片" />
          </el-form-item>
          <el-form-item label="服用时机">
            <el-radio-group v-model="planForm.takeRelation" size="large">
              <el-radio value="饭前">饭前</el-radio>
              <el-radio value="饭后">饭后</el-radio>
              <el-radio value="睡前">睡前</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="提醒时间">
            <el-time-select
              v-model="planForm.remindTime"
              start="05:00"
              step="00:30"
              end="23:00"
              size="large"
              placeholder="选择提醒时间"
            />
          </el-form-item>
        </el-form>
        <el-button
          type="success"
          size="large"
          :loading="saving"
          :disabled="!planForm.elderId || planForm.selected.length === 0"
          @click="addToPlans"
        >
          加入用药计划
        </el-button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createPlan, listElders, ocrDrug } from '../api'
import { useCurrentElder } from '../stores/currentElder'
import { speak } from '../utils/speech'

const { state } = useCurrentElder()

const elders = ref([])
const file = ref(null)
const previewUrl = ref('')
const recognizing = ref(false)
const saving = ref(false)
const result = ref(null)

const planForm = reactive({
  elderId: state.elder ? state.elder.id : null,
  selected: [],
  dosage: '1片',
  takeRelation: '饭后',
  remindTime: '08:00'
})

function onFileChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  if (!raw.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }
  file.value = raw
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(raw)
  result.value = null
}

async function startRecognize() {
  if (!file.value) {
    ElMessage.warning('请先选择药盒照片')
    return
  }
  recognizing.value = true
  try {
    const data = await ocrDrug(file.value)
    result.value = data
    planForm.selected = (data.drugs || []).map((d) => d.standardName)
    if (data.drugs && data.drugs.length > 0) {
      ElMessage.success(`识别到 ${data.drugs.length} 种药品`)
      speak(`识别到${data.drugs.map((d) => d.standardName).join('、')}`)
    }
  } finally {
    recognizing.value = false
  }
}

async function addToPlans() {
  saving.value = true
  try {
    for (const drugName of planForm.selected) {
      await createPlan({
        elderId: planForm.elderId,
        drugName,
        dosage: planForm.dosage,
        frequency: '每日1次',
        takeRelation: planForm.takeRelation,
        remindTimes: planForm.remindTime,
        startDate: new Date().toISOString().slice(0, 10)
      })
    }
    ElMessage.success('已加入用药计划，可到「用药安全」页面检查冲突')
    speak('已加入用药计划，建议检查用药冲突')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  elders.value = (await listElders()) || []
})
</script>

<style scoped>
.preview-area {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-start;
}

.preview-img {
  max-width: 420px;
  max-height: 320px;
  border-radius: var(--app-radius);
  border: 2px solid #dfe4ea;
}

.drug-tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.raw-text {
  white-space: pre-wrap;
  font-size: 17px;
  background: #f7f8fa;
  padding: 14px;
  border-radius: 8px;
  margin: 0;
}
</style>

