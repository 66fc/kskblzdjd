<template>
  <div>
    <div class="page-header">
      <div>
        <h1>数据总览</h1>
        <div class="sub">
          业务日期 {{ report?.businessDate || date }} · 数据来自 MySQL 实时统计
          <span class="chip" style="margin-left: 8px">模拟时钟 {{ report?.simulatedNow || '--' }}</span>
        </div>
      </div>
      <div class="actions">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false"
                        style="width: 150px" @change="load" />
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Calendar" @click="router.push('/bookings')">预约管理</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <div class="grid grid-8">
        <div class="stat-card">
          <div class="glow" />
          <div class="label"><span class="stat-icon"><Location /></span>场地总数</div>
          <div class="value">{{ counts.totalCourts ?? 0 }}<small>块</small></div>
          <div class="foot">其中开放 {{ counts.openCourts ?? 0 }} 块</div>
        </div>
        <div class="stat-card cyan">
          <div class="glow" />
          <div class="label"><span class="stat-icon"><Sunny /></span>当前空闲场地</div>
          <div class="value">{{ counts.freeCourtsNow ?? 0 }}<small>块</small></div>
          <div class="foot">按当前模拟时间实时计算</div>
        </div>
        <div class="stat-card pink">
          <div class="glow" />
          <div class="label"><span class="stat-icon"><Clock /></span>已预约时间段</div>
          <div class="value">{{ counts.bookedSlotsToday ?? 0 }}<small>段</small></div>
          <div class="foot">业务日期内 30 分钟段</div>
        </div>
        <div class="stat-card gold">
          <div class="glow" />
          <div class="label"><span class="stat-icon"><VideoPlay /></span>使用中场地</div>
          <div class="value">{{ counts.inUseCourts ?? 0 }}<small>块</small></div>
          <div class="foot">状态 IN_USE 的时间段</div>
        </div>
        <div class="stat-card">
          <div class="glow" />
          <div class="label"><span class="stat-icon"><User /></span>注册会员</div>
          <div class="value">{{ counts.totalMembers ?? 0 }}<small>人</small></div>
          <div class="foot">不含管理员账号</div>
        </div>
        <div class="stat-card cyan">
          <div class="glow" />
          <div class="label"><span class="stat-icon"><Calendar /></span>今日预约订单</div>
          <div class="value">{{ counts.todayBookings ?? 0 }}<small>单</small></div>
          <div class="foot">待支付 {{ counts.pendingBookings ?? 0 }} 单</div>
        </div>
        <div class="stat-card gold">
          <div class="glow" />
          <div class="label"><span class="stat-icon"><Wallet /></span>今日租赁收入</div>
          <div class="value">{{ money(report?.todayRentalRevenue) }}</div>
          <div class="foot">来自实际使用结算</div>
        </div>
        <div class="stat-card pink">
          <div class="glow" />
          <div class="label"><span class="stat-icon"><Goods /></span>今日商品收入</div>
          <div class="value">{{ money(report?.todayProductRevenue) }}</div>
          <div class="foot">与场地收入分开统计</div>
        </div>
      </div>

      <div class="grid grid-main" style="margin-top: 16px">
        <div class="glass-card">
          <div class="card-head">
            <h3>近 14 天收入趋势</h3>
            <span class="muted" style="font-size: 11.5px">场地租赁与商品销售独立统计</span>
          </div>
          <div class="card-body">
            <div ref="trendEl" style="height: 300px" />
          </div>
        </div>

        <div class="glass-card">
          <div class="card-head"><h3>今日收入结构</h3></div>
          <div class="card-body">
            <div class="revenue-total">
              <div class="rt-label">今日总收入</div>
              <div class="rt-value">{{ money(totalRevenue) }}</div>
              <div class="rt-sub">
                <span class="green-text">实际使用 {{ report?.todayHours ?? 0 }} 小时</span>
                <span class="muted">· {{ report?.todaySummary?.usageRecordCount ?? 0 }} 条使用记录</span>
              </div>
            </div>
            <div class="split-row">
              <div class="split-name"><span class="dot" /> 场地租赁</div>
              <div class="split-val">{{ money(report?.todayRentalRevenue) }}</div>
            </div>
            <el-progress :percentage="rentalPercent" :show-text="false" :stroke-width="8"
                         color="#8b5cf6" style="margin-bottom: 14px" />
            <div class="split-row">
              <div class="split-name"><span class="dot pink" /> 商品销售</div>
              <div class="split-val">{{ money(report?.todayProductRevenue) }}</div>
            </div>
            <el-progress :percentage="productPercent" :show-text="false" :stroke-width="8"
                         color="#f472b6" />

            <el-divider />

            <div class="period">
              <div>
                <div class="p-label">本周累计</div>
                <div class="p-value">{{ money(week?.totalRevenue) }}</div>
              </div>
              <div>
                <div class="p-label">本月累计</div>
                <div class="p-value">{{ money(month?.totalRevenue) }}</div>
              </div>
            </div>
            <div class="timeline-note" style="margin-top: 10px">
              周/月统计范围：{{ week?.from }} ~ {{ week?.to }} / {{ month?.from }} ~ {{ month?.to }}
            </div>
          </div>
        </div>
      </div>

      <div class="grid grid-main" style="margin-top: 16px">
        <div class="glass-card">
          <div class="card-head">
            <h3>场地使用率</h3>
            <span class="muted" style="font-size: 11.5px">实际使用分钟 / 营业时长</span>
          </div>
          <div class="card-body">
            <div ref="usageEl" style="height: 290px" />
          </div>
        </div>

        <div class="glass-card">
          <div class="card-head">
            <h3>今日场次</h3>
            <el-button text size="small" @click="router.push('/bookings')">全部</el-button>
          </div>
          <div class="card-body" style="padding-top: 6px">
            <div v-if="!report?.todayBookings?.length" class="empty-hint">今日暂无预约</div>
            <div v-for="b in report?.todayBookings" :key="b.bookingId" class="schedule-item">
              <div class="si-time mono">{{ timePart(b.startAt) }}<span>-{{ timePart(b.endAt) }}</span></div>
              <div class="si-main">
                <div class="si-title">{{ b.courtName }} · {{ b.memberNickname }}</div>
                <div class="si-sub">{{ b.bookingNo }}</div>
              </div>
              <el-tag size="small" :type="statusType(b.status)">{{ statusLabel(b.status) }}</el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { api } from '@/api'
import { money, todayString, bookingStatusMap } from '@/utils/format'
import type { DashboardData } from '@/types'
import {
  Refresh, Calendar, Location, Sunny, Clock, VideoPlay, User, Wallet, Goods
} from '@element-plus/icons-vue'

const router = useRouter()
const date = ref(todayString())
const loading = ref(false)
const report = ref<DashboardData | null>(null)
const week = ref<any>(null)
const month = ref<any>(null)
const trendEl = ref<HTMLElement>()
const usageEl = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let usageChart: echarts.ECharts | null = null

const counts = computed(() => report.value?.counts || ({} as any))
const totalRevenue = computed(() => Number(report.value?.todayRentalRevenue || 0) + Number(report.value?.todayProductRevenue || 0))
const rentalPercent = computed(() => {
  if (totalRevenue.value <= 0) return 0
  return Math.round((Number(report.value?.todayRentalRevenue || 0) / totalRevenue.value) * 100)
})
const productPercent = computed(() => (totalRevenue.value <= 0 ? 0 : 100 - rentalPercent.value))

const statusLabel = (s: string) => bookingStatusMap[s]?.label || s
const statusType = (s: string) => bookingStatusMap[s]?.type || 'info'
const timePart = (value: string) => (value ? value.slice(11, 16) : '--')

async function load() {
  loading.value = true
  try {
    const [dash, w, m] = await Promise.all([
      api.dashboard({ date: date.value }),
      api.overview({ date: date.value, range: 'week' }),
      api.overview({ date: date.value, range: 'month' })
    ])
    report.value = dash.data
    week.value = w.data
    month.value = m.data
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  if (trendEl.value) {
    if (!trendChart) trendChart = echarts.init(trendEl.value)
    const trend = report.value?.trend || []
    trendChart.setOption({
      backgroundColor: 'transparent',
      grid: { left: 44, right: 18, top: 34, bottom: 30 },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(21,16,49,.95)',
        borderColor: 'rgba(139,92,246,.4)',
        textStyle: { color: '#e9e6ff' },
        valueFormatter: (v: any) => `¥${Number(v).toFixed(2)}`
      },
      legend: {
        right: 6,
        top: 0,
        textStyle: { color: '#a29cc9', fontSize: 11 },
        data: ['场地租赁', '商品销售']
      },
      xAxis: {
        type: 'category',
        data: trend.map((t) => t.date.slice(5)),
        axisLine: { lineStyle: { color: 'rgba(139,92,246,.25)' } },
        axisLabel: { color: '#8f88b8', fontSize: 10 }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: 'rgba(139,92,246,.1)' } },
        axisLabel: { color: '#8f88b8', fontSize: 10 }
      },
      series: [
        {
          name: '场地租赁',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          data: trend.map((t) => t.rental),
          lineStyle: { width: 2.4, color: '#a78bfa' },
          itemStyle: { color: '#c4b5fd' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(139,92,246,.42)' },
              { offset: 1, color: 'rgba(139,92,246,0)' }
            ])
          }
        },
        {
          name: '商品销售',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          data: trend.map((t) => t.product),
          lineStyle: { width: 2.2, color: '#f472b6' },
          itemStyle: { color: '#f9a8d4' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(244,114,182,.3)' },
              { offset: 1, color: 'rgba(244,114,182,0)' }
            ])
          }
        }
      ]
    })
  }

  if (usageEl.value) {
    if (!usageChart) usageChart = echarts.init(usageEl.value)
    const usage = report.value?.courtUsage || []
    usageChart.setOption({
      backgroundColor: 'transparent',
      grid: { left: 76, right: 44, top: 16, bottom: 20 },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(21,16,49,.95)',
        borderColor: 'rgba(139,92,246,.4)',
        textStyle: { color: '#e9e6ff' }
      },
      xAxis: {
        type: 'value',
        max: 100,
        splitLine: { lineStyle: { color: 'rgba(139,92,246,.1)' } },
        axisLabel: { color: '#8f88b8', fontSize: 10, formatter: '{value}%' }
      },
      yAxis: {
        type: 'category',
        data: usage.map((u) => u.courtName),
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#b9b1e3', fontSize: 11 }
      },
      series: [
        {
          type: 'bar',
          data: usage.map((u) => u.usageRate),
          barWidth: 12,
          itemStyle: {
            borderRadius: [0, 8, 8, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#6366f1' },
              { offset: 0.6, color: '#a855f7' },
              { offset: 1, color: '#22d3ee' }
            ])
          },
          label: {
            show: true,
            position: 'right',
            color: '#c4b5fd',
            fontSize: 10,
            formatter: '{c}%'
          }
        }
      ]
    })
  }
}

const resize = () => {
  trendChart?.resize()
  usageChart?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose()
  usageChart?.dispose()
})
</script>

<style scoped>
.revenue-total {
  padding: 6px 0 16px;
}

.rt-label {
  font-size: 11.5px;
  color: var(--muted);
  letter-spacing: 1px;
}

.rt-value {
  font-size: 32px;
  font-weight: 700;
  margin: 6px 0;
  background: linear-gradient(100deg, #fff, #fcd34d 45%, #f9a8d4);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.rt-sub {
  font-size: 11px;
  display: flex;
  gap: 8px;
}

.split-row {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  margin: 10px 0 6px;
}

.split-name {
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 7px;
}

.split-val {
  color: var(--text-soft);
  font-variant-numeric: tabular-nums;
}

.period {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.p-label {
  font-size: 10.5px;
  color: var(--muted);
  letter-spacing: 0.6px;
}

.p-value {
  font-size: 18px;
  color: #fcd34d;
  margin-top: 4px;
  font-variant-numeric: tabular-nums;
}

.schedule-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed rgba(139, 92, 246, 0.12);
}

.schedule-item:last-child {
  border-bottom: none;
}

.si-time {
  font-size: 12px;
  color: #a5f3fc;
  min-width: 84px;
}

.si-time span {
  color: #7c76a8;
  font-size: 10.5px;
}

.si-main {
  flex: 1;
  min-width: 0;
}

.si-title {
  font-size: 12.5px;
  color: var(--text-soft);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.si-sub {
  font-size: 10px;
  color: #7c76a8;
  margin-top: 2px;
}
</style>
