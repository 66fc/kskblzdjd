<template>
  <div>
    <div class="page-header">
      <div>
        <h1>日使用统计</h1>
        <div class="sub">数据来自 court_usage_record，通过 MySQL 存储过程按自然日拆分统计</div>
      </div>
      <div class="actions">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false"
                        style="width: 160px" @change="load" />
        <el-select v-model="courtId" clearable placeholder="全部场地" style="width: 150px">
          <el-option v-for="c in courts" :key="c.courtId" :label="c.courtName" :value="c.courtId" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">查询统计</el-button>
      </div>
    </div>

    <div class="grid grid-3" style="margin-bottom: 16px">
      <div class="stat-card">
        <div class="glow" />
        <div class="label">实际使用总时长</div>
        <div class="value">{{ report?.total?.totalActualHours ?? 0 }}<small>小时</small></div>
        <div class="foot">{{ report?.total?.totalActualMinutes ?? 0 }} 分钟 · {{ report?.total?.usageRecordCount ?? 0 }} 条记录</div>
      </div>
      <div class="stat-card gold">
        <div class="glow" />
        <div class="label">场地销售总金额</div>
        <div class="value">{{ money(report?.total?.totalSalesAmount) }}</div>
        <div class="foot">按实际结算金额分日拆分</div>
      </div>
      <div class="stat-card cyan">
        <div class="glow" />
        <div class="label">有使用的场地</div>
        <div class="value">{{ report?.total?.usedCourtCount ?? 0 }}<small>块</small></div>
        <div class="foot">统计日期 {{ report?.date || date }}</div>
      </div>
    </div>

    <div class="glass-card">
      <div class="card-head">
        <h3>场地统计明细</h3>
        <div class="toolbar">
          <span class="chip mono">sp_daily_court_stats_by_court</span>
          <span class="chip mono">sp_daily_court_stats_all</span>
        </div>
      </div>
      <div class="card-body">
        <el-table :data="displayRows" v-loading="loading">
          <el-table-column label="场地" min-width="160">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 8px">
                <span class="court-dot" :style="{ background: row.statDate ? '#8b5cf6' : '#22d3ee' }" />
                <span>{{ row.courtName }}</span>
                <span v-if="!row.statDate" class="chip">汇总</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="实际使用时长" width="180">
            <template #default="{ row }">
              <span class="mono">{{ row.totalActualMinutes }} 分钟</span>
              <span class="muted" style="font-size: 10.5px"> / {{ row.totalActualHours }} h</span>
            </template>
          </el-table-column>
          <el-table-column label="场地销售金额" width="160">
            <template #default="{ row }">
              <span class="gold-text mono">{{ money(row.totalSalesAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="使用记录数" width="120" align="center">
            <template #default="{ row }"><span class="mono">{{ row.usageRecordCount ?? 0 }}</span></template>
          </el-table-column>
          <el-table-column label="占比" min-width="180">
            <template #default="{ row }">
              <el-progress :percentage="percent(row.totalActualMinutes)"
                           :stroke-width="6" :show-text="false" color="#8b5cf6" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="glass-card" style="margin-top: 16px">
      <div class="card-head">
        <h3>时长分布</h3>
        <span class="muted" style="font-size: 11.5px">单一场地可单独调用 sp_daily_court_stats_by_court</span>
      </div>
      <div class="card-body">
        <div ref="chartEl" style="height: 260px" />
      </div>
    </div>

    <div v-if="single" class="glass-card" style="margin-top: 16px">
      <div class="card-head">
        <h3>单场地查询结果 · {{ single.courtName }}</h3>
        <el-tag size="small" class="tag-booked">存储过程返回</el-tag>
      </div>
      <div class="card-body">
        <div class="grid grid-3">
          <div class="kv"><span>统计日期</span><span>{{ single.statDate }}</span></div>
          <div class="kv"><span>实际使用</span><span>{{ single.totalActualMinutes }} 分钟 / {{ single.totalActualHours }} 小时</span></div>
          <div class="kv"><span>销售金额</span><span class="gold-text">{{ money(single.totalSalesAmount) }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { api } from '@/api'
import { money, todayString } from '@/utils/format'
import { Search } from '@element-plus/icons-vue'

const date = ref(todayString())
const courtId = ref<number | undefined>()
const report = ref<any>(null)
const single = ref<any>(null)
const loading = ref(false)
const courts = ref<any[]>([])
const chartEl = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const displayRows = computed(() => {
  const rows = [...(report.value?.courts || [])]
  if (report.value?.total) {
    rows.push({
      courtId: null,
      courtName: '全部场地',
      totalActualMinutes: report.value.total.totalActualMinutes,
      totalActualHours: report.value.total.totalActualHours,
      totalSalesAmount: report.value.total.totalSalesAmount,
      usageRecordCount: report.value.total.usageRecordCount
    })
  }
  return rows
})

const maxMinutes = computed(() =>
  Math.max(1, ...(report.value?.courts || []).map((c: any) => Number(c.totalActualMinutes || 0)))
)

function percent(minutes: number) {
  return Math.min(100, Math.round((Number(minutes || 0) / maxMinutes.value) * 100))
}

async function load() {
  loading.value = true
  try {
    if (courtId.value) {
      const res = await api.dailyCourtReport(courtId.value, date.value)
      single.value = res.data
      const all = await api.dailyReport(date.value)
      report.value = all.data
    } else {
      single.value = null
      const res = await api.dailyReport(date.value)
      report.value = res.data
    }
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)
  const rows = report.value?.courts || []
  chart.setOption({
    backgroundColor: 'transparent',
    grid: { left: 70, right: 40, top: 16, bottom: 24 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(21,16,49,.95)',
      borderColor: 'rgba(139,92,246,.4)',
      textStyle: { color: '#e9e6ff' }
    },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(139,92,246,.1)' } },
      axisLabel: { color: '#8f88b8', fontSize: 10 }
    },
    yAxis: {
      type: 'category',
      data: rows.map((r: any) => r.courtName),
      axisLabel: { color: '#b9b1e3', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar',
      data: rows.map((r: any) => r.totalActualMinutes),
      barWidth: 13,
      itemStyle: {
        borderRadius: [0, 8, 8, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#22d3ee' },
          { offset: 1, color: '#8b5cf6' }
        ])
      },
      label: { show: true, position: 'right', color: '#c4b5fd', fontSize: 10, formatter: '{c} 分' }
    }]
  })
}

const resize = () => chart?.resize()

onMounted(async () => {
  const c = await api.adminCourts()
  courts.value = c.data
  await load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
})
</script>

<style scoped>
.court-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  box-shadow: 0 0 10px rgba(139, 92, 246, 0.8);
}
</style>
