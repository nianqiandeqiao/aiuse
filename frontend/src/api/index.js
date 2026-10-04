import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一请求封装：
 * - 后端统一返回 {code, message, data}，code=0 成功
 * - 这里直接把 data 解包出来，业务代码拿到的就是数据本身
 * - 出错时统一弹出中文提示，页面不用重复写错误处理
 */
const http = axios.create({
  baseURL: '/api',
  timeout: 60000
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 0) {
        return body.data
      }
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message || '操作失败'))
    }
    return body
  },
  (error) => {
    if (error?.response?.status === 401) {
      ElMessage.warning('未登录或登录已过期，请重新输入访问口令')
      if (!window.location.hash.startsWith('#/login')) {
        window.location.hash = '#/login'
      }
      return Promise.reject(error)
    }
    const message = error?.response ? `请求失败（HTTP ${error.response.status}）` : '网络异常，请确认后端服务已启动'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/* ---------------- 访问口令 ---------------- */
export const authStatus = () => http.get('/auth/status')
export const login = (password) => http.post('/auth/login', { password })
export const logout = () => http.post('/auth/logout')

/* ---------------- 健康检查 ---------------- */
export const getHealth = () => http.get('/health')

/* ---------------- 老人档案 ---------------- */
export const listElders = (keyword) => http.get('/elders', { params: keyword ? { keyword } : {} })
export const getElder = (id) => http.get(`/elders/${id}`)
export const createElder = (data) => http.post('/elders', data)
export const updateElder = (id, data) => http.put(`/elders/${id}`, data)
export const deleteElder = (id) => http.delete(`/elders/${id}`)

/* ---------------- 用药计划 ---------------- */
export const listPlans = (elderId) => http.get('/plans', { params: { elderId } })
export const createPlan = (data) => http.post('/plans', data)
export const updatePlan = (id, data) => http.put(`/plans/${id}`, data)
export const updatePlanStatus = (id, status) => http.put(`/plans/${id}/status`, null, { params: { status } })
export const deletePlan = (id) => http.delete(`/plans/${id}`)
export const todayDoses = (elderId, date) => http.get('/plans/today', { params: { elderId, date } })

/* ---------------- 服药打卡 ---------------- */
export const checkin = (data) => http.post('/checkins', data)
export const listCheckins = (elderId, params = {}) => http.get('/checkins', { params: { elderId, ...params } })
export const undoCheckin = (id) => http.delete(`/checkins/${id}`)
export const checkinStats = (elderId, from, to) =>
  http.get('/checkins/stats', { params: { elderId, from, to } })

/* ---------------- 拍照识药 ---------------- */
export const ocrDrug = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/ocr/drug', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

/* ---------------- 用药安全 ---------------- */
export const checkInteractions = (elderId) => http.post('/interactions/check', { elderId })
export const checkDrugNames = (drugNames) => http.post('/interactions/check-drugs', { drugNames })
export const listRules = () => http.get('/interactions/rules')
