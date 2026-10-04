import { reactive, watch } from 'vue'

const STORAGE_KEY = 'med-reminder:current-elder'

function readStored() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch (e) {
    return null
  }
}

const state = reactive({
  elder: readStored()
})

watch(
  () => state.elder,
  (value) => {
    if (value) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  },
  { deep: true }
)

/** 当前选中的老人（全局共享，刷新后仍然记得） */
export function useCurrentElder() {
  return {
    state,
    setElder(elder) {
      state.elder = elder
    },
    clear() {
      state.elder = null
    }
  }
}

