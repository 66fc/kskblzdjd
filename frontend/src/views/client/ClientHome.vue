<template>
  <div class="c-fade-up">
    <section class="c-hero">
      <div class="c-petal-layer">
        <span v-for="p in petals" :key="p.id" class="c-petal" :style="p.style" />
      </div>

      <div class="hero-inner">
        <div>
          <div class="hero-eyebrow">
            <span class="dot" style="width: 5px; height: 5px; border-radius: 50%; background: #67e8f9" />
            BADMINTON FANTASY CLUB · 深圳龙岗
          </div>
          <h1 class="hero-title">
            今晚，去打一场
            <span>把日常留在场外，把心跳留给羽毛球</span>
          </h1>
          <p class="hero-desc">
            星羽羽毛球馆 5 块开放场地，30 分钟粒度实时占用。
            选一个属于你的时间，剩下的交给我们。
          </p>
          <div class="hero-actions">
            <el-button class="c-btn-primary" @click="goBooking">
              <el-icon style="margin-right: 6px"><Calendar /></el-icon>
              立即预约场地
            </el-button>
            <el-button class="c-btn-ghost" @click="goBookings">
              查看我的预约
            </el-button>
          </div>
          <div class="hero-stats">
            <div>
              <div class="hero-stat-num">{{ stats.openCourts }}<small>块</small></div>
              <div class="hero-stat-label">开放场地</div>
            </div>
            <div>
              <div class="hero-stat-num">{{ stats.freeNow }}<small>块</small></div>
              <div class="hero-stat-label">此刻空闲</div>
            </div>
            <div>
              <div class="hero-stat-num">¥{{ stats.minPrice }}<small>起 / 时</small></div>
              <div class="hero-stat-label">会员价更低</div>
            </div>
            <div>
              <div class="hero-stat-num">30<small>min</small></div>
              <div class="hero-stat-label">最小预约粒度</div>
            </div>
          </div>
        </div>

        <div class="mascot-stage">
          <div class="mascot-halo" />
          <div class="mascot-ring" />
          <div class="mascot-clip">
            <MascotFigure variant="hero" />
          </div>
          <div class="hero-tag tag-a">
            <span style="width: 6px; height: 6px; border-radius: 50%; background: #10b981" />
            现在有 {{ stats.freeNow }} 块场地空闲
          </div>
          <div class="hero-tag tag-b">
            <el-icon color="#7c3aed"><Medal /></el-icon>
            会员最高 85 折
          </div>
        </div>
      </div>

      <div class="hero-quick">
        <div class="quick-field quick-grow">
          <label>预约日期</label>
          <el-date-picker v-model="quick.date" type="date" value-format="YYYY-MM-DD"
                          :clearable="false" :disabled-date="disabledDate" style="width: 100%" />
        </div>
        <div class="quick-field">
          <label>开始时间</label>
          <el-select v-model="quick.start" @change="ensureEnd">
            <el-option v-for="t in startOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </div>
        <div class="quick-field">
          <label>结束时间</label>
          <el-select v-model="quick.end">
            <el-option v-for="t in quickEndOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </div>
        <div class="quick-field" style="min-width: 86px">
          <label>时长</label>
          <div style="font-size: 15px; font-weight: 700; color: #4c1d95; line-height: 32px">
            {{ durationLabel(quickMinutes) }}
          </div>
        </div>
        <el-button class="c-btn-primary" style="height: 40px" @click="goBooking">
          查询空闲
        </el-button>
      </div>
    </section>

    <section class="c-section">
      <SectionHead kicker="LIVE COURT STATUS" title="今日场地状态"
                   :subtitle="`${prettyDate(today)} · 数据每 8 秒随系统时钟刷新`">
        <template #action>
          <el-button text bg @click="goBooking">查看完整日历 →</el-button>
        </template>
      </SectionHead>

      <div v-loading="loading" class="court-mini-grid">
        <div v-for="court in overview" :key="court.courtId" class="court-mini">
          <div class="court-mini-top">
            <div>
              <div class="court-mini-name">{{ court.courtName }}</div>
              <div class="court-mini-num">{{ court.courtNumber }} · {{ court.operationalStatus === 'MAINTENANCE' ? '维护中' : '标准场' }}</div>
            </div>
            <span class="status-pill" :class="`st-${courtPill(court).status}`">{{ courtPill(court).label }}</span>
          </div>
          <div class="court-mini-price">¥{{ Number(court.hourlyRate).toFixed(0) }}<small> / 小时</small></div>
          <div class="court-mini-meta">
            <span>空闲 {{ court.availableSlots }} 段</span>
            <span>已约 {{ court.bookedSlots + court.inUseSlots }} 段</span>
          </div>
        </div>
      </div>
    </section>

    <section class="c-section">
      <SectionHead kicker="MEMBER BENEFITS" title="会员权益"
                   subtitle="等级由后台统一维护，消费累计自动升级；你也可以请管理员为你手动调整。">
        <template #action>
          <span class="chip" style="padding: 6px 12px; border-radius: 999px; background: #fff; border: 1px solid rgba(124,58,237,.15); font-size: 11px; color: #6d28d9">
            {{ auth.user ? `当前：${auth.user.levelName} · ${discountText(auth.user.discountRate)}` : '登录后查看你的等级' }}
          </span>
        </template>
      </SectionHead>

      <div class="level-grid">
        <div v-for="level in levels" :key="level.levelId" class="level-card"
             :class="{ current: auth.user?.levelId === level.levelId }">
          <div class="level-icon" :style="{ background: levelGradient(level.levelRank) }">
            L{{ level.levelRank }}
          </div>
          <div class="level-name">{{ level.levelName }}</div>
          <div class="level-discount">{{ discountText(level.discountRate) }}</div>
          <div class="level-threshold">
            累计消费满 ¥{{ Number(level.minimumConsumption).toFixed(0) }} 自动升级
          </div>
        </div>
      </div>
    </section>

    <section class="c-section">
      <div class="home-split">
        <div class="venue-panel">
          <img :src="venueImage" alt="星羽羽毛球馆夜景" />
          <div class="venue-copy">
            <div class="hero-eyebrow" style="border-color: rgba(255,255,255,.3); color: #e9d5ff">
              OUR VENUE
            </div>
            <h3>{{ venue?.venueName || '星羽羽毛球馆' }}</h3>
            <p>
              专业地胶与赛事级灯光，5 块开放场地覆盖不同价位。
              场馆每日 08:00 - 23:30 营业，支持 30 分钟粒度的灵活预约。
            </p>
            <div class="venue-tags">
              <span class="venue-tag">木地板 / 专业地胶</span>
              <span class="venue-tag">赛事级灯光</span>
              <span class="venue-tag">独立休息区</span>
              <span class="venue-tag">免费淋浴</span>
            </div>
          </div>
        </div>

        <div class="client-card" style="padding: 24px">
          <SectionHead kicker="NOTICE" title="最新公告" />
          <div class="notice-list">
            <div v-for="(notice, index) in notices" :key="notice.title" class="notice-item">
              <div class="notice-date">
                <b>{{ notice.day }}</b>
                <span>{{ notice.month }}</span>
              </div>
              <div class="notice-content">
                <h4>{{ notice.title }}</h4>
                <p>{{ notice.desc }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="c-section">
      <div class="client-card" style="padding: 28px 30px; display: flex; align-items: center; justify-content: space-between; gap: 20px; flex-wrap: wrap">
        <div>
          <div class="c-kicker" style="margin-bottom: 6px">READY TO PLAY</div>
          <h2 class="c-title" style="font-size: 24px">选好时间了吗？去挑一块场地吧</h2>
          <p class="c-sub" style="margin-top: 6px">可预约未来 30 天，支持按日期、开始时间和结束时间精确查询。</p>
        </div>
        <el-button class="c-btn-primary" @click="goBooking">开始预约</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { Calendar, Medal } from '@element-plus/icons-vue'
import MascotFigure from '@/components/client/MascotFigure.vue'
import SectionHead from '@/components/client/SectionHead.vue'
import { clientApi } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useClockStore } from '@/stores/clock'
import {
  addDays, durationLabel, endTimeOptions, minutesBetween, prettyDate, timeOptions, todayFromClock
} from '@/utils/time'
import venueImage from '@/assets/mascot/venue-night.webp'

const router = useRouter()
const auth = useAuthStore()
const clock = useClockStore()

const loading = ref(false)
const overview = ref<any[]>([])
const levels = ref<any[]>([])
const venue = ref<any>(null)
const today = ref(todayFromClock(clock.info?.effectiveNow))
const quick = reactive({ date: today.value, start: '10:00', end: '12:00' })
let timer = 0

const petals = Array.from({ length: 10 }).map((_, i) => ({
  id: i,
  style: {
    left: `${(i * 9.3 + 4) % 100}%`,
    animationDuration: `${12 + (i % 5) * 2.2}s`,
    animationDelay: `${-i * 1.7}s`,
    opacity: 0.35 + (i % 3) * 0.12
  }
}))

const opening = computed(() => venue.value?.openingTime || '08:00:00')
const closing = computed(() => venue.value?.closingTime || '23:30:00')
const startOptions = computed(() => timeOptions(opening.value, closing.value))
const quickEndOptions = computed(() => endTimeOptions(opening.value, closing.value, quick.start))
const quickMinutes = computed(() => minutesBetween(quick.start, quick.end))

const stats = computed(() => {
  const open = overview.value.filter((c) => c.operationalStatus === 'OPEN')
  const free = open.filter((c) => c.currentStatus === 'AVAILABLE').length
  const prices = open.map((c) => Number(c.hourlyRate))
  return {
    openCourts: open.length,
    freeNow: free,
    minPrice: prices.length ? Math.min(...prices).toFixed(0) : '60'
  }
})

const notices = computed(() => {
  const base = dayjs(today.value)
  return [
    {
      day: base.format('DD'),
      month: base.format('MM月'),
      title: '场馆灯光系统升级完成',
      desc: '3 号与 4 号场地完成赛事级灯光调试，夜间场次体验更佳。'
    },
    {
      day: base.subtract(1, 'day').format('DD'),
      month: base.subtract(1, 'day').format('MM月'),
      title: '会员日：金卡及以上场地 9 折起',
      desc: '每周三会员日，等级折扣可与活动叠加，具体以结算金额为准。'
    },
    {
      day: base.subtract(3, 'day').format('DD'),
      month: base.subtract(3, 'day').format('MM月'),
      title: '关于预约取消规则',
      desc: '开赛前可免费取消，开赛后不可取消；取消后时间段立即释放。'
    }
  ]
})

const discountText = (rate: number | string | undefined) => {
  const value = Number(rate || 1)
  return value >= 1 ? '无折扣' : `${(value * 10).toFixed(1)} 折`
}

const levelGradient = (rank: number) => {
  const list = [
    'linear-gradient(140deg,#64748b,#94a3b8)',
    'linear-gradient(140deg,#0891b2,#67e8f9)',
    'linear-gradient(140deg,#d97706,#fbbf24)',
    'linear-gradient(140deg,#7c3aed,#ec4899)'
  ]
  return list[(rank - 1) % list.length]
}

function courtPill(court: any) {
  if (court.operationalStatus === 'MAINTENANCE') return { label: '维护中', status: 'MAINTENANCE' }
  const status = court.currentStatus
  if (status === 'IN_USE') return { label: '使用中', status: 'IN_PROGRESS' }
  if (status === 'BOOKED') return { label: '已预约', status: 'BOOKED' }
  if (status === 'AVAILABLE') return { label: '空闲', status: 'AVAILABLE' }
  return { label: '可预约', status: 'OPEN' }
}

function disabledDate(date: Date) {
  const d = dayjs(date)
  const start = dayjs(today.value)
  return d.isBefore(start, 'day') || d.isAfter(start.add(30, 'day'), 'day')
}

function ensureEnd() {
  if (quick.end <= quick.start) {
    quick.end = quickEndOptions.value[0] || quick.start
  }
}

function goBooking() {
  if (!quick.start || !quick.end || quick.end <= quick.start) {
    ElMessage.warning('结束时间需要晚于开始时间')
    return
  }
  router.push({
    path: '/club/booking',
    query: { date: quick.date, start: quick.start, end: quick.end }
  })
}

function goBookings() {
  router.push(auth.token ? '/club/bookings' : { path: '/club/login', query: { redirect: '/club/bookings' } })
}

function initQuick() {
  const now = dayjs(clock.info?.effectiveNow)
  let cursor = now.second(0).millisecond(0)
  const remainder = cursor.minute() % 30
  cursor = remainder === 0 && now.second() === 0 ? cursor : cursor.add(30 - remainder, 'minute')
  if (!cursor.isAfter(now)) cursor = cursor.add(30, 'minute')
  const close = dayjs(`${cursor.format('YYYY-MM-DD')} ${closing.value.slice(0, 5)}`)
  if (cursor.add(2, 'hour').isAfter(close)) {
    quick.date = cursor.add(1, 'day').format('YYYY-MM-DD')
    quick.start = '10:00'
    quick.end = '12:00'
  } else {
    quick.date = cursor.format('YYYY-MM-DD')
    quick.start = cursor.format('HH:mm')
    quick.end = cursor.add(2, 'hour').format('HH:mm')
  }
}

async function load() {
  loading.value = true
  try {
    today.value = todayFromClock(clock.info?.effectiveNow)
    const [courtRes, overviewRes, levelRes, venueRes] = await Promise.all([
      clientApi.courts(),
      clientApi.courtOverview(today.value),
      clientApi.memberLevels(),
      clientApi.venues()
    ])
    overview.value = overviewRes.data
    levels.value = levelRes.data
    venue.value = venueRes.data?.[0] || null
    if (!quick.date || quick.date < today.value) {
      initQuick()
    }
    if (!courtRes.data.length) {
      ElMessage.warning('暂未获取到场地数据')
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await clock.fetch()
  initQuick()
  await load()
  timer = window.setInterval(async () => {
    await clock.fetch()
    if (todayFromClock(clock.info?.effectiveNow) !== today.value) {
      await load()
    } else {
      const res = await clientApi.courtOverview(today.value)
      overview.value = res.data
    }
  }, 20000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.home-split {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 18px;
}

@media (max-width: 980px) {
  .home-split {
    grid-template-columns: 1fr;
  }
}
</style>
