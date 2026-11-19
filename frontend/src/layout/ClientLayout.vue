<template>
  <div class="client-app">
    <header class="client-nav">
      <div class="nav-inner">
        <router-link to="/club/home" class="nav-logo">
          <div class="nav-logo-mark">
            <svg viewBox="0 0 48 48" width="25" height="25">
              <path d="M24 5 L15 18 H33 Z" fill="#fff" opacity="0.96" />
              <path d="M15 18 L12 26 M24 18 L24 26 M33 18 L36 26" stroke="#fff" stroke-width="2.2"
                    stroke-linecap="round" opacity="0.9" />
              <circle cx="24" cy="33" r="6.5" fill="#fff" />
            </svg>
          </div>
          <div>
            <div class="nav-logo-title">羽球幻想馆</div>
            <div class="nav-logo-sub">BADMINTON FANTASY</div>
          </div>
        </router-link>

        <nav class="nav-links">
          <router-link to="/club/home">首页</router-link>
          <router-link to="/club/booking">场地预约</router-link>
          <router-link to="/club/bookings">我的预约</router-link>
          <router-link to="/club/profile">个人中心</router-link>
        </nav>

        <div class="nav-actions">
          <div class="client-clock" title="由场馆统一校准的业务时间">
            <span class="dot" />
            <span>{{ clock.info?.effectiveNow?.slice(5, 16) || '校准中' }}</span>
          </div>

          <el-dropdown v-if="auth.user" trigger="click" @command="onCommand">
            <div class="nav-user">
              <div class="nav-avatar">
                <img v-if="auth.user.avatar" :src="auth.user.avatar" alt="头像" />
                <span v-else>{{ auth.user.nickname?.slice(0, 1) }}</span>
              </div>
              <div>
                <div class="nav-user-name">{{ auth.user.nickname }}</div>
                <div class="nav-user-level">{{ auth.user.levelName }}</div>
              </div>
              <el-icon color="#8b7fae"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="bookings">我的预约</el-dropdown-item>
                <el-dropdown-item v-if="auth.isAdmin" divided command="admin">进入管理后台</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button v-else class="c-btn-primary" style="height: 38px; padding: 0 18px" @click="goLogin">
            登录 / 注册
          </el-button>

          <button class="nav-hamburger" aria-label="打开菜单" @click="mobileOpen = !mobileOpen">
            <el-icon :size="18"><Menu /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <div v-if="mobileOpen" class="mobile-menu" @click="mobileOpen = false">
      <router-link to="/club/home">首页</router-link>
      <router-link to="/club/booking">场地预约</router-link>
      <router-link to="/club/bookings">我的预约</router-link>
      <router-link to="/club/profile">个人中心</router-link>
      <router-link v-if="!auth.user" to="/club/login">登录 / 注册</router-link>
    </div>

    <main class="client-main">
      <router-view v-slot="{ Component }">
        <transition name="c-page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="client-footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <h4>羽球幻想馆 · Starfeather</h4>
          <p>
            以羽毛球为名的幻想空间。真实场地、真实占用、真实价格，
            每一次挥拍都从一次靠谱的预约开始。
          </p>
        </div>
        <div class="footer-col">
          <h5>预约服务</h5>
          <router-link to="/club/booking">场地预约</router-link>
          <router-link to="/club/bookings">我的预约</router-link>
          <router-link to="/club/profile">会员中心</router-link>
        </div>
        <div class="footer-col">
          <h5>场馆信息</h5>
          <span>深圳市龙岗区坪地街道星羽路 88 号</span>
          <span>营业时间 08:00 - 23:30</span>
          <span>电话 0755-8888 6666</span>
        </div>
        <div class="footer-col">
          <h5>系统说明</h5>
          <span>MySQL 8.0 · Spring Boot 3</span>
          <span>Vue 3 · TypeScript</span>
          <router-link v-if="auth.isAdmin" to="/dashboard">管理后台入口</router-link>
        </div>
      </div>
      <div class="footer-bottom">
        © 2026 羽球幻想馆 · 数据库分析与设计课程项目 · 业务时区 Asia/Shanghai
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Menu } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useClockStore } from '@/stores/clock'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const clock = useClockStore()
const mobileOpen = ref(false)

watch(() => route.fullPath, () => {
  mobileOpen.value = false
})

function goLogin() {
  router.push({ path: '/club/login', query: { redirect: route.fullPath } })
}

function onCommand(command: string) {
  if (command === 'profile') router.push('/club/profile')
  else if (command === 'bookings') router.push('/club/bookings')
  else if (command === 'admin') router.push('/dashboard')
  else if (command === 'logout') {
    auth.logout()
    router.push('/club/home')
  }
}

onMounted(() => clock.start())
onBeforeUnmount(() => clock.stop())
</script>

<style scoped>
.c-page-enter-active,
.c-page-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.c-page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.c-page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
