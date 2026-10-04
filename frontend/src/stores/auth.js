import { reactive } from 'vue'
import { authStatus, currentUser } from '../api'

const state = reactive({
  required: true,
  loggedIn: false,
  allowRegister: true,
  user: null,
  checked: false
})

/** 查询登录状态（结果缓存，避免每次路由都请求） */
export async function ensureAuthChecked(force = false) {
  if (state.checked && !force) {
    return state
  }
  try {
    const data = await authStatus()
    state.required = !!data.required
    state.loggedIn = !!data.loggedIn
    state.allowRegister = data.allowRegister !== false
    state.user = data.loggedIn ? await currentUser() : null
  } catch (e) {
    state.loggedIn = false
    state.user = null
  }
  state.checked = true
  return state
}

export function useAuth() {
  return state
}

export function markLoggedIn(user) {
  state.checked = true
  state.loggedIn = true
  state.user = user
}

export function markLoggedOut() {
  state.checked = true
  state.loggedIn = false
  state.user = null
}

