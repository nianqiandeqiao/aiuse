function pad(value) {
  return String(value).padStart(2, '0')
}

/**
 * 取本地时区的"今天"（yyyy-MM-dd）。
 * 注意：不能用 toISOString()，它按 UTC 计算，中国时区凌晨 0-8 点会得到前一天。
 */
export function todayStr() {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

/** 在 yyyy-MM-dd 上加减天数，同样按本地时区计算 */
export function shiftDateStr(dateStr, days) {
  const [year, month, day] = dateStr.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  date.setDate(date.getDate() + days)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

