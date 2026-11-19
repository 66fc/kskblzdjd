<template>
  <div class="app-shell">
    <div class="sakura-layer">
      <span v-for="p in petals" :key="p.id" class="sakura" :style="p.style" />
    </div>

    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">
          <svg viewBox="0 0 48 48" width="26" height="26">
            <circle cx="24" cy="15" r="9" fill="url(#g1)" />
            <path d="M24 24 L18 42 L24 38 L30 42 Z" fill="#22d3ee" />
            <defs>
              <linearGradient id="g1" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#f472b6" />
                <stop offset="1" stop-color="#8b5cf6" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <div class="brand-title">星羽 STARFEATHER</div>
          <div class="brand-sub">COURT CONSOLE v1.0</div>
        </div>
      </div>

      <div class="side-section">运营中心</div>
      <el-menu :default-active="route.path" router class="side-menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon><span>数据总览</span>
        </el-menu-item>
        <el-menu-item index="/courts">
          <el-icon><Location /></el-icon><span>场地管理</span>
        </el-menu-item>
        <el-menu-item index="/bookings">
          <el-icon><Calendar /></el-icon><span>预约管理</span>
        </el-menu-item>
        <el-menu-item index="/sales">
          <el-icon><ShoppingCart /></el-icon><span>销售管理</span>
        </el-menu-item>
      </el-menu>

      <div class="side-section">会员体系</div>
      <el-menu :default-active="route.path" router class="side-menu">
        <el-menu-item index="/members">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/levels">
          <el-icon><Medal /></el-icon><span>会员等级</span>
        </el-menu-item>
      </el-menu>

      <div class="side-section">数据与系统</div>
      <el-menu :default-active="route.path" router class="side-menu">
        <el-menu-item index="/daily-stats">
          <el-icon><TrendCharts /></el-icon><span>日使用统计</span>
        </el-menu-item>
        <el-menu-item index="/clock">
          <el-icon><Timer /></el-icon><span>模拟时间</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon><span>操作日志</span>
        </el-menu-item>
      </el-menu>

      <div class="side-footer">
        <div class="sf-card">
          <div class="sf-title">
            <span class="dot" :class="{ pink: clock.paused }" />
            {{ clock.paused ? '时间已冻结' : '系统运行中' }}
          </div>
          <div class="sf-time mono">{{ clock.info?.effectiveNow || '--' }}</div>
          <div class="sf-note">{{ clock.info?.clockMode === 'REAL' ? '真实时钟模式' : `模拟时钟 · x${clock.info?.speed ?? 1}` }}</div>
        </div>
        <div class="sf-copy">Database Course Project<br />MySQL 8.0 · Spring Boot 3 · Vue 3</div>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <div class="crumb">星羽管理后台 / {{ route.meta.title }}</div>
          <h2>{{ route.meta.title }}</h2>
        </div>
        <div class="topbar-right">
          <div class="clock-chip" :class="{ paused: clock.paused }" @click="goClock">
            <span class="dot" :class="{ gold: clock.paused }" />
            <span class="mono">{{ clock.info?.effectiveNow || '加载中' }}</span>
            <span class="chip-mode">{{ clock.info?.clockMode === 'REAL' ? 'REAL' : (clock.paused ? 'PAUSED' : 'SIM') }}</span>
          </div>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user-chip">
              <div class="avatar">{{ (auth.user?.nickname || 'A').slice(0, 1) }}</div>
              <div class="user-meta">
                <div class="uname">{{ auth.user?.nickname }}</div>
                <div class="urole">{{ auth.isAdmin ? '超级管理员' : '会员' }}</div>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="portal">会员预约台</el-dropdown-item>
                <el-dropdown-item command="clock">模拟时间控制</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="page-body">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useClockStore } from '@/stores/clock'
import {
  DataLine, Location, Calendar, ShoppingCart, User, Medal, TrendCharts, Timer, Document, ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const clock = useClockStore()

const petals = computed(() =>
  Array.from({ length: 12 }).map((_, i) => ({
    id: i,
    style: {
      left: `${(i * 8.3 + 4) % 100}%`,
      animationDuration: `${13 + (i % 6) * 2.4}s`,
      animationDelay: `${-i * 1.9}s`,
      transform: `scale(${0.6 + (i % 4) * 0.22})`
    }
  }))
)

function goClock() {
  router.push('/clock')
}

function onCommand(command: string) {
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  } else if (command === 'portal') {
    router.push('/portal')
  } else if (command === 'clock') {
    router.push('/clock')
  }
}

onMounted(() => clock.start())
onBeforeUnmount(() => clock.stop())
</script>

<style scoped>
.sidebar {
  width: 236px;
  flex: 0 0 236px;
  min-height: 100vh;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  padding: 20px 14px 16px;
  background: linear-gradient(180deg, rgba(20, 14, 50, 0.94), rgba(9, 6, 26, 0.96));
  border-right: 1px solid rgba(139, 92, 246, 0.2);
  backdrop-filter: blur(18px);
  z-index: 2;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 20px;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(140deg, rgba(139, 92, 246, 0.3), rgba(34, 211, 238, 0.16));
  border: 1px solid rgba(167, 139, 250, 0.4);
  box-shadow: 0 0 22px rgba(139, 92, 246, 0.35);
}

.brand-title {
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 1.2px;
  background: linear-gradient(92deg, #fff, #c4b5fd, #67e8f9);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-sub {
  font-size: 9px;
  color: #6f6899;
  letter-spacing: 1.6px;
  margin-top: 3px;
}

.side-section {
  font-size: 10px;
  letter-spacing: 2px;
  color: #6f6899;
  padding: 16px 10px 6px;
}

.side-menu {
  border-right: none;
  background: transparent;
}

.side-menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  margin: 2px 0;
  border-radius: 11px;
  color: #a6a0cf;
  font-size: 13px;
  letter-spacing: 0.4px;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(139, 92, 246, 0.1);
  color: #ddd6fe;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(100deg, rgba(124, 58, 237, 0.35), rgba(139, 92, 246, 0.12));
  color: #ede9fe;
  box-shadow: inset 0 0 0 1px rgba(167, 139, 250, 0.3), 0 8px 20px rgba(76, 29, 149, 0.25);
}

.side-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  width: 3px;
  height: 20px;
  border-radius: 0 4px 4px 0;
  background: linear-gradient(180deg, #f472b6, #8b5cf6, #22d3ee);
}

.side-footer {
  margin-top: auto;
  padding: 12px 6px 0;
}

.sf-card {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(34, 211, 238, 0.18);
  background: linear-gradient(140deg, rgba(34, 211, 238, 0.08), rgba(139, 92, 246, 0.1));
}

.sf-title {
  font-size: 10.5px;
  color: #a5f3fc;
  display: flex;
  align-items: center;
  gap: 6px;
  letter-spacing: 0.6px;
}

.sf-time {
  font-size: 12.5px;
  color: #ecebff;
  margin: 7px 0 4px;
}

.sf-note {
  font-size: 10px;
  color: #8f88b8;
}

.sf-copy {
  text-align: center;
  font-size: 9.5px;
  color: #5f5885;
  line-height: 1.8;
  margin-top: 12px;
}

.topbar {
  height: 76px;
  padding: 0 26px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid rgba(139, 92, 246, 0.12);
  background: linear-gradient(180deg, rgba(10, 7, 28, 0.72), rgba(10, 7, 28, 0.32));
  backdrop-filter: blur(14px);
  position: sticky;
  top: 0;
  z-index: 5;
}

.crumb {
  font-size: 10.5px;
  color: #7c76a8;
  letter-spacing: 0.8px;
}

.topbar-left h2 {
  margin: 4px 0 0;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.8px;
  color: #ede9fe;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.chip-mode {
  font-size: 9px;
  letter-spacing: 1px;
  opacity: 0.7;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 10px 5px 5px;
  border-radius: 999px;
  border: 1px solid rgba(139, 92, 246, 0.22);
  background: rgba(139, 92, 246, 0.07);
  cursor: pointer;
  color: #c9c4ea;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(140deg, #f472b6, #8b5cf6 60%, #22d3ee);
  box-shadow: 0 0 14px rgba(139, 92, 246, 0.4);
}

.user-meta .uname {
  font-size: 12.5px;
  color: #ecebff;
  line-height: 1.3;
}

.user-meta .urole {
  font-size: 9.5px;
  color: #8f88b8;
  letter-spacing: 0.6px;
}

@media (max-width: 900px) {
  .sidebar {
    display: none;
  }
}
</style>
