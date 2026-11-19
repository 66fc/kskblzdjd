export const money = (value: number | string | null | undefined) => {
  const num = Number(value ?? 0)
  return `¥${num.toFixed(2)}`
}

export const number = (value: number | string | null | undefined, digits = 0) => {
  const num = Number(value ?? 0)
  return num.toFixed(digits)
}

export const minutesText = (minutes: number | null | undefined) => {
  const value = Number(minutes ?? 0)
  const h = Math.floor(value / 60)
  const m = value % 60
  if (h <= 0) return `${m} 分钟`
  return m === 0 ? `${h} 小时` : `${h} 小时 ${m} 分`
}

export const bookingStatusMap: Record<string, { label: string; type: string }> = {
  PENDING_PAYMENT: { label: '待支付', type: 'warning' },
  CONFIRMED: { label: '已确认', type: 'primary' },
  CANCELLED: { label: '已取消', type: 'info' },
  IN_PROGRESS: { label: '使用中', type: 'danger' },
  COMPLETED: { label: '已完成', type: 'success' },
  NO_SHOW: { label: '未到场', type: 'info' }
}

export const slotStatusMap: Record<string, { label: string; cls: string }> = {
  AVAILABLE: { label: '空闲', cls: 'available' },
  BOOKED: { label: '已预约', cls: 'booked' },
  IN_USE: { label: '使用中', cls: 'in-use' },
  CLOSED: { label: '关闭', cls: 'closed' }
}

export const courtStatusMap: Record<string, { label: string; cls: string }> = {
  OPEN: { label: '正常开放', cls: 'tag-open' },
  MAINTENANCE: { label: '维护中', cls: 'tag-maintenance' },
  DISABLED: { label: '已停用', cls: 'tag-disabled' }
}

export const paymentStatusMap: Record<string, string> = {
  UNPAID: '未支付',
  PAID: '已支付',
  REFUNDED: '已退款'
}

export const paymentMethodMap: Record<string, string> = {
  CASH: '现金',
  WECHAT: '微信',
  ALIPAY: '支付宝',
  CARD: '银行卡',
  MIXED: '组合支付'
}

export const categoryMap: Record<string, string> = {
  BALL: '羽毛球',
  RACKET: '球拍',
  DRINK: '饮料',
  ACCESSORY: '配件',
  OTHER: '其他'
}

export const todayString = () => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export const toApiDateTime = (value: string) => {
  if (!value) return ''
  const text = value.replace('T', ' ')
  return text.length === 16 ? `${text}:00` : text
}

export const uuid = () => {
  if (crypto?.randomUUID) return crypto.randomUUID()
  return `req-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
