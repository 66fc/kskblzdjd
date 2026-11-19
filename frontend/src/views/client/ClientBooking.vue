<template>
  <div class="booking-page c-fade-up">
    <div class="booking-head">
      <div>
        <div class="c-kicker">COURT RESERVATION</div>
        <h1>场地预约</h1>
        <p>
          当前业务时间 <b class="mono">{{ clock.info?.effectiveNow || '校准中' }}</b>
          · 营业时间 {{ opening.slice(0, 5) }} - {{ closing.slice(0, 5) }}
          · 可预约未来 30 天
        </p>
      </div>
      <div style="display: flex; gap: 8px; align-items: center">
        <el-button text bg :icon="Calendar" @click="calendarVisible = true">日历选择</el-button>
        <el-button text bg :icon="Refresh" :loading="loading" @click="reloadAll">刷新</el-button>
      </div>
    </div>

    <div class="booking-layout">
      <div>
        <div class="filter-card">
          <div class="date-strip-wrap">
            <button class="date-arrow" :disabled="date <= today" @click="shiftDate(-1)">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <div class="date-strip">
              <button v-for="day in dateStrip" :key="day" class="date-chip" :class="{ active: day === date }"
                      @click="selectDate(day)">
                <div class="dc-week">{{ relativeDay(day, today) }}</div>
                <div class="dc-day">{{ day.slice(8) }}</div>
                <div class="dc-free">
                  {{ dayCounts[day] === undefined ? '查询中' : `${dayCounts[day]} 块可约` }}
                </div>
              </button>
            </div>
            <button class="date-arrow" :disabled="date >= maxDate" @click="shiftDate(1)">
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>

          <div class="time-row">
            <div class="time-field">
              <label>开始时间</label>
              <el-select v-model="startTime" @change="onStartChange">
                <el-option v-for="t in startOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </div>
            <div class="time-field">
              <label>结束时间</label>
              <el-select v-model="endTime">
                <el-option v-for="t in endOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </div>
            <div class="duration-pills">
              <button v-for="d in durationPresets" :key="d.minutes" class="duration-pill"
                      :class="{ active: durationMinutes === d.minutes }" @click="applyDuration(d.minutes)">
                {{ d.label }}
              </button>
            </div>
            <div class="time-summary">
              使用时长
              <b>{{ durationLabel(durationMinutes) }}</b>
              <span>{{ prettyDate(date) }}</span>
            </div>
          </div>
        </div>

        <div class="c-section-head" style="margin: 26px 0 4px">
          <div>
            <div class="c-kicker">AVAILABLE COURTS</div>
            <h2 class="c-title" style="font-size: 21px">
              {{ prettyDate(date) }} {{ startTime }} - {{ endTime }}
            </h2>
            <p class="c-sub">
              共 {{ courts.length }} 块场地，{{ availableCourts.length }} 块在该时段完整空闲。
              只有整个时间段全部空闲的场地才可预约。
            </p>
          </div>
        </div>

        <div v-if="errorText" class="inline-error">{{ errorText }}</div>

        <div v-loading="loading" class="court-list">
          <div v-for="court in courts" :key="court.courtId" class="court-card"
               :class="{ selected: selectedCourt?.courtId === court.courtId, disabled: !isAvailable(court) }"
               @click="selectCourt(court)">
            <div class="court-art-wrap">
              <CourtArt :court-id="court.courtId" :court-name="court.courtName" />
              <span class="court-art-badge">{{ court.courtNumber }}</span>
              <span class="court-art-status">
                <span class="status-pill" :class="`st-${courtPill(court).status}`">{{ courtPill(court).label }}</span>
              </span>
            </div>
            <div class="court-body">
              <div class="court-name-row">
                <div>
                  <h3>{{ court.courtName }}</h3>
                  <p>{{ court.description || '标准双打场地' }}</p>
                </div>
                <div class="court-price">
                  <b>¥{{ Number(court.hourlyRate).toFixed(0) }}</b>
                  <span>每小时</span>
                </div>
              </div>
              <div class="court-info">
                <span><el-icon><Location /></el-icon>{{ court.venueName || '星羽羽毛球馆' }}</span>
                <span><el-icon><Clock /></el-icon>{{ durationLabel(durationMinutes) }}</span>
                <span v-if="isAvailable(court)"><el-icon><CircleCheck /></el-icon>整段空闲</span>
                <span v-else><el-icon><CircleClose /></el-icon>{{ unavailableReason(court) }}</span>
              </div>
              <div class="court-foot">
                <span class="hint">
                  {{ auth.user ? `会员价 ¥${memberPrice(court).toFixed(2)}（${discountText(auth.user.discountRate)}）` : '登录后显示会员价' }}
                </span>
                <button class="court-select" :disabled="!isAvailable(court)"
                        @click.stop="selectCourt(court)">
                  {{ selectedCourt?.courtId === court.courtId ? '已选择' : (isAvailable(court) ? '选择场地' : '不可预约') }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <EmptyState v-if="!loading && !courts.length" title="暂时没有场地数据"
                    description="请检查后端服务是否启动，或稍后刷新重试。" />
        <EmptyState v-else-if="!loading && !availableCourts.length" title="这个时间段已经被约满啦"
                    description="换一个时间或日期，幻想馆还有别的空场在等你。" />
      </div>

      <aside>
        <div class="summary-card">
          <div class="summary-head">
            <h3>预约摘要</h3>
            <span>ORDER PREVIEW</span>
          </div>

          <div v-if="auth.user" class="summary-mascot">
            <div class="summary-mascot-orb">
              <img :src="chibi" alt="会员" />
            </div>
            <p>
              {{ auth.user.nickname }} · {{ auth.user.levelName }}<br />
              会员折扣 {{ discountText(auth.user.discountRate) }}
            </p>
          </div>
          <div v-else class="summary-mascot">
            <div class="summary-mascot-orb">
              <img :src="chibi" alt="未登录" />
            </div>
            <p>还未登录，登录后可享受会员折扣<br />并保存这笔预约。</p>
          </div>

          <div class="summary-line"><span>场地</span><span>{{ selectedCourt?.courtName || '尚未选择' }}</span></div>
          <div class="summary-line"><span>日期</span><span>{{ prettyDate(date) }}</span></div>
          <div class="summary-line"><span>时间</span><span>{{ startTime }} - {{ endTime }}</span></div>
          <div class="summary-line"><span>时长</span><span>{{ durationLabel(durationMinutes) }}</span></div>
          <div class="summary-line"><span>单价</span><span>{{ selectedCourt ? `¥${Number(selectedCourt.hourlyRate).toFixed(2)} / 时` : '—' }}</span></div>
          <div class="summary-line">
            <span>会员折扣</span>
            <span>{{ auth.user ? discountText(auth.user.discountRate) : '登录后可用' }}</span>
          </div>
          <div class="summary-line">
            <span>原价</span>
            <span>{{ selectedCourt ? `¥${originalPrice.toFixed(2)}` : '—' }}</span>
          </div>

          <div class="summary-total">
            <div>
              <div class="summary-total-label">应付金额</div>
              <div style="font-size: 10px; color: #7f76a0; margin-top: 4px">最终以服务端计价为准</div>
            </div>
            <div class="summary-total-value">{{ selectedCourt ? `¥${payablePrice.toFixed(2)}` : '—' }}</div>
          </div>

          <el-button class="c-btn-primary" style="width: 100%" :disabled="!selectedCourt"
                     @click="openConfirm">
            确认预约
          </el-button>

          <div class="summary-tip">
            提交后系统将锁定对应 30 分钟时间段；取消预约会立即释放。
            演示环境为即时支付，未接入真实支付网关。
          </div>
        </div>
      </aside>
    </div>

    <el-dialog v-model="confirmVisible" title="确认预约信息" width="520px" class="client-dialog"
               :close-on-click-modal="false">
      <div v-if="selectedCourt" class="confirm-box">
        <div class="confirm-head">
          <div class="confirm-court">
            <span class="status-pill st-CONFIRMED">{{ selectedCourt.courtNumber }}</span>
            <h3>{{ selectedCourt.courtName }}</h3>
            <p>{{ prettyDate(date) }} · {{ startTime }} - {{ endTime }}</p>
          </div>
          <div class="confirm-price">¥{{ payablePrice.toFixed(2) }}</div>
        </div>

        <div class="confirm-lines">
          <div><span>预约用户</span><span>{{ auth.user?.nickname }}（{{ auth.user?.levelName }}）</span></div>
          <div><span>使用时长</span><span>{{ durationLabel(durationMinutes) }}</span></div>
          <div><span>场地单价</span><span>¥{{ Number(selectedCourt.hourlyRate).toFixed(2) }} / 小时</span></div>
          <div><span>会员折扣</span><span>{{ discountText(auth.user?.discountRate) }}</span></div>
          <div><span>原价</span><span>¥{{ originalPrice.toFixed(2) }}</span></div>
          <div class="total"><span>应付金额</span><span>¥{{ payablePrice.toFixed(2) }}</span></div>
        </div>

        <div class="confirm-note">
          确认后将生成正式预约并占用场地时间段。若该时段刚被他人预约，系统会提示冲突并自动刷新。
        </div>
      </div>
      <template #footer>
        <el-button @click="confirmVisible = false">再想想</el-button>
        <el-button class="c-btn-primary" style="height: 38px" :loading="submitting" @click="submitBooking">
          {{ submitting ? '正在锁定场地…' : '确认并支付' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="successVisible" width="440px" class="client-dialog" :show-close="false">
      <div class="success-panel">
        <div class="success-orb">
          <img :src="chibi" alt="预约成功" />
        </div>
        <h2>预约成功！</h2>
        <p>场地已经为你锁定，记得提前 10 分钟到场热身。</p>
        <div class="success-order">
          <span>ORDER NUMBER</span>
          <b>{{ createdBooking?.bookingNo }}</b>
        </div>
        <div style="display: flex; gap: 10px">
          <el-button style="flex: 1" @click="continueBooking">继续预约</el-button>
          <el-button class="c-btn-primary" style="flex: 1; height: 38px" @click="goMyBookings">查看订单</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="calendarVisible" title="选择日期" width="520px" class="client-dialog">
      <el-calendar v-model="calendarValue">
        <template #date-cell="{ data }">
          <div class="cal-cell" :class="{ disabled: calendarDisabled(data.day), selected: data.day === date }"
               @click="pickCalendar(data.day)">
            {{ data.day.split('-')[2] }}
          </div>
        </template>
      </el-calendar>
      <div class="calendar-tip">可预约 {{ today }} 至 {{ maxDate }}，点击日期即可查询该日场地。</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import {
  ArrowLeft, ArrowRight, Calendar, CircleCheck, CircleClose, Clock, Location, Refresh
} from '@element-plus/icons-vue'
import CourtArt from '@/components/client/CourtArt.vue'
import EmptyState from '@/components/client/EmptyState.vue'
import { clientApi } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useClockStore } from '@/stores/clock'
import {
  addDays, dateStrip as buildDateStrip, durationLabel, endTimeOptions, minutesBetween,
  prettyDate, relativeDay, timeOptions, toIso, todayFromClock
} from '@/utils/time'
import chibi from '@/assets/mascot/mascot-chibi.webp'
import { uuid } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const clock = useClockStore()

const loading = ref(false)
const submitting = ref(false)
const courts = ref<any[]>([])
const availableRows = ref<any[]>([])
const venue = ref<any>(null)
const today = ref(todayFromClock(clock.info?.effectiveNow))
const maxDate = computed(() => addDays(today.value, 30))
const date = ref(today.value)
const startTime = ref('10:00')
const endTime = ref('12:00')
const selectedCourt = ref<any>(null)
const errorText = ref('')
const dayCounts = reactive<Record<string, number>>({})
const confirmVisible = ref(false)
const successVisible = ref(false)
const createdBooking = ref<any>(null)
const calendarVisible = ref(false)
const calendarValue = ref(new Date())
const ready = ref(false)
let requestId = ''
let weekToken = 0

const opening = computed(() => venue.value?.openingTime || '08:00:00')
const closing = computed(() => venue.value?.closingTime || '23:30:00')
const startOptions = computed(() => timeOptions(opening.value, closing.value))
const endOptions = computed(() => endTimeOptions(opening.value, closing.value, startTime.value))
const durationMinutes = computed(() => Math.max(0, minutesBetween(startTime.value, endTime.value)))

const dateStrip = computed(() => buildDateStrip(today.value, 14))
const availableCourts = computed(() => {
  const free = new Set(availableRows.value.map((r) => r.courtId))
  return courts.value.filter((c) => c.operationalStatus === 'OPEN' && free.has(c.courtId))
})

const durationPresets = [
  { label: '1 小时', minutes: 60 },
  { label: '1.5 小时', minutes: 90 },
  { label: '2 小时', minutes: 120 },
  { label: '3 小时', minutes: 180 }
]

const originalPrice = computed(() => {
  if (!selectedCourt.value) return 0
  return Number(((Number(selectedCourt.value.hourlyRate) * durationMinutes.value) / 60).toFixed(2))
})

const payablePrice = computed(() => {
  if (!selectedCourt.value) return 0
  const discount = Number(auth.user?.discountRate || 1)
  return Number((originalPrice.value * discount).toFixed(2))
})

const discountText = (rate: number | string | undefined) => {
  const value = Number(rate || 1)
  return value >= 1 ? '无折扣' : `${(value * 10).toFixed(1)} 折`
}

function isAvailable(court: any) {
  return court.operationalStatus === 'OPEN'
    && availableCourts.value.some((c) => c.courtId === court.courtId)
}

function unavailableReason(court: any) {
  if (court.operationalStatus === 'MAINTENANCE') return '维护中'
  if (court.operationalStatus === 'DISABLED') return '已停用'
  return '该时段已约满'
}

function courtPill(court: any) {
  if (court.operationalStatus === 'MAINTENANCE') return { label: '维护中', status: 'MAINTENANCE' }
  if (court.operationalStatus === 'DISABLED') return { label: '已停用', status: 'CANCELLED' }
  if (!isAvailable(court)) return { label: '该时段不可约', status: 'BOOKED' }
  if (selectedCourt.value?.courtId === court.courtId) return { label: '已选择', status: 'CONFIRMED' }
  return { label: '可预约', status: 'AVAILABLE' }
}

function memberPrice(court: any) {
  return Number(((Number(court.hourlyRate) * durationMinutes.value) / 60
    * Number(auth.user?.discountRate || 1)).toFixed(2))
}

function selectDate(day: string) {
  if (day < today.value || day > maxDate.value) return
  date.value = day
}

function shiftDate(delta: number) {
  const next = dayjs(date.value).add(delta, 'day').format('YYYY-MM-DD')
  if (next < today.value || next > maxDate.value) return
  date.value = next
}

function onStartChange() {
  if (endTime.value <= startTime.value) {
    endTime.value = endTimeOptions(opening.value, closing.value, startTime.value)[0] || startTime.value
  }
  selectedCourt.value = null
}

function applyDuration(minutes: number) {
  const end = dayjs(`2000-01-01 ${startTime.value}`).add(minutes, 'minute').format('HH:mm')
  const close = closing.value.slice(0, 5)
  if (end > close) {
    ElMessage.warning('超出营业时间，请调整开始时间')
    return
  }
  endTime.value = end
  selectedCourt.value = null
}

function selectCourt(court: any) {
  if (!isAvailable(court)) {
    ElMessage.warning(unavailableReason(court))
    return
  }
  selectedCourt.value = selectedCourt.value?.courtId === court.courtId ? null : court
}

function pickCalendar(day: string) {
  if (calendarDisabled(day)) {
    ElMessage.warning('该日期不在可预约范围内')
    return
  }
  selectDate(day)
  calendarVisible.value = false
}

function calendarDisabled(day: string) {
  return day < today.value || day > maxDate.value
}

async function loadAvailability() {
  if (endTime.value <= startTime.value) {
    errorText.value = '结束时间必须晚于开始时间'
    return
  }
  loading.value = true
  errorText.value = ''
  selectedCourt.value = null
  try {
    const [courtRes, availRes] = await Promise.all([
      clientApi.courts(),
      clientApi.availability(toIso(date.value, startTime.value), toIso(date.value, endTime.value))
    ])
    courts.value = courtRes.data
    availableRows.value = availRes.data
    const freeIds = new Set(availRes.data.map((r: any) => r.courtId))
    courts.value.forEach((c: any) => {
      if (c.operationalStatus === 'OPEN') {
        c._free = freeIds.has(c.courtId)
      }
    })
  } catch (e: any) {
    errorText.value = e?.response?.data?.message || '加载场地失败，请稍后重试'
    availableRows.value = []
  } finally {
    loading.value = false
  }
}

async function updateWeekCounts() {
  const token = ++weekToken
  const days = buildDateStrip(today.value, 14)
  await Promise.all(days.map(async (day) => {
    try {
      const res = await clientApi.availability(toIso(day, startTime.value), toIso(day, endTime.value))
      if (token === weekToken) dayCounts[day] = res.data.length
    } catch {
      if (token === weekToken) dayCounts[day] = 0
    }
  }))
}

async function reloadAll() {
  await clock.fetch()
  today.value = todayFromClock(clock.info?.effectiveNow)
  await loadAvailability()
}

function openConfirm() {
  if (!selectedCourt.value) {
    ElMessage.warning('请先选择一块可预约的场地')
    return
  }
  if (!auth.token) {
    const redirect = `${route.path}?date=${date.value}&start=${startTime.value}&end=${endTime.value}`
    router.push({ path: '/club/login', query: { redirect } })
    return
  }
  requestId = uuid()
  confirmVisible.value = true
}

async function submitBooking() {
  if (submitting.value) return
  if (!requestId) requestId = uuid()
  submitting.value = true
  try {
    const res = await clientApi.createBooking({
      courtId: selectedCourt.value.courtId,
      startAt: toIso(date.value, startTime.value),
      endAt: toIso(date.value, endTime.value),
      requestId,
      payNow: true
    })
    createdBooking.value = res.data
    confirmVisible.value = false
    successVisible.value = true
    selectedCourt.value = null
    requestId = ''
    await Promise.all([loadAvailability(), updateWeekCounts(), auth.fetchMe()])
  } catch (e: any) {
    errorText.value = e?.response?.data?.message || '预约失败，请稍后重试'
    if (e?.response?.status === 409) {
      confirmVisible.value = false
      await Promise.all([loadAvailability(), updateWeekCounts()])
    }
  } finally {
    submitting.value = false
  }
}

function continueBooking() {
  successVisible.value = false
  selectedCourt.value = null
}

function goMyBookings() {
  successVisible.value = false
  router.push('/club/bookings')
}

async function initFromRoute() {
  const d = route.query.date as string
  const s = route.query.start as string
  const e = route.query.end as string
  if (d && d >= today.value && d <= maxDate.value) date.value = d
  if (s && startOptions.value.includes(s)) startTime.value = s
  const validEnds = endTimeOptions(opening.value, closing.value, startTime.value)
  if (e && validEnds.includes(e)) endTime.value = e
  else if (durationMinutes.value < 30) {
    endTime.value = validEnds[0] || startTime.value
  }
}

function initDefaultTime() {
  const now = dayjs(clock.info?.effectiveNow || undefined)
  const openText = opening.value.slice(0, 5)
  const closeText = closing.value.slice(0, 5)
  let cursor = now.second(0).millisecond(0)
  const remainder = cursor.minute() % 30
  if (remainder !== 0 || now.second() > 0) {
    cursor = cursor.add(30 - remainder, 'minute')
  }
  if (!cursor.isAfter(now)) cursor = cursor.add(30, 'minute')
  const openToday = dayjs(`${now.format('YYYY-MM-DD')} ${openText}`)
  const closeToday = dayjs(`${now.format('YYYY-MM-DD')} ${closeText}`)
  if (cursor.isBefore(openToday)) cursor = openToday

  if (cursor.add(2, 'hour').isAfter(closeToday)) {
    date.value = now.add(1, 'day').format('YYYY-MM-DD')
    cursor = dayjs(`${date.value} 10:00`)
  } else {
    date.value = now.format('YYYY-MM-DD')
  }
  startTime.value = cursor.format('HH:mm')
  endTime.value = cursor.add(2, 'hour').format('HH:mm')
}

watch([date, startTime, endTime], () => {
  if (!ready.value) return
  loadAvailability()
})

watch([startTime, endTime], () => {
  if (!ready.value) return
  updateWeekCounts()
})

watch(date, (val) => {
  router.replace({ query: { ...route.query, date: val, start: startTime.value, end: endTime.value } })
})

onMounted(async () => {
  await clock.fetch()
  today.value = todayFromClock(clock.info?.effectiveNow)
  calendarValue.value = dayjs(today.value).toDate()
  const venueRes = await clientApi.venues()
  venue.value = venueRes.data?.[0] || null
  initDefaultTime()
  await initFromRoute()
  ready.value = true
  await Promise.all([loadAvailability(), updateWeekCounts()])
})

onBeforeUnmount(() => {
  weekToken++
})
</script>

<style scoped>
.mono {
  font-variant-numeric: tabular-nums;
}

.inline-error {
  margin: 12px 0;
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff1f2;
  border: 1px solid rgba(244, 63, 94, 0.25);
  color: #be123c;
  font-size: 12px;
}

.confirm-box {
  padding: 2px;
}

.confirm-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px dashed rgba(124, 58, 237, 0.16);
}

.confirm-court h3 {
  margin: 8px 0 4px;
  font-size: 18px;
  color: #241a45;
}

.confirm-court p {
  margin: 0;
  font-size: 11.5px;
  color: #8b7fae;
}

.confirm-price {
  font-size: 26px;
  font-weight: 800;
  color: #7c3aed;
  align-self: center;
  font-variant-numeric: tabular-nums;
}

.confirm-lines {
  padding: 10px 0 2px;
}

.confirm-lines > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  font-size: 12px;
  color: #4a3d6b;
}

.confirm-lines > div span:first-child {
  color: #8b7fae;
}

.confirm-lines .total {
  margin-top: 6px;
  padding-top: 12px;
  border-top: 1px dashed rgba(124, 58, 237, 0.16);
  font-size: 15px;
  font-weight: 700;
}

.confirm-lines .total span:last-child {
  color: #7c3aed;
}

.confirm-note {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #faf8ff;
  border: 1px dashed rgba(124, 58, 237, 0.18);
  font-size: 10.5px;
  line-height: 1.8;
  color: #8b7fae;
}

.cal-cell {
  height: 100%;
  display: grid;
  place-items: center;
  border-radius: 8px;
  font-size: 12px;
  color: #4a3d6b;
}

.cal-cell.disabled {
  color: #d4cce6;
  cursor: not-allowed;
}

.cal-cell.selected {
  background: linear-gradient(120deg, #7c3aed, #a855f7);
  color: #fff;
  font-weight: 700;
}

.calendar-tip {
  margin-top: 8px;
  font-size: 10.5px;
  color: #8b7fae;
  text-align: center;
}

@media (max-width: 720px) {
  .confirm-head {
    flex-direction: column;
  }

  .confirm-price {
    align-self: flex-start;
  }
}
</style>
