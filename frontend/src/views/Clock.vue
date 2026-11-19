<template>
  <div>
    <div class="page-header">
      <div>
        <h1>模拟时间控制</h1>
        <div class="sub">全局统一时间服务 · 所有业务接口与统计日期均以此时钟为准</div>
      </div>
      <div class="actions">
        <el-button :icon="Refresh" @click="clock.fetch()">刷新</el-button>
        <el-tag :type="clock.paused ? 'warning' : 'success'" effect="dark">
          {{ clock.paused ? '已暂停' : '运行中' }}
        </el-tag>
      </div>
    </div>

    <div class="grid grid-main">
      <div class="glass-card clock-hero">
        <div class="hero-glow" />
        <div class="hero-label">CURRENT BUSINESS TIME</div>
        <div class="hero-time mono">{{ clock.info?.effectiveNow || '--:--:--' }}</div>
        <div class="hero-meta">
          <span class="chip">业务日期 {{ clock.info?.businessDate || '--' }}</span>
          <span class="chip">{{ clock.info?.clockMode === 'REAL' ? '真实时钟' : '模拟时钟' }}</span>
          <span class="chip" :class="{ 'tag-maintenance': clock.paused }">
            {{ clock.paused ? '时间已冻结' : `流速 x${clock.info?.speed ?? 1}` }}
          </span>
        </div>
        <div class="hero-grid">
          <div>
            <div class="hg-label">模拟基准</div>
            <div class="hg-value mono">{{ clock.info?.simulatedNow || '--' }}</div>
          </div>
          <div>
            <div class="hg-label">真实时间</div>
            <div class="hg-value mono">{{ clock.info?.realNow || '--' }}</div>
          </div>
          <div>
            <div class="hg-label">基准对应真实时刻</div>
            <div class="hg-value mono">{{ clock.info?.baseRealAt || '--' }}</div>
          </div>
        </div>
        <div class="hero-note">
          有效时间 = 模拟基准 + (真实经过秒数 × 流速)。暂停时冻结在基准点；
          切换日期后，场地日历、可预约查询、日统计与数据总览会同步更新。
        </div>
      </div>

      <div class="glass-card">
        <div class="card-head"><h3>时间控制台</h3></div>
        <div class="card-body">
          <el-form label-width="86px">
            <el-form-item label="目标日期">
              <el-date-picker v-model="targetDate" type="date" value-format="YYYY-MM-DD"
                              style="width: 100%" :clearable="false" />
            </el-form-item>
            <el-form-item label="目标时间">
              <el-time-picker v-model="targetTime" format="HH:mm:ss" value-format="HH:mm:ss"
                              style="width: 100%" :clearable="false" />
            </el-form-item>
            <el-form-item label="时间流速">
              <el-input-number v-model="speed" :min="0.1" :max="100" :step="0.5" :precision="2"
                               style="width: 100%" />
            </el-form-item>
            <el-form-item label="演示模式">
              <el-switch v-model="demoMode" active-text="开启" inactive-text="关闭" />
            </el-form-item>
          </el-form>
          <el-button type="primary" style="width: 100%" :loading="saving" @click="applyTime">
            设置模拟时间并应用
          </el-button>

          <el-divider />

          <div class="control-row">
            <div>
              <div class="cr-title">暂停 / 恢复</div>
              <div class="cr-sub">暂停后所有业务时间冻结</div>
            </div>
            <el-button :type="clock.paused ? 'success' : 'warning'" plain @click="togglePause">
              {{ clock.paused ? '恢复时间' : '暂停时间' }}
            </el-button>
          </div>
          <div class="control-row">
            <div>
              <div class="cr-title">时钟模式</div>
              <div class="cr-sub">REAL 使用服务器真实时间</div>
            </div>
            <el-radio-group :model-value="clock.info?.clockMode" size="small" @change="switchMode">
              <el-radio-button label="SIMULATED">模拟</el-radio-button>
              <el-radio-button label="REAL">真实</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </div>
    </div>

    <div class="glass-card" style="margin-top: 16px">
      <div class="card-head">
        <h3>快捷跳转</h3>
        <span class="muted" style="font-size: 11.5px">演示跨日期业务时使用</span>
      </div>
      <div class="card-body">
        <div class="toolbar">
          <el-button @click="quickJump(0, 1)">+ 1 小时</el-button>
          <el-button @click="quickJump(1, 0)">+ 1 天</el-button>
          <el-button @click="quickJump(7, 0)">+ 7 天</el-button>
          <el-button @click="jumpTomorrowMorning">明天 09:00</el-button>
          <el-button @click="jumpToNow">回到当前真实时间</el-button>
        </div>
        <div class="timeline-note" style="margin-top: 14px">
          修改模拟时间需要管理员权限（PUT /api/admin/system/clock）。前端每 8 秒拉取一次
          GET /api/system/clock，各页面读取后端统一时间，不使用浏览器本地时间作为业务依据。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { api } from '@/api'
import { useClockStore } from '@/stores/clock'
import { Refresh } from '@element-plus/icons-vue'

const clock = useClockStore()
const targetDate = ref(dayjs().format('YYYY-MM-DD'))
const targetTime = ref(dayjs().format('HH:mm:ss'))
const speed = ref(1)
const demoMode = ref(true)
const saving = ref(false)

function syncForm() {
  if (!clock.info) return
  targetDate.value = clock.info.simulatedNow.slice(0, 10)
  targetTime.value = clock.info.simulatedNow.slice(11, 19)
  speed.value = Number(clock.info.speed)
  demoMode.value = Boolean(clock.info.demoMode)
}

async function applyClock(payload: any, message: string) {
  saving.value = true
  try {
    await api.updateClock(payload)
    await clock.fetch()
    syncForm()
    ElMessage.success(message)
  } finally {
    saving.value = false
  }
}

function applyTime() {
  applyClock({
    simulatedNow: `${targetDate.value} ${targetTime.value}`,
    speed: Number(speed.value),
    demoMode: demoMode.value,
    paused: clock.paused
  }, '模拟时间已更新')
}

function togglePause() {
  applyClock({ paused: !clock.paused }, clock.paused ? '时间已恢复运行' : '时间已暂停')
}

function switchMode(mode: string) {
  applyClock({ clockMode: mode }, mode === 'REAL' ? '已切换为真实时钟' : '已切换为模拟时钟')
}

function quickJump(days: number, hours: number) {
  const base = dayjs(clock.info?.effectiveNow || undefined)
  const next = base.add(days, 'day').add(hours, 'hour')
  applyClock({ simulatedNow: next.format('YYYY-MM-DD HH:mm:ss') }, `已跳转到 ${next.format('MM-DD HH:mm')}`)
}

function jumpTomorrowMorning() {
  const next = dayjs(clock.info?.effectiveNow || undefined).add(1, 'day').hour(9).minute(0).second(0)
  applyClock({ simulatedNow: next.format('YYYY-MM-DD HH:mm:ss') }, '已跳转到明天 09:00')
}

function jumpToNow() {
  applyClock({ clockMode: 'REAL' }, '已回到真实时间')
}

onMounted(async () => {
  await clock.fetch()
  syncForm()
})
</script>

<style scoped>
.clock-hero {
  position: relative;
  padding: 30px 32px;
  overflow: hidden;
}

.hero-glow {
  position: absolute;
  right: -80px;
  top: -100px;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.22), transparent 65%);
  filter: blur(4px);
}

.hero-label {
  font-size: 10.5px;
  letter-spacing: 3px;
  color: #7c76a8;
}

.hero-time {
  font-size: 46px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 10px 0 14px;
  background: linear-gradient(100deg, #ffffff, #a5f3fc 45%, #c4b5fd);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 30px rgba(34, 211, 238, 0.2);
}

.hero-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

.hero-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  padding-top: 18px;
  border-top: 1px dashed rgba(139, 92, 246, 0.18);
}

.hg-label {
  font-size: 10.5px;
  color: #7c76a8;
  letter-spacing: 0.6px;
}

.hg-value {
  font-size: 11.5px;
  color: #c9c4ea;
  margin-top: 5px;
}

.hero-note {
  margin-top: 20px;
  font-size: 11.5px;
  line-height: 1.9;
  color: #8f88b8;
  max-width: 720px;
}

.control-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(139, 92, 246, 0.12);
}

.control-row:last-child {
  border-bottom: none;
}

.cr-title {
  font-size: 12.5px;
  color: var(--text-soft);
}

.cr-sub {
  font-size: 10.5px;
  color: #7c76a8;
  margin-top: 3px;
}
</style>
