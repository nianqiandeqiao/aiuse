<template>
  <div class="page">
    <div class="page-title">老人档案</div>

    <div class="card-block">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="输入姓名 / 电话 / 基础疾病搜索"
          size="large"
          clearable
          style="max-width: 420px"
          @keyup.enter="load"
          @clear="load"
        />
        <el-button type="primary" size="large" @click="load">搜索</el-button>
        <el-button type="success" size="large" @click="openCreate">＋ 添加老人</el-button>
      </div>

      <el-table v-loading="loading" :data="elders" style="width: 100%" empty-text="暂无数据">
        <el-table-column prop="name" label="姓名" width="140" />
        <el-table-column prop="age" label="年龄" width="100" />
        <el-table-column prop="gender" label="性别" width="100">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="180" />
        <el-table-column prop="chronicDisease" label="基础疾病" min-width="180" />
        <el-table-column prop="note" label="备注" min-width="140" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">修改</el-button>
            <el-button type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改老人档案' : '添加老人档案'" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" size="large" placeholder="请输入老人姓名" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="0" :max="130" size="large" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender" size="large">
            <el-radio value="M">男</el-radio>
            <el-radio value="F">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" size="large" placeholder="用于紧急联系" />
        </el-form-item>
        <el-form-item label="基础疾病">
          <el-input v-model="form.chronicDisease" size="large" placeholder="如：高血压、糖尿病" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" type="textarea" :rows="3" placeholder="如：独居、行动不便" />
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
import { createElder, deleteElder, listElders, updateElder } from '../api'
import { useCurrentElder } from '../stores/currentElder'

const { state, clear } = useCurrentElder()

const elders = ref([])
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null,
  name: '',
  age: 75,
  gender: 'M',
  phone: '',
  chronicDisease: '',
  note: ''
})

const form = reactive(emptyForm())

const rules = {
  name: [
    { required: true, message: '请输入老人姓名', trigger: 'blur' },
    { max: 50, message: '姓名不能超过 50 个字', trigger: 'blur' }
  ],
  age: [{ type: 'number', min: 0, max: 130, message: '年龄应在 0 - 130 之间', trigger: 'change' }]
}

function genderText(gender) {
  if (gender === 'M') return '男'
  if (gender === 'F') return '女'
  return '未填写'
}

async function load() {
  loading.value = true
  try {
    elders.value = (await listElders(keyword.value)) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    age: row.age ?? 75,
    gender: row.gender || 'M',
    phone: row.phone || '',
    chronicDisease: row.chronicDisease || '',
    note: row.note || ''
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name,
      age: form.age,
      gender: form.gender,
      phone: form.phone,
      chronicDisease: form.chronicDisease,
      note: form.note
    }
    if (form.id) {
      await updateElder(form.id, payload)
      ElMessage.success('档案已更新')
    } else {
      await createElder(payload)
      ElMessage.success('老人档案已添加')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.name}」的档案吗？删除后其用药计划也会一并停用。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  await deleteElder(row.id)
  ElMessage.success('已删除')
  if (state.elder && state.elder.id === row.id) {
    clear()
  }
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}
</style>

