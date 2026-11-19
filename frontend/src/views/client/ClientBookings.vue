<template>
  <div class="c-fade-up">
    <div class="page-banner">
      <div>
        <div class="c-kicker" style="color: #a5f3fc; margin-bottom: 6px">MY RESERVATIONS</div>
        <h1>我的预约</h1>
        <p>查看全部订单、取消未开始的预约，或再次预约同一块场地。</p>
      </div>
      <div class="banner-stats">
        <div class="banner-stat"><b>{{ stats.ALL || 0 }}</b><span>全部订单</span></div>
        <div class="banner-stat"><b>{{ stats.CONFIRMED || 0 }}</b><span>待使用</span></div>
        <div class="banner-stat"><b>{{ stats.COMPLETED || 0 }}</b><span>已完成</span></div>
        <div class="banner-stat"><b>{{ stats.CANCELLED || 0 }}</b><span>已取消</span></div>
      </div>
    </div>

    <div class="toolbar-row">
      <div class="booking-tabs">
        <button v-for="tab in tabs" :key="tab.value" class="booking-tab" :class="{ active: status === tab.value }"
                @click="switchTab(tab.value)">
          {{ tab.label }}
          <b>{{ stats[tab.value] || 0 }}</b>
        </button>
      </div>
      <div class="filter-tools">
        <el-date-picker v-model="dateFilter" type="date" value-format="YYYY-MM-DD" clearable
                        placeholder="按日期筛选" style="width: 150px" @change="reload" />
        <el-button text bg :icon="Refresh" :loading="loading" @click="reload">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="booking-list">
      <div v-for="booking in list" :key="booking.bookingId" class="booking-card">
        <div class="bc-date">
          <b>{{ dayOf(booking.startAt) }}</b>
          <span>{{ monthOf(booking.startAt) }}</span>
        </div>
        <div class="bc-main">
          <h3>
            {{ booking.courtName }}
            <StatusBadge :status="booking.status" />
          </h3>
          <div class="bc-meta">
            <span><el-icon><Clock /></el-icon>{{ timeOf(booking.startAt) }} - {{ timeOf(booking.endAt) }}</span>
            <span><el-icon><Timer /></el-icon>{{ durationLabel(booking.durationMinutes) }}</span>
            <span><el-icon><Location /></el-icon>{{ booking.venueName }}</span>
            <span><el-icon><Medal /></el-icon>{{ booking.memberLevelName }}</span>
          </div>
          <div class="bc-no">{{ booking.bookingNo }}</div>
        </div>
        <div class="bc-side">
          <div class="bc-amount">¥{{ Number(booking.payableAmount).toFixed(2) }}</div>
          <div class="bc-actions">
            <button class="text-btn" @click="openDetail(booking)">详情</button>
            <button v-if="booking.status === 'PENDING_PAYMENT'" class="text-btn" @click="pay(booking)">支付</button>
            <button v-if="canCancel(booking)" class="text-btn danger" @click="cancel(booking)">取消预约</button>
            <button v-if="booking.status === 'COMPLETED' || booking.status === 'CANCELLED'"
                    class="text-btn" @click="rebook(booking)">再次预约</button>
          </div>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !list.length" title="还没有符合条件的预约"
                description="去场地预约页挑一个喜欢的时间段吧。">
      <el-button class="c-btn-primary" @click="router.push('/club/booking')">去预约</el-button>
    </EmptyState>

    <div v-if="total > size" class="pager-row">
      <el-pagination v-model:current-page="page" :page-size="size" :total="total"
                     layout="prev, pager, next" background @current-change="load" />
    </div>

    <el-drawer v-model="detailVisible" size="520px" class="client-drawer">
      <template #header>
        <div>
          <div style="font-size: 16px; font-weight: 700; color: #241a45">预约详情</div>
          <div style="font-size: 10.5px; color: #8b7fae; margin-top: 4px">{{ detail?.booking?.bookingNo }}</div>
        </div>
      </template>
      <div v-if="detail" v-loading="detailLoading">
        <div class="detail-hero">
          <div>
            <div style="font-size: 11px; color: #8b7fae">{{ prettyDate(detail.booking.startAt.slice(0, 10)) }}</div>
            <h3>{{ detail.booking.courtName }}</h3>
            <p>{{ timeOf(detail.booking.startAt) }} - {{ timeOf(detail.booking.endAt) }} · {{ durationLabel(detail.booking.durationMinutes) }}</p>
          </div>
          <StatusBadge :status="detail.booking.status" />
        </div>

        <div class="detail-block">
          <div class="kv-row"><span>订单编号</span><span class="mono">{{ detail.booking.bookingNo }}</span></div>
          <div class="kv-row"><span>会员</span><span>{{ detail.booking.memberNickname }} · {{ detail.booking.memberLevelName }}</span></div>
          <div class="kv-row"><span>场地单价</span><span>¥{{ Number(detail.booking.hourlyRateSnapshot).toFixed(2) }} / 时</span></div>
          <div class="kv-row"><span>会员折扣</span><span>{{ discountText(detail.booking.discountSnapshot) }}</span></div>
          <div class="kv-row"><span>原价</span><span>¥{{ Number(detail.booking.originalAmount).toFixed(2) }}</span></div>
          <div class="kv-row"><span>应付金额</span><span style="color: #7c3aed; font-weight: 700">¥{{ Number(detail.booking.payableAmount).toFixed(2) }}</span></div>
          <div class="kv-row"><span>创建时间</span><span>{{ detail.booking.createdAt }}</span></div>
          <div v-if="detail.booking.cancelReason" class="kv-row"><span>取消原因</span><span>{{ detail.booking.cancelReason }}</span></div>
        </div>

        <div class="detail-title">时间段占用</div>
        <div class="detail-slots">
          <div v-for="slot in detail.slots" :key="slot.slotId" class="detail-slot" :class="slot.slotStatus.toLowerCase()">
            <span>{{ timeOf(slot.slotStart) }}</span>
            <b>{{ slotLabel(slot.slotStatus) }}</b>
          </div>
        </div>

        <div class="detail-title">实际使用</div>
        <div v-if="detail.usage" class="detail-block">
          <div class="kv-row"><span>实际起止</span><span>{{ detail.usage.actualStartAt }} ~ {{ timeOf(detail.usage.actualEndAt) }}</span></div>
          <div class="kv-row"><span>实际时长</span><span>{{ detail.usage.actualMinutes }} 分钟（计费 {{ detail.usage.billedMinutes }} 分钟）</span></div>
          <div class="kv-row"><span>结算金额</span><span style="color: #7c3aed; font-weight: 700">¥{{ Number(detail.usage.actualFeeAmount).toFixed(2) }}</span></div>
        </div>
        <div v-else class="detail-empty">尚未产生实际使用记录</div>

        <div class="drawer-actions">
          <el-button v-if="detail.booking.status === 'PENDING_PAYMENT'" class="c-btn-primary"
                     style="height: 38px" @click="pay(detail.booking)">去支付</el-button>
          <el-button v-if="canCancel(detail.booking)" type="danger" plain @click="cancel(detail.booking)">取消预约</el-button>
          <el-button @click="rebook(detail.booking)">再次预约</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { Clock, Location, Medal, Refresh, Timer } from '@element-plus/icons-vue'
import StatusBadge from '@/components/client/StatusBadge.vue'
import EmptyState from '@/components/client/EmptyState.vue'
import { clientApi } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useClockStore } from '@/stores/clock'
import { durationLabel, prettyDate, timePart, toIso } from '@/utils/time'

const router = useRouter()
const auth = useAuthStore()
const clock = useClockStore()

const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = 6
const loading = ref(false)
const status = ref('ALL')
const dateFilter = ref('')
const stats = ref<Record<string, number>>({})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const tabs = [
  { label: '全部', value: 'ALL' },
  { label: '待使用', value: 'CONFIRMED' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

const now = computed(() => clock.info?.effectiveNow || dayjs().format('YYYY-MM-DD HH:mm:ss'))

const dayOf = (value: string) => dayjs(value).format('DD')
const monthOf = (value: string) => dayjs(value).format('MM月')
const timeOf = (value: string) => timePart(value)
const discountText = (rate: number | string) => {
  const value = Number(rate)
  return value >= 1 ? '无折扣' : `${(value * 10).toFixed(1)} 折`
}

function slotLabel(statusValue: string) {
  return { AVAILABLE: '空闲', BOOKED: '已预约', IN_USE: '使用中', CLOSED: '关闭' }[statusValue] || statusValue
}

function canCancel(booking: any) {
  return ['CONFIRMED', 'PENDING_PAYMENT'].includes(booking.status) && booking.startAt > now.value
}

function switchTab(value: string) {
  status.value = value
  page.value = 1
  load()
}

async function load() {
  loading.value = true
  try {
    const res = await clientApi.myBookings({
      page: page.value,
      size,
      status: status.value === 'ALL' ? undefined : status.value,
      date: dateFilter.value || undefined
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await clientApi.bookingStats()
  stats.value = res.data
}

async function reload() {
  page.value = 1
  await Promise.all([load(), loadStats()])
}

async function openDetail(booking: any) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await clientApi.bookingDetail(booking.bookingId)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

async function cancel(booking: any) {
  const { value } = await ElMessageBox.prompt(
    `确认取消「${booking.courtName} ${prettyDate(booking.startAt.slice(0, 10))} ${timeOf(booking.startAt)}」的预约吗？\n取消后该时间段会立即释放。`,
    '取消预约确认',
    {
      inputPlaceholder: '请填写取消原因',
      inputValidator: (v: string) => (v && v.trim() ? true : '请填写取消原因'),
      confirmButtonText: '确认取消',
      cancelButtonText: '保留预约',
      type: 'warning'
    }
  )
  await clientApi.cancelBooking(booking.bookingId, value.trim())
  const check = await clientApi.availability(
    toIso(booking.startAt.slice(0, 10), timeOf(booking.startAt)),
    toIso(booking.startAt.slice(0, 10), timeOf(booking.endAt))
  )
  ElMessage.success(`已取消，该时段现有 ${check.data.length} 块场地可预约`)
  detailVisible.value = false
  await reload()
}

async function pay(booking: any) {
  await clientApi.payBooking(booking.bookingId)
  ElMessage.success('支付成功，场地已锁定')
  detailVisible.value = false
  await reload()
}

function rebook(booking: any) {
  router.push({
    path: '/club/booking',
    query: {
      date: booking.startAt.slice(0, 10),
      start: timeOf(booking.startAt),
      end: timeOf(booking.endAt)
    }
  })
}

onMounted(async () => {
  await clock.fetch()
  await reload()
})
</script>

<style scoped>
.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.filter-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(150deg, #f6f0ff, #ffffff);
  border: 1px solid rgba(124, 58, 237, 0.12);
}

.detail-hero h3 {
  margin: 5px 0 4px;
  font-size: 19px;
  color: #241a45;
}

.detail-hero p {
  margin: 0;
  font-size: 11.5px;
  color: #8b7fae;
}

.detail-block {
  margin-top: 14px;
  padding: 6px 14px;
  border-radius: 14px;
  border: 1px solid rgba(124, 58, 237, 0.1);
}

.detail-title {
  margin: 20px 0 9px;
  font-size: 12px;
  font-weight: 700;
  color: #4a3d6b;
  letter-spacing: 0.6px;
}

.detail-slots {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.detail-slot {
  padding: 7px 4px;
  border-radius: 8px;
  text-align: center;
  font-size: 9.5px;
  background: #f8f5ff;
  color: #8b7fae;
}

.detail-slot span {
  display: block;
  font-size: 10.5px;
  color: #4a3d6b;
}

.detail-slot.booked {
  background: rgba(124, 58, 237, 0.08);
  color: #6d28d9;
}

.detail-slot.in_use {
  background: rgba(6, 182, 212, 0.1);
  color: #0e7490;
}

.detail-slot.available {
  background: rgba(16, 185, 129, 0.08);
  color: #047857;
}

.detail-empty {
  padding: 16px;
  text-align: center;
  font-size: 11px;
  color: #a79bc4;
  border: 1px dashed rgba(124, 58, 237, 0.16);
  border-radius: 12px;
}

.drawer-actions {
  display: flex;
  gap: 10px;
  margin-top: 22px;
}

.mono {
  font-family: monospace;
}
</style>
