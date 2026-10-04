import { reactive } from 'vue'
import { authStatus } from '../api'

const state = reactive({
  required: false,
  loggedIn: true,
  checked: false
})

/** 查询一次访问口令状态（结果会缓存，避免每次路由都请求） */
export async function ensureAuthChecked(force = false) {
  if (state.checked && !force) {
    return state
  }
  try {
    const data = await authStatus()
    state.required = !!data.required
    state.loggedIn = !!data.loggedIn
  } catch (e) {
    // 网络异常时不阻塞使用，由具体接口的报错提示
    state.required = false
    state.loggedIn = true
  }
  state.checked = true
  return state
}

export function useAuth() {
  return state
}

export function markLoggedIn() {
  state.checked = true
  state.required = true
  state.loggedIn = true
}

export function markLoggedOut() {
  state.checked = true
  state.loggedIn = false
}

