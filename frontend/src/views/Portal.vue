<template>
  <div class="portal">
    <div class="sakura-layer">
      <span v-for="p in petals" :key="p.id" class="sakura" :style="p.style" />
    </div>

    <header class="portal-head">
      <div class="ph-brand">
        <div class="ph-mark">羽</div>
        <div>
          <div class="ph-title">星羽羽毛球馆</div>
          <div class="ph-sub">MEMBER BOOKING DESK</div>
        </div>
      </div>
      <div class="ph-right">
        <div class="clock-chip" :class="{ paused: clock.paused }">
          <span class="dot" />
          <span class="mono">{{ clock.info?.effectiveNow || '--' }}</span>
        </div>
        <el-button v-if="auth.isAdmin" text bg @click="router.push('/dashboard')">进入管理后台</el-button>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="member-chip">
            <div class="m-avatar">{{ auth.user?.nickname?.slice(0, 1) }}</div>
            <div>
              <div class="m-name">{{ auth.user?.nickname }}</div>
              <div class="m-level">{{ auth.user?.levelName }}</div>
            </div>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="portal-body">
      <section class="hero glass-card">
        <div class="hero-copy">
          <div class="chip">GOOD DAY, {{ auth.user?.nickname }}</div>
          <h1>找到你的下一场对决</h1>
          <p>
            选择日期与时间段，系统会实时检查 30 分钟粒度的场地占用情况，
            只有整个区间全部空闲的场地才会出现在下方。
          </p>
          <div class="hero-badges">
            <span class="chip">会员折扣 {{ discountText(auth.user?.discountRate) }}</span>
            <span class="chip">累计消费 {{ money(auth.user?.totalConsumption) }}</span>
            <span class="chip">30 分钟起订</span>
          </div>
        </div>
        <div class="hero-orb">
          <div class="ring r1" />
          <div class="ring r2" />
          <div class="shuttle">🏸</div>
        </div>
      </section>

      <section class="glass-card booking-panel">
        <div class="card-head">
          <h3>预约场地</h3>
          <span class="muted" style="font-size: 11.5px">视图 v_available_court_slots 实时查询</span>
        </div>
        <div class="card-body">
          <div class="date-strip">
            <button v-for="d in nextDays" :key="d.value" class="date-btn" :class="{ active: d.value === date }"
                    @click="date = d.value; loadAvailability()">
              <span class="db-week">{{ d.week }}</span>
              <span class="db-day mono">{{ d.label }}</span>
            </button>
          </div>

          <div class="time-row">
            <div class="time-field">
              <span class="tf-label">开始时间</span>
              <el-select v-model="startTime" style="width: 130px" @change="loadAvailability">
                <el-option v-for="t in timeOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </div>
            <div class="time-field">
              <span class="tf-label">结束时间</span>
              <el-select v-model="endTime" style="width: 130px" @change="loadAvailability">
                <el-option v-for="t in endOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </div>
            <div class="tf-duration">
              时长 <b>{{ durationMinutes }}</b> 分钟
            </div>
            <div class="spacer" />
            <el-button type="primary" :icon="Search" :loading="loading" @click="loadAvailability">查询可预约场地</el-button>
          </div>

          <div v-if="errorText" class="portal-error">{{ errorText }}</div>

          <div v-loading="loading">
            <div v-if="available.length" class="court-grid">
              <div v-for="c in available" :key="c.courtId" class="portal-court"
                   :class="{ selected: selected?.courtId === c.courtId }" @click="selectCourt(c)">
                <div class="pc-top">
                  <div>
                    <h4>{{ c.courtName }}</h4>
                    <div class="muted" style="font-size: 11px">{{ c.venueName }}</div>
                  </div>
                  <el-tag size="small" class="tag-open">可预约</el-tag>
                </div>
                <div class="pc-price">
                  <span class="price">{{ money(estimated(c)) }}</span>
                  <small>会员价 / {{ durationLabel }}</small>
                </div>
                <div class="pc-foot">
                  <span class="muted" style="font-size: 11px">原价 {{ money(original(c)) }}</span>
                  <span class="chip">空闲 {{ c.freeSlots }} 段</span>
                </div>
              </div>
            </div>
            <el-empty v-else-if="!loading" :description="searched ? '该时间段暂无可预约场地，换个时间试试' : '请选择日期与时间段后查询'" />
          </div>
        </div>
      </section>

      <section class="grid grid-main" style="margin-top: 16px">
        <div class="glass-card">
          <div class="card-head">
            <h3>我的预约</h3>
            <el-button text size="small" @click="loadMine">刷新</el-button>
          </div>
          <div class="card-body">
            <el-table :data="mine" v-loading="mineLoading" size="small">
              <el-table-column label="时间" min-width="160">
                <template #default="{ row }">
                  <div class="mono" style="font-size: 11.5px">{{ row.startAt?.slice(0, 16) }}</div>
                  <div class="muted mono" style="font-size: 10.5px">至 {{ row.endAt?.slice(11, 16) }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="courtName" label="场地" width="95" />
              <el-table-column label="金额" width="95">
                <template #default="{ row }"><span class="gold-text">{{ money(row.payableAmount) }}</span></template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" align="right">
                <template #default="{ row }">
                  <el-button v-if="row.status === 'PENDING_PAYMENT'" size="small" type="primary" text bg
                             @click="pay(row)">支付</el-button>
                  <el-button v-if="['CONFIRMED','PENDING_PAYMENT'].includes(row.status)" size="small" text bg
                             type="danger" @click="cancel(row)">取消</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination v-model:current-page="minePage" :page-size="6" :total="mineTotal"
                           layout="prev, pager, next" @change="loadMine" />
          </div>
        </div>

        <div class="glass-card">
          <div class="card-head"><h3>预约摘要</h3></div>
          <div class="card-body">
            <div class="kv"><span>日期</span><span>{{ date }}</span></div>
            <div class="kv"><span>时间</span><span>{{ startTime }} - {{ endTime }}</span></div>
            <div class="kv"><span>场地</span><span>{{ selected?.courtName || '未选择' }}</span></div>
            <div class="kv"><span>时长</span><span>{{ durationLabel }}</span></div>
            <div class="kv"><span>原价</span><span>{{ selected ? money(original(selected)) : '—' }}</span></div>
            <div class="kv"><span>会员折扣</span><span>{{ discountText(auth.user?.discountRate) }}</span></div>
            <div class="kv">
              <span>应付金额</span>
              <span class="price" style="font-size: 18px">{{ selected ? money(estimated(selected)) : '—' }}</span>
            </div>
            <el-button type="primary" size="large" style="width: 100%; margin-top: 16px"
                       :disabled="!selected" :loading="bookingLoading" @click="submitBooking">
              立即预约并支付
            </el-button>
            <div class="timeline-note" style="margin-top: 10px">
              支付后生成 CONFIRMED 预约，数据库触发器会占用对应的 30 分钟时间段；
              取消后仅释放本订单占用的时段。
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { api } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useClockStore } from '@/stores/clock'
import { bookingStatusMap, money, todayString, toApiDateTime, uuid } from '@/utils/format'
import { Search, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()
const clock = useClockStore()

const date = ref(todayString())
const startTime = ref('10:00')
const endTime = ref('12:00')
const available = ref<any[]>([])
const selected = ref<any>(null)
const loading = ref(false)
const searched = ref(false)
const errorText = ref('')
const bookingLoading = ref(false)

const mine = ref<any[]>([])
const mineTotal = ref(0)
const minePage = ref(1)
const mineLoading = ref(false)
const venue = ref<any>(null)

const nextDays = computed(() => {
  const base = dayjs(clock.info?.businessDate || undefined)
  const weeks = ['日', '一', '二', '三', '四', '五', '六']
  return Array.from({ length: 14 }).map((_, i) => {
    const d = base.add(i, 'day')
    return { value: d.format('YYYY-MM-DD'), label: d.format('MM/DD'), week: i === 0 ? '今天' : `周${weeks[d.day()]}` }
  })
})

const timeOptions = computed(() => {
  const start = venue.value?.openingTime?.slice(0, 5) || '08:00'
  const close = venue.value?.closingTime?.slice(0, 5) || '23:30'
  const list: string[] = []
  let t = dayjs(`2000-01-01 ${start}`)
  const end = dayjs(`2000-01-01 ${close}`)
  while (t.isBefore(end)) {
    list.push(t.format('HH:mm'))
    t = t.add(30, 'minute')
  }
  return list
})

const endOptions = computed(() => timeOptions.value.filter((t) => t > startTime.value))

const durationMinutes = computed(() => {
  if (!startTime.value || !endTime.value) return 0
  return dayjs(`2000-01-01 ${endTime.value}`).diff(dayjs(`2000-01-01 ${startTime.value}`), 'minute')
})

const durationLabel = computed(() => {
  const m = durationMinutes.value
  if (m <= 0) return '—'
  return m % 60 === 0 ? `${m / 60} 小时` : `${m} 分钟`
})

const petals = computed(() =>
  Array.from({ length: 10 }).map((_, i) => ({
    id: i,
    style: {
      left: `${(i * 9.7 + 2) % 100}%`,
      animationDuration: `${15 + (i % 5) * 2.8}s`,
      animationDelay: `${-i * 2.2}s`,
      transform: `scale(${0.5 + (i % 3) * 0.3})`
    }
  }))
)

const statusLabel = (s: string) => bookingStatusMap[s]?.label || s
const statusType = (s: string) => bookingStatusMap[s]?.type || 'info'
const discountText = (rate: number | string | undefined) => {
  const value = Number(rate || 1)
  return value >= 1 ? '无折扣' : `${(value * 10).toFixed(1)} 折`
}

const hours = () => durationMinutes.value / 60
const original = (court: any) => Number(court.hourlyRate) * hours()
const estimated = (court: any) => original(court) * Number(auth.user?.discountRate || 1)

function selectCourt(court: any) {
  selected.value = court
}

async function loadAvailability() {
  errorText.value = ''
  if (!startTime.value || !endTime.value || endTime.value <= startTime.value) {
    errorText.value = '结束时间必须晚于开始时间'
    available.value = []
    return
  }
  loading.value = true
  selected.value = null
  try {
    const res = await api.availability(
      toApiDateTime(`${date.value} ${startTime.value}`),
      toApiDateTime(`${date.value} ${endTime.value}`)
    )
    available.value = res.data
    searched.value = true
  } catch (e: any) {
    available.value = []
  } finally {
    loading.value = false
  }
}

async function loadMine() {
  mineLoading.value = true
  try {
    const res = await api.myBookings({ page: minePage.value, size: 6 })
    mine.value = res.data.records
    mineTotal.value = res.data.total
  } finally {
    mineLoading.value = false
  }
}

function initDefaultTime() {
  const now = dayjs(clock.info?.effectiveNow || undefined)
  const openText = venue.value?.openingTime?.slice(0, 5) || '08:00'
  const closeText = venue.value?.closingTime?.slice(0, 5) || '23:30'
  const openToday = dayjs(`${now.format('YYYY-MM-DD')} ${openText}`)
  const closeToday = dayjs(`${now.format('YYYY-MM-DD')} ${closeText}`)

  let candidate = now.second(0).millisecond(0)
  const remainder = candidate.minute() % 30
  if (remainder !== 0 || now.second() > 0) {
    candidate = candidate.add(30 - remainder, 'minute')
  }
  if (candidate.isBefore(openToday)) {
    candidate = openToday
  }

  if (candidate.add(2, 'hour').isAfter(closeToday)) {
    date.value = now.add(1, 'day').format('YYYY-MM-DD')
    candidate = dayjs(`${date.value} 10:00`)
  } else {
    date.value = now.format('YYYY-MM-DD')
  }

  startTime.value = candidate.format('HH:mm')
  const endCandidate = candidate.add(2, 'hour')
  const lastSlot = timeOptions.value[timeOptions.value.length - 1] || closeText
  endTime.value = endCandidate.isAfter(dayjs(`${date.value} ${lastSlot}`))
    ? lastSlot
    : endCandidate.format('HH:mm')
}

async function submitBooking() {  if (!selected.value) return
  bookingLoading.value = true
  try {
    await ElMessageBox.confirm(
      `确认预约 ${selected.value.courtName}，${date.value} ${startTime.value}-${endTime.value}，应付 ${money(estimated(selected.value))}？`,
      '预约确认',
      { type: 'info', confirmButtonText: '确认支付', cancelButtonText: '再想想' }
    )
    await api.createBooking({
      courtId: selected.value.courtId,
      startAt: toApiDateTime(`${date.value} ${startTime.value}`),
      endAt: toApiDateTime(`${date.value} ${endTime.value}`),
      requestId: uuid(),
      payNow: true
    })
    ElMessage.success('预约成功，场地时间段已锁定')
    await Promise.all([loadAvailability(), loadMine()])
  } finally {
    bookingLoading.value = false
  }
}

async function cancel(row: any) {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消预约', {
    inputPlaceholder: '如：临时有事',
    inputValidator: (v: string) => (v && v.trim() ? true : '请填写原因'),
    confirmButtonText: '确认取消'
  })
  await api.cancelBooking(row.bookingId, value)
  ElMessage.success('已取消，时间段已释放')
  await Promise.all([loadAvailability(), loadMine()])
}

async function pay(row: any) {
  await api.payBooking(row.bookingId)
  ElMessage.success('支付成功')
  await Promise.all([loadAvailability(), loadMine()])
}

function onCommand(command: string) {
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  }
}

onMounted(async () => {
  await clock.fetch()
  const v = await api.venues()
  venue.value = v.data?.[0] || null
  initDefaultTime()
  await Promise.all([loadAvailability(), loadMine()])
})
</script>

<style scoped>
.portal {
  min-height: 100vh;
  position: relative;
}

.portal-head {
  height: 74px;
  padding: 0 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(139, 92, 246, 0.16);
  background: rgba(9, 6, 26, 0.6);
  backdrop-filter: blur(16px);
  position: sticky;
  top: 0;
  z-index: 10;
}

.ph-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ph-mark {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(140deg, #f472b6, #8b5cf6 60%, #22d3ee);
  box-shadow: 0 0 22px rgba(139, 92, 246, 0.45);
}

.ph-title {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
}

.ph-sub {
  font-size: 9px;
  color: #7c76a8;
  letter-spacing: 2px;
  margin-top: 3px;
}

.ph-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 10px 5px 5px;
  border-radius: 999px;
  border: 1px solid rgba(139, 92, 246, 0.25);
  background: rgba(139, 92, 246, 0.08);
  cursor: pointer;
  color: #c9c4ea;
}

.m-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(140deg, #f472b6, #8b5cf6 60%, #22d3ee);
}

.m-name {
  font-size: 12px;
}

.m-level {
  font-size: 9.5px;
  color: #8f88b8;
}

.portal-body {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 26px 60px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 30px;
  padding: 32px 36px;
  overflow: hidden;
}

.hero-copy h1 {
  margin: 14px 0 10px;
  font-size: 30px;
  letter-spacing: 1.5px;
  background: linear-gradient(100deg, #fff, #c4b5fd 45%, #67e8f9);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-copy p {
  color: var(--muted);
  line-height: 1.9;
  max-width: 560px;
  font-size: 12.5px;
}

.hero-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.hero-orb {
  position: relative;
  width: 170px;
  height: 170px;
  display: grid;
  place-items: center;
  flex: none;
}

.ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(167, 139, 250, 0.35);
}

.ring.r1 {
  inset: 0;
  animation: spin 18s linear infinite;
  border-style: dashed;
}

.ring.r2 {
  inset: 24px;
  border-color: rgba(34, 211, 238, 0.35);
  animation: spin 12s linear infinite reverse;
}

.shuttle {
  font-size: 46px;
  filter: drop-shadow(0 0 18px rgba(139, 92, 246, 0.6));
}

.booking-panel {
  margin-top: 16px;
}

.date-strip {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 6px;
}

.date-btn {
  flex: none;
  width: 76px;
  padding: 9px 0;
  border-radius: 12px;
  border: 1px solid rgba(139, 92, 246, 0.18);
  background: rgba(139, 92, 246, 0.05);
  color: var(--muted);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  transition: all 0.18s ease;
}

.date-btn:hover {
  border-color: rgba(167, 139, 250, 0.45);
  transform: translateY(-2px);
}

.date-btn.active {
  border-color: rgba(34, 211, 238, 0.6);
  background: linear-gradient(140deg, rgba(34, 211, 238, 0.16), rgba(139, 92, 246, 0.2));
  color: #e0f2fe;
  box-shadow: 0 0 18px rgba(34, 211, 238, 0.18);
}

.db-week {
  font-size: 10px;
}

.db-day {
  font-size: 12px;
  color: #ddd6fe;
}

.time-row {
  display: flex;
  align-items: flex-end;
  gap: 18px;
  margin: 18px 0 6px;
  flex-wrap: wrap;
}

.time-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tf-label {
  font-size: 10.5px;
  color: var(--muted);
  letter-spacing: 0.6px;
}

.tf-duration {
  font-size: 11.5px;
  color: var(--muted);
  padding-bottom: 8px;
}

.tf-duration b {
  color: #a5f3fc;
}

.portal-error {
  margin: 10px 0;
  padding: 9px 12px;
  border-radius: 10px;
  border: 1px solid rgba(251, 113, 133, 0.3);
  background: rgba(251, 113, 133, 0.08);
  color: #fda4af;
  font-size: 12px;
}

.court-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.pc-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.pc-price {
  margin: 14px 0 10px;
  display: flex;
  align-items: baseline;
  gap: 7px;
}

.pc-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed rgba(139, 92, 246, 0.15);
  padding-top: 9px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .portal-head {
    padding: 0 16px;
  }

  .portal-body {
    padding: 16px;
  }

  .hero-orb {
    display: none;
  }
}
</style>
