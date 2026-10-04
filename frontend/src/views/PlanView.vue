<template>
  <div class="page">
    <div class="page-title">用药计划</div>

    <div class="card-block">
      <div class="toolbar">
        <span class="label">选择老人：</span>
        <el-select
          v-model="elderId"
          placeholder="请选择老人"
          size="large"
          style="width: 260px"
          @change="onElderChange"
        >
          <el-option v-for="e in elders" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
        <el-button type="success" size="large" :disabled="!elderId" @click="openCreate">
          ＋ 添加药品
        </el-button>
      </div>

      <el-alert
        v-if="!elderId"
        title="请先在上方选择一位老人"
        type="info"
        :closable="false"
        show-icon
      />

      <el-table v-else v-loading="loading" :data="plans" style="width: 100%" empty-text="该老人还没有用药计划">
        <el-table-column prop="drugName" label="药品名称" min-width="150" />
        <el-table-column prop="dosage" label="剂量" width="110" />
        <el-table-column prop="frequency" label="频次" width="130" />
        <el-table-column prop="takeRelation" label="服用时机" width="130" />
        <el-table-column prop="remindTimes" label="提醒时间" min-width="150" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark">
              {{ row.status === 1 ? '进行中' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">修改</el-button>
            <el-button type="warning" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '恢复' }}
            </el-button>
            <el-button type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改用药计划' : '添加药品'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="药品名称" prop="drugName">
          <el-input v-model="form.drugName" size="large" placeholder="如：阿司匹林肠溶片" />
        </el-form-item>
        <el-form-item label="剂量" prop="dosage">
          <el-input v-model="form.dosage" size="large" placeholder="如：1片 / 半片 / 5ml" />
        </el-form-item>
        <el-form-item label="服用频次">
          <el-select
            v-model="form.frequency"
            size="large"
            filterable
            allow-create
            default-first-option
            placeholder="可选择或直接输入"
            style="width: 100%"
          >
            <el-option v-for="item in frequencyOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="服用时机">
          <el-radio-group v-model="form.takeRelation" size="large">
            <el-radio value="饭前">饭前</el-radio>
            <el-radio value="饭后">饭后</el-radio>
            <el-radio value="睡前">睡前</el-radio>
            <el-radio value="空腹">空腹</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="提醒时间">
          <el-select
            v-model="form.remindTimeList"
            multiple
            size="large"
            filterable
            allow-create
            default-first-option
            placeholder="可多选，也可输入 07:30 这种格式"
            style="width: 100%"
          >
            <el-option v-for="item in timeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" size="large" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            size="large"
            value-format="YYYY-MM-DD"
            placeholder="不填表示长期服用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="large" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="large" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createPlan,
  deletePlan,
  listElders,
  listPlans,
  updatePlan,
  updatePlanStatus
} from '../api'
import { useCurrentElder } from '../stores/currentElder'
import { todayStr } from '../utils/date'

const { state, setElder } = useCurrentElder()

const elders = ref([])
const plans = ref([])
const elderId = ref(state.elder ? state.elder.id : null)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()

const frequencyOptions = ['每日1次', '每日2次', '每日3次', '隔日1次', '需要时服用']
const timeOptions = ['06:00', '08:00', '12:00', '18:00', '20:00', '22:00']

const emptyForm = () => ({
  id: null,
  drugName: '',
  dosage: '',
  frequency: '每日1次',
  takeRelation: '饭后',
  remindTimeList: ['08:00'],
  startDate: todayStr(),
  endDate: null
})

const form = reactive(emptyForm())

const rules = {
  drugName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  dosage: [{ required: true, message: '请输入剂量，如 1片', trigger: 'blur' }]
}

async function loadElders() {
  elders.value = (await listElders()) || []
  if (!elderId.value && elders.value.length > 0) {
    elderId.value = elders.value[0].id
    syncCurrentElder()
    await loadPlans()
  }
}

function syncCurrentElder() {
  const found = elders.value.find((e) => e.id === elderId.value)
  if (found) {
    setElder(found)
  }
}

async function loadPlans() {
  if (!elderId.value) {
    plans.value = []
    return
  }
  loading.value = true
  try {
    plans.value = (await listPlans(elderId.value)) || []
  } finally {
    loading.value = false
  }
}

async function onElderChange() {
  syncCurrentElder()
  await loadPlans()
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    drugName: row.drugName,
    dosage: row.dosage,
    frequency: row.frequency || '每日1次',
    takeRelation: row.takeRelation || '饭后',
    remindTimeList: row.remindTimes ? row.remindTimes.split(',').map((t) => t.trim()) : ['08:00'],
    startDate: row.startDate || null,
    endDate: row.endDate || null
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  const payload = {
    drugName: form.drugName,
    dosage: form.dosage,
    frequency: form.frequency,
    takeRelation: form.takeRelation,
    remindTimes: (form.remindTimeList || []).filter(Boolean).join(','),
    startDate: form.startDate,
    endDate: form.endDate
  }
  saving.value = true
  try {
    if (form.id) {
      await updatePlan(form.id, payload)
      ElMessage.success('用药计划已更新')
    } else {
      await createPlan({ ...payload, elderId: elderId.value })
      ElMessage.success('药品已添加')
    }
    dialogVisible.value = false
    await loadPlans()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await updatePlanStatus(row.id, next)
  ElMessage.success(next === 1 ? '已恢复用药' : '已停用该药品')
  await loadPlans()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.drugName}」这条用药计划吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  await deletePlan(row.id)
  ElMessage.success('已删除')
  await loadPlans()
}

onMounted(async () => {
  await loadElders()
  await loadPlans()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.label {
  font-weight: 600;
}
</style>
