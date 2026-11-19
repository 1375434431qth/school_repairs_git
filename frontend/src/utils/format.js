/** 时间格式化：后端返回 'YYYY-MM-DDTHH:mm:ss' 或 'YYYY-MM-DD HH:mm:ss' */
export function formatDateTime(value) {
  if (!value) return '-'
  const text = String(value).replace('T', ' ')
  return text.length >= 16 ? text.slice(0, 16) : text
}

export function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 10)
}

/** 分钟 → Xh Ym */
export function formatDuration(minutes) {
  if (minutes === null || minutes === undefined || minutes === '') return '-'
  const total = Number(minutes)
  if (Number.isNaN(total)) return '-'
  if (total < 60) return `${total}m`
  const hours = Math.floor(total / 60)
  const mins = total % 60
  return `${hours}h ${mins}m`
}

/** 0~1 小数 → 百分比，保留 1 位 */
export function formatPercent(ratio) {
  if (ratio === null || ratio === undefined) return '-'
  return `${(Number(ratio) * 100).toFixed(1)}%`
}

export function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '-'
  return `¥${Number(value).toFixed(2)}`
}

/** 生成近 N 天的日期区间（右闭），用于统计页默认值 */
export function recentRange(days = 30) {
  const end = new Date()
  const begin = new Date()
  begin.setDate(end.getDate() - (days - 1))
  const toText = (date) => {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }
  return [toText(begin), toText(end)]
}

export function monthRange() {
  const end = new Date()
  const begin = new Date(end.getFullYear(), end.getMonth(), 1)
  const toText = (date) => {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }
  return [toText(begin), toText(end)]
}
