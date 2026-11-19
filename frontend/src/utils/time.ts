import dayjs, { type Dayjs } from 'dayjs'

export const BUSINESS_TIMEZONE = 'Asia/Shanghai'
export const BUSINESS_OFFSET = '+08:00'

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

/** 服务端统一时间；未取到时钟时回退浏览器时间，仅用于首屏占位。 */
export function nowFromClock(effectiveNow?: string | null): Dayjs {
  return effectiveNow ? dayjs(effectiveNow) : dayjs()
}

export function todayFromClock(effectiveNow?: string | null): string {
  return nowFromClock(effectiveNow).format('YYYY-MM-DD')
}

export function addDays(date: string, days: number): string {
  return dayjs(date).add(days, 'day').format('YYYY-MM-DD')
}

export function diffDays(date: string, base: string): number {
  return dayjs(date).startOf('day').diff(dayjs(base).startOf('day'), 'day')
}

export function formatDateCN(date: string): string {
  return dayjs(date).format('M月D日')
}

export function weekdayCN(date: string): string {
  return WEEKDAYS[dayjs(date).day()]
}

export function relativeDay(date: string, today: string): string {
  const diff = diffDays(date, today)
  if (diff === 0) return '今天'
  if (diff === 1) return '明天'
  if (diff === 2) return '后天'
  if (diff === -1) return '昨天'
  return weekdayCN(date)
}

export function toIso(date: string, time: string): string {
  const normalized = time.length === 5 ? `${time}:00` : time
  return `${date}T${normalized}${BUSINESS_OFFSET}`
}

export function minutesBetween(start: string, end: string): number {
  return dayjs(`2000-01-01 ${end}`).diff(dayjs(`2000-01-01 ${start}`), 'minute')
}

export function durationLabel(minutes: number): string {
  if (!minutes || minutes <= 0) return '—'
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  if (hours === 0) return `${mins} 分钟`
  return mins === 0 ? `${hours} 小时` : `${hours} 小时 ${mins} 分`
}

export function timeOptions(opening = '08:00:00', closing = '23:30:00'): string[] {
  const list: string[] = []
  let cursor = dayjs(`2000-01-01 ${opening.slice(0, 5)}`)
  const end = dayjs(`2000-01-01 ${closing.slice(0, 5)}`)
  while (cursor.isBefore(end)) {
    list.push(cursor.format('HH:mm'))
    cursor = cursor.add(30, 'minute')
  }
  return list
}

export function endTimeOptions(opening: string, closing: string, start: string): string[] {
  return [...timeOptions(opening, closing), closing.slice(0, 5)]
    .filter((t) => t > start)
    .filter((t, i, arr) => arr.indexOf(t) === i)
}

export function prettyDate(date: string): string {
  return `${dayjs(date).format('YYYY年M月D日')} ${weekdayCN(date)}`
}

export function prettyDateTime(value?: string | null): string {
  if (!value) return '—'
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}

export function timePart(value?: string | null): string {
  return value ? dayjs(value).format('HH:mm') : '--:--'
}

/** 生成从 date 起 count 天的日期数组 */
export function dateStrip(date: string, count: number): string[] {
  return Array.from({ length: count }).map((_, i) => addDays(date, i))
}
